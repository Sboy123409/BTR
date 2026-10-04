package com.example.btrapp.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.btrapp.BtrApplication
import com.example.btrapp.avatar.AvatarConfig
import com.example.btrapp.avatar.AvatarStore
import com.example.btrapp.habits.data.DayChecklist
import com.example.btrapp.habits.data.HabitsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** Joins the avatar with today's habit progress, which sets the avatar's mood. */
class HubViewModel(avatarStore: AvatarStore, habits: HabitsRepository) : ViewModel() {

    val avatar: StateFlow<AvatarConfig> = avatarStore.config

    val today: StateFlow<DayChecklist?> = habits.todayChecklist()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as BtrApplication
                HubViewModel(AvatarStore.get(app), app.habitsGraph.repository)
            }
        }
    }
}
