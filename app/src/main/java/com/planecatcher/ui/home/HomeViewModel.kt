package com.planecatcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planecatcher.core.radar.NearbyPlane
import com.planecatcher.core.rules.GameRules
import com.planecatcher.core.rules.JumpStatus
import com.planecatcher.core.rules.JumpTarget
import com.planecatcher.data.PhotoRepository
import com.planecatcher.data.PlanePhoto
import com.planecatcher.data.local.CaughtPlaneDao
import com.planecatcher.data.local.QuizLockDao
import com.planecatcher.data.prefs.UserPrefs
import com.planecatcher.data.prefs.UserSettings
import com.planecatcher.data.time.TrustedClock
import com.planecatcher.core.progress.ProgressSummary
import com.planecatcher.domain.JumpManager
import com.planecatcher.domain.ProgressRepository
import com.planecatcher.feedback.Feedback
import com.planecatcher.feedback.Sfx
import com.planecatcher.domain.RadarState
import com.planecatcher.domain.RadarTracker
import com.planecatcher.domain.ticker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PopupState(
    val plane: NearbyPlane,
    val photo: PlanePhoto?,
    val photoLoading: Boolean,
    val caught: Boolean,
    /** Non-null while the plane's quiz is locked after a wrong answer. */
    val lockedUntilMs: Long?,
)

data class HomeUiState(
    val radar: RadarState = RadarState(),
    val jump: JumpStatus = JumpStatus.Ready,
    val caughtHexes: Set<String> = emptySet(),
    val locks: Map<String, Long> = emptyMap(),
    val settings: UserSettings = UserSettings(),
    val popup: PopupState? = null,
    val nowMs: Long = 0L,
) {
    fun lockedUntil(hex: String): Long? = locks[hex]?.takeIf { it > nowMs }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val tracker: RadarTracker,
    private val jumpManager: JumpManager,
    private val photos: PhotoRepository,
    private val clock: TrustedClock,
    private val feedback: Feedback,
    progressRepository: ProgressRepository,
    caughtDao: CaughtPlaneDao,
    lockDao: QuizLockDao,
    prefs: UserPrefs,
) : ViewModel() {

    private data class PopupSelection(val plane: NearbyPlane, val photo: PlanePhoto?, val loading: Boolean)

    private val selection = MutableStateFlow<PopupSelection?>(null)
    private val dismissedAt = mutableMapOf<String, Long>()
    private val autoShown = mutableSetOf<String>()
    private var photoJob: Job? = null
    private var holding = false
    /** A plane a notification asked us to show, waiting for the radar to report it. */
    private var pendingHex: String? = null

    private val caught = caughtDao.observeHexes().map { it.toSet() }
    private val locks = lockDao.observeAll().map { list -> list.associate { it.planeHex to it.retryAt } }
    private val now = ticker().map { clock.now() }

    val state: StateFlow<HomeUiState> = combine(
        combine(tracker.state, jumpManager.status, caught, ::Triple),
        combine(locks, prefs.settings, selection, now) { l, s, sel, n -> Quad(l, s, sel, n) },
    ) { (radar, jump, caughtSet), (lockMap, settings, sel, nowMs) ->
        val popup = sel?.let { s ->
            // Keep the pop-up's distance and position live while the plane is still tracked.
            val live = radar.planes.firstOrNull { it.hex == s.plane.hex } ?: s.plane
            PopupState(
                plane = live,
                photo = s.photo,
                photoLoading = s.loading,
                caught = live.hex in caughtSet,
                lockedUntilMs = lockMap[live.hex]?.takeIf { it > nowMs },
            )
        }
        HomeUiState(radar, jump, caughtSet, lockMap, settings, popup, nowMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    val progress: StateFlow<ProgressSummary?> = progressRepository.summary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Auto pop-up: the nearest uncaught, unlocked plane that hasn't been shown or recently dismissed.
        viewModelScope.launch {
            combine(tracker.state, caught, locks, ::Triple).collect { (radar, caughtSet, lockMap) ->
                pendingHex?.let { hex ->
                    radar.planes.firstOrNull { it.hex == hex }?.let {
                        pendingHex = null
                        open(it)
                        return@collect
                    }
                }
                if (selection.value != null) return@collect
                val t = clock.now()
                val candidate = radar.planes.firstOrNull { p ->
                    p.inRange &&
                        p.hex !in caughtSet &&
                        p.hex !in autoShown &&
                        (lockMap[p.hex] ?: 0L) <= t &&
                        (dismissedAt[p.hex]?.let { t - it > GameRules.DISMISS_SNOOZE_MS } ?: true)
                }
                if (candidate != null) {
                    autoShown += candidate.hex
                    open(candidate)
                    feedback.play(Sfx.PING)
                }
            }
        }
    }

    /** Start polling while the Home screen is visible. */
    fun onVisible() {
        if (!holding) {
            holding = true
            tracker.acquire()
        }
    }

    fun onHidden() {
        if (holding) {
            holding = false
            tracker.release()
        }
    }

    fun onPlaneTapped(hex: String) {
        tracker.state.value.planes.firstOrNull { it.hex == hex }?.let(::open)
    }

    /** Opens the pop-up for [hex] now, or as soon as the radar sees it (e.g. from a notification). */
    fun requestPopup(hex: String) {
        val plane = tracker.state.value.planes.firstOrNull { it.hex == hex }
        if (plane != null) open(plane) else pendingHex = hex
    }

    fun dismissPopup() {
        selection.value?.let { dismissedAt[it.plane.hex] = clock.now() }
        selection.value = null
        photoJob?.cancel()
    }

    fun refresh() = tracker.refreshNow()

    fun activateJump(target: JumpTarget) {
        viewModelScope.launch { jumpManager.activate(target) }
    }

    fun endJump() {
        viewModelScope.launch { jumpManager.endEarly() }
    }

    private fun open(plane: NearbyPlane) {
        photoJob?.cancel()
        val cachedPhoto = photos.cached(plane.hex)
        selection.value = PopupSelection(plane, cachedPhoto, loading = cachedPhoto == null)
        if (cachedPhoto == null) {
            photoJob = viewModelScope.launch {
                val photo = photos.photoFor(plane.hex)
                selection.value = selection.value
                    ?.takeIf { it.plane.hex == plane.hex }
                    ?.copy(photo = photo, loading = false)
                    ?: selection.value
            }
        }
    }

    override fun onCleared() {
        onHidden()
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
