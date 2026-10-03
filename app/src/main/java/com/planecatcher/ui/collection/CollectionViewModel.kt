package com.planecatcher.ui.collection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.planecatcher.core.model.Tier
import com.planecatcher.data.local.CaughtPlaneDao
import com.planecatcher.data.local.CaughtPlaneEntity
import com.planecatcher.core.progress.ProgressSummary
import com.planecatcher.data.prefs.UserPrefs
import com.planecatcher.domain.ProgressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

enum class CollectionSort(val label: String) { NEWEST("Newest"), RARITY("Rarity") }

data class CollectionUiState(
    val planes: List<CaughtPlaneEntity> = emptyList(),
    val totalCaught: Int = 0,
    val totalPoints: Int = 0,
    val countByTier: Map<Tier, Int> = emptyMap(),
    val tierFilter: Tier? = null,
    val sort: CollectionSort = CollectionSort.NEWEST,
    val loaded: Boolean = false,
)

@HiltViewModel
class CollectionViewModel @Inject constructor(
    dao: CaughtPlaneDao,
    progressRepository: ProgressRepository,
) : ViewModel() {
    val progress: StateFlow<ProgressSummary?> = progressRepository.summary
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val filter = MutableStateFlow<Tier?>(null)
    private val sort = MutableStateFlow(CollectionSort.NEWEST)

    val state: StateFlow<CollectionUiState> = combine(dao.observeAll(), filter, sort) { all, f, s ->
        val shown = all
            .filter { f == null || it.tier == f }
            .let { list ->
                when (s) {
                    CollectionSort.NEWEST -> list.sortedByDescending { it.caughtAt }
                    CollectionSort.RARITY -> list.sortedWith(
                        compareByDescending<CaughtPlaneEntity> { it.tier.ordinal }.thenByDescending { it.caughtAt },
                    )
                }
            }
        CollectionUiState(
            planes = shown,
            totalCaught = all.size,
            totalPoints = all.sumOf { it.points },
            countByTier = all.groupingBy { it.tier }.eachCount(),
            tierFilter = f,
            sort = s,
            loaded = true,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CollectionUiState())

    fun setFilter(tier: Tier?) { filter.value = tier }
    fun setSort(s: CollectionSort) { sort.value = s }
}

@HiltViewModel
class PlaneDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    dao: CaughtPlaneDao,
    prefs: UserPrefs,
) : ViewModel() {
    private val hex: String = checkNotNull(savedState["hex"])

    val plane: StateFlow<CaughtPlaneEntity?> = dao.observe(hex)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val useMetric: StateFlow<Boolean> = prefs.settings.map { it.useMetric }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
}
