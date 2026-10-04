package com.example.btrapp.habits.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.btrapp.core.DaysMask
import com.example.btrapp.habits.HabitsGraphProvider
import com.example.btrapp.habits.data.DayChecklist
import com.example.btrapp.habits.data.HabitSummary
import com.example.btrapp.habits.data.HabitsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Builds a ViewModel from the habits repository (and the nav-arg [SavedStateHandle]). */
internal inline fun <reified VM : ViewModel> habitsFactory(
    crossinline create: (HabitsRepository, SavedStateHandle) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        val graph = (this[APPLICATION_KEY] as HabitsGraphProvider).habitsGraph
        create(graph.repository, createSavedStateHandle())
    }
}

/** Shared by the Habits tab and the Home card: tick things off on one day. */
abstract class ChecklistViewModel(protected val repository: HabitsRepository) : ViewModel() {
    abstract val checklist: StateFlow<DayChecklist?>

    fun setHabitChecked(habitId: Long, epochDay: Long, checked: Boolean) {
        viewModelScope.launch { repository.setHabitChecked(habitId, epochDay, checked) }
    }

    fun setTaskDone(id: Long, done: Boolean) {
        viewModelScope.launch { repository.setTaskDone(id, done) }
    }
}

class TodayChecklistViewModel(repository: HabitsRepository) : ChecklistViewModel(repository) {
    override val checklist: StateFlow<DayChecklist?> = repository.todayChecklist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalCoroutinesApi::class)
class HabitsViewModel(repository: HabitsRepository) : ChecklistViewModel(repository) {

    val today: Long get() = repository.today()

    val selectedDay = MutableStateFlow(repository.today())

    override val checklist: StateFlow<DayChecklist?> = selectedDay
        .flatMapLatest { repository.checklistFor(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun addTask(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        val day = selectedDay.value
        viewModelScope.launch { repository.addTask(trimmed, day) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repository.deleteTask(id) }
    }
}

class ManageHabitsViewModel(repository: HabitsRepository) : ViewModel() {
    val summaries: StateFlow<List<HabitSummary>> = repository.summaries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

class HabitEditorViewModel(
    private val repository: HabitsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val habitId: Long = savedStateHandle.get<Long>(ARG_HABIT_ID) ?: NEW_HABIT
    val isNew = habitId == NEW_HABIT

    var title by mutableStateOf("")
    var emoji by mutableStateOf("")
    var daysMask by mutableIntStateOf(DaysMask.EVERY_DAY)

    init {
        if (!isNew) {
            viewModelScope.launch {
                val habit = repository.getHabit(habitId) ?: return@launch
                title = habit.title
                emoji = habit.emoji.orEmpty()
                daysMask = habit.daysMask
            }
        }
    }

    val validationError: String?
        get() = when {
            title.isBlank() -> "Give it a name"
            daysMask == 0 -> "Pick at least one day"
            else -> null
        }

    fun toggleDay(isoDay: Int) {
        daysMask = DaysMask.toggle(daysMask, isoDay)
    }

    fun save(onSaved: () -> Unit) {
        if (validationError != null) return
        viewModelScope.launch {
            repository.saveHabit(habitId, title.trim(), emoji.trim().ifEmpty { null }, daysMask)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
            onDeleted()
        }
    }

    companion object {
        const val ARG_HABIT_ID = "habitId"
        const val NEW_HABIT = 0L
    }
}
