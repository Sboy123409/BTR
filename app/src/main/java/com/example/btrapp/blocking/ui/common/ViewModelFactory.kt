package com.example.btrapp.blocking.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.btrapp.AppContainer
import com.example.btrapp.BtrApplication

/** Builds a ViewModel from the [AppContainer] (and the nav-arg [SavedStateHandle]). */
inline fun <reified VM : ViewModel> containerFactory(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        create(container(), createSavedStateHandle())
    }
}

fun CreationExtras.container(): AppContainer = (this[APPLICATION_KEY] as BtrApplication).container
