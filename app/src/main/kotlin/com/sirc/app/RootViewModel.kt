package com.sirc.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sirc.domain.usecase.GetDriverConfigUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class RootViewModel @Inject constructor(
    getDriverConfig: GetDriverConfigUseCase,
) : ViewModel() {
    /** null = cargando; false = falta configuración inicial; true = ya configurado. */
    val isConfigured: StateFlow<Boolean?> =
        getDriverConfig
            .observeIsConfigured()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )
}
