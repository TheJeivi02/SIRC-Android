package com.sirc.core.platform

/**
 * Estado de pantalla detectado por OCR antes de intentar parsear una oferta.
 *
 * El [OfferDetectionEngine] clasifica el texto visible para saber si hay una
 * oferta evaluable (REQUEST), otra pantalla del conductor (HOME/TRIP/
 * NAVIGATION/OFFLINE) o un error de la app. Solo REQUEST debe producir
 * ofertas evaluables.
 */
enum class ScreenType {
    /** Texto sin patrones reconocibles o fuera del dominio del conductor. */
    UNKNOWN,

    /** Pantalla de inicio: conectado, buscando viajes, sin oferta activa. */
    HOME,

    /** Solicitud/oferta entrante evaluable (precio, distancia, tiempo). */
    REQUEST,

    /** Viaje en curso (recogida o con pasajero a bordo). */
    TRIP,

    /** Navegación hacia el punto de recogida o destino. */
    NAVIGATION,

    /** App desconectada o sin conexión; no está recibiendo solicitudes. */
    OFFLINE,

    /** Pantalla de error de la app (no del dispositivo). */
    ERROR,
}
