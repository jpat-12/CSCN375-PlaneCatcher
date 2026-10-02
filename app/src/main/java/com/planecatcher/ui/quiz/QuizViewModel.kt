package com.planecatcher.ui.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planecatcher.core.geo.Geo
import com.planecatcher.core.quiz.QuizGenerator
import com.planecatcher.core.quiz.QuizQuestion
import com.planecatcher.core.radar.NearbyPlane
import com.planecatcher.data.PhotoRepository
import com.planecatcher.data.PlanePhoto
import com.planecatcher.data.TierProvider
import com.planecatcher.data.local.CaughtPlaneDao
import com.planecatcher.data.local.CaughtPlaneEntity
import com.planecatcher.data.local.NearbyCacheDao
import com.planecatcher.data.time.TrustedClock
import com.planecatcher.domain.CatchManager
import com.planecatcher.domain.CatchResult
import com.planecatcher.domain.RadarTracker
import com.planecatcher.domain.ticker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface QuizUiState {
    data object Loading : QuizUiState
    data object NotFound : QuizUiState
    data object AlreadyCaught : QuizUiState
    data object MockLocation : QuizUiState
    data class Locked(val retryAtMs: Long) : QuizUiState
    data class Asking(
        val plane: NearbyPlane,
        val question: QuizQuestion,
        val photo: PlanePhoto?,
        val submitting: Boolean = false,
    ) : QuizUiState
    data class Caught(val plane: CaughtPlaneEntity) : QuizUiState
    data class Missed(
        val question: QuizQuestion,
        val chosenIndex: Int,
        val retryAtMs: Long,
    ) : QuizUiState
}

@HiltViewModel
class QuizViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val tracker: RadarTracker,
    private val cacheDao: NearbyCacheDao,
    private val caughtDao: CaughtPlaneDao,
    private val catchManager: CatchManager,
    private val generator: QuizGenerator,
    private val tiers: TierProvider,
    private val photos: PhotoRepository,
    private val clock: TrustedClock,
) : ViewModel() {
    private val hex: String = checkNotNull(savedState["hex"])

    private val _state = MutableStateFlow<QuizUiState>(QuizUiState.Loading)
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    val now: StateFlow<Long> = ticker().map { clock.now() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), clock.now())

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        if (caughtDao.isCaught(hex)) {
            _state.value = QuizUiState.AlreadyCaught
            return
        }
        val radar = tracker.state.value
        if (radar.mockLocation && radar.jumpTarget == null) {
            _state.value = QuizUiState.MockLocation
            return
        }
        val plane = radar.planes.firstOrNull { it.hex == hex } ?: cacheDao.get(hex)?.let { cached ->
            val a = cached.toAircraft()
            val center = radar.center ?: a.position
            NearbyPlane(
                aircraft = a,
                distanceMiles = Geo.distanceMiles(center, a.position),
                bearingDeg = Geo.bearingDeg(center, a.position),
                tier = tiers.table.classify(a.typeCode, a.isMilitary),
            )
        }
        if (plane == null) {
            _state.value = QuizUiState.NotFound
            return
        }
        val lock = catchManager.lockFor(hex)
        if (lock != null && lock.isLocked(clock.now())) {
            _state.value = QuizUiState.Locked(lock.retryAtMs)
            return
        }
        // A retry after a wrong answer gets a different question.
        val question = generator.generate(plane.aircraft, avoidKind = lock?.failedKind)
        _state.value = QuizUiState.Asking(plane, question, photos.cached(hex))
        if (photos.cached(hex) == null) {
            val photo = photos.photoFor(hex)
            (_state.value as? QuizUiState.Asking)?.let { _state.value = it.copy(photo = photo) }
        }
    }

    fun answer(choice: Int) {
        val asking = _state.value as? QuizUiState.Asking ?: return
        if (asking.submitting) return
        _state.value = asking.copy(submitting = true)
        viewModelScope.launch {
            val radar = tracker.state.value
            val result = catchManager.submit(
                plane = asking.plane,
                question = asking.question,
                choice = choice,
                catchCenter = radar.center ?: asking.plane.aircraft.position,
                jumpCode = radar.jumpTarget?.code,
            )
            _state.value = when (result) {
                is CatchResult.Caught -> QuizUiState.Caught(result.plane)
                is CatchResult.Missed -> QuizUiState.Missed(asking.question, choice, result.retryAtMs)
            }
        }
    }
}
