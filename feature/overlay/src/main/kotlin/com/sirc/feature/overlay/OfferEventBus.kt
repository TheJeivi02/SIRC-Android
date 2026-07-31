package com.sirc.feature.overlay

import com.sirc.domain.model.TripOffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Puente en memoria entre el Accessibility Service y el Overlay Service.
 *
 * `null` significa "no hay oferta visible": el overlay debe ocultarse.
 */
@Singleton
class OfferEventBus @Inject constructor() {
    private val _offer = MutableStateFlow<TripOffer?>(null)
    val offer: StateFlow<TripOffer?> = _offer.asStateFlow()

    fun onOffer(offer: TripOffer) {
        _offer.value = offer
    }

    fun clearOffer() {
        _offer.value = null
    }
}
