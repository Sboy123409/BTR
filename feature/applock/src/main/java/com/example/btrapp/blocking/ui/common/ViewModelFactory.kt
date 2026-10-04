package com.example.btrapp.blocking.ui.common

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.btrapp.blocking.AppLockGraph
import com.example.btrapp.blocking.AppLockGraphProvider

/** Builds a ViewModel from the [AppLockGraph] (and the nav-arg [SavedStateHandle]). */
inline fun <reified VM : ViewModel> containerFactory(
    crossinline create: (AppLockGraph, SavedStateHandle) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        create(container(), createSavedStateHandle())
    }
}

fun CreationExtras.container(): AppLockGraph =
    (this[APPLICATION_KEY] as AppLockGraphProvider).appLockGraph
