# Dataset de imágenes de prueba (O6)

Imágenes marcador (placeholder) para validar el pipeline de captura → OCR →
parsing. Son PNG pequeños (360×640) que simulan el layout de cada pantalla;
no deben usarse para validar precisión del OCR.

## Cómo se usan

1. Copiar una captura real de pantalla de Uber Driver al dispositivo/emulador.
2. Nombrarla según el escenario (p. ej. `offer_uberx_1.png`).
3. Ejecutar la prueba manual de OCR del pipeline apuntando a esa imagen.

## Escenarios de Uber (O6)

| Archivo               | Pantalla simulada          | Tipo esperado (OfferType) |
|-----------------------|----------------------------|---------------------------|
| `offer_uberx_1.png`   | Solicitud estándar (X)     | `UBER_REQUEST`            |
| `offer_comfort_1.png` | Solicitud Comfort          | `UBER_REQUEST`            |
| `offer_moto_1.png`    | Solicitud Uber Moto        | `UBER_MOTO`               |
| `offer_xl_1.png`      | Solicitud Uber XL          | `UBER_XL`                 |
| `offer_reservation_1.png` | Viaje reservado/programado | `UBER_RESERVATION`     |
| `offer_radar_1.png`   | Radar (explorar mapa)      | `UBER_RADAR`              |
| `offer_bonus_1.png`   | Promoción/bono activo      | `UBER_REQUEST`            |
| `offer_night_1.png`   | Oferta nocturna            | `UBER_REQUEST`            |
| `offer_invalid_1.png` | Pantalla sin oferta        | `GENERIC` (sin oferta)    |

## Otras plataformas

| Archivo               | Pantalla simulada |
|-----------------------|-------------------|
| `offer_cabify_1.png`  | Cabify            |
| `offer_didi_1.png`    | DiDi              |
| `offer_indrive_1.png` | inDrive           |
| `offer_uber_1.png`    | Uber genérico     |

## Flujo esperado para cada escenario

- `offer_uberx_1` / `offer_comfort_1` / `offer_bonus_1` / `offer_night_1`:
  detectan `REQUEST`, el parser de solicitud extrae la oferta y el overlay
  muestra una recomendación accionable (confianza HIGH/MEDIUM).
- `offer_moto_1`: detecta `REQUEST`, parser `UberMotoParser` → `UBER_MOTO`.
- `offer_xl_1`: detecta `REQUEST`, parser `UberXlParser` → `UBER_XL`.
- `offer_reservation_1`: detecta `REQUEST`, parser `UberReservationParser` →
  `UBER_RESERVATION`.
- `offer_radar_1`: detecta `REQUEST`, parser `UberRadarParser` → `UBER_RADAR`.
- `offer_invalid_1`: el pipeline no produce oferta (no es una solicitud).
