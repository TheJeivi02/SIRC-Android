# Cumplimiento de políticas de Google Play

Este documento fija las reglas de la aplicación respecto a las políticas
actuales de Google Play (Accesibilidad, Overlay, servicios en primer plano,
privacidad y permisos).

## 1. Política de Accessibility Service

Google Play exige que todo uso del `AccessibilityService` esté **justificado**,
sea **necesario** para una función central de la app y se declare su propósito.

Declaración (SIRC):

- **Propósito**: ayudar al conductor a decidir si una oferta de viaje es
  rentable, calculando indicadores a partir de la información **visible**.
- **Uso permitido**: únicamente **lectura** del contenido de pantalla
  (`canRetrieveWindowContent="true"`).
- **Prohibido explícitamente en el código**:
  - `canPerformGestures="false"` — sin gestos ni toques simulados.
  - `canRequestFilterKeyEvents="false"` — sin captura de teclas.
  - No hay `AccessibilityNodeInfo.performAction(...)` en el código.
  - No se automatiza la aceptación/rechazo de viajes.
  - No se envía ningún dato fuera del dispositivo (todo queda local).

La declaración de propósito se encuentra en
`feature/overlay/src/main/res/xml/accessibility_service_config.xml` y en el
formulario de Play (ficha de la app), y debe mantenerse sincronizada.

## 2. Overlay sobre otras apps

- Se usa `SYSTEM_ALERT_WINDOW` + `TYPE_APPLICATION_OVERLAY`, el mecanismo
  estándar y compatible con Play.
- El permiso se solicita explícitamente al usuario (pantalla de permisos del
  sistema). La app no funciona sin él: se muestra el estado en Home.
- El overlay **nunca intercepta toques destinados a otras apps**
  (`FLAG_NOT_FOCUSABLE`). Su único toque propio es el botón de cerrar y el
  arrastre del propio indicador.

## 3. Servicio en primer plano (Foreground Service)

- El overlay corre en un FGS para no ser detenido al minimizar la app.
- Android 14+: `foregroundServiceType="specialUse"` con subtipo descriptivo
  declarado vía `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`.
- El **`MediaProjectionService`** (`:core:capture:android`) corre como FGS de
  tipo **`mediaProjection`** (Android 14+) con su propio
  `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; arranca con
  `startForegroundService` justo después del consentimiento del usuario y se
  detiene al terminar la proyección.
- Notificación obligatoria en un canal de **importancia baja** (`IMPORTANCE_LOW`).
- Android 13+: se solicita `POST_NOTIFICATIONS` en tiempo de ejecución.

## 3bis. Captura de pantalla (MediaProjection)

- La captura usa **MediaProjection** (`MediaProjectionManager`): la **única**
  forma de obtener imágenes de pantalla es el consentimiento explícito del
  sistema mostrado al usuario (nunca se captura en segundo plano sin su
  permiso).
- La proyección se solicita solo desde la pantalla Home (botón "Permitir
  captura de pantalla"); el usuario puede detenerla en cualquier momento.
- **Uso declarado en Play Console**: analizar localmente el contenido visible
  de las ofertas para calcular su rentabilidad; todo el procesamiento es
  local (OCR con ML Kit on-device) y **no se transmite, almacena ni comparte**
  ninguna imagen ni texto fuera del dispositivo.
- Sin proyección activa, el pipeline degrada a los textos del servicio de
  accesibilidad (no hay imagen capturada).

## 4. Permisos

| Permiso | Uso | Política |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Dibujar el indicador | Consentimiento explícito del usuario |
| `BIND_ACCESSIBILITY_SERVICE` | Leer ofertas visibles | Propósito declarado (sección 1) |
| `FOREGROUND_SERVICE` / `SPECIAL_USE` | Mantener el overlay vivo | Uso declarado (sección 3) |
| `FOREGROUND_SERVICE` / `MEDIA_PROJECTION` | Mantener viva la captura de pantalla | Consentimiento explícito del sistema (sección 3bis) |
| `POST_NOTIFICATIONS` | Notificación del FGS | Runtime (Android 13+) |
| `QUERIES` (platformas) | Visibilidad de paquetes | Sin datos personales |

No se solicitan permisos de ubicación, contactos, cámara ni almacenamiento.

## 5. Privacidad y datos

- **Todo el procesamiento es local.** No hay backend, ni telemetría, ni SDK de
  anuncios, ni analytics de terceros.
- Datos guardados (Room, cifrado del propio Android): configuración del
  conductor y historial de ofertas evaluadas.
- El historial se puede borrar desde la pantalla de Historial.

## 6. Revisión previa al release

Antes de subir una versión a Play:

1. Ejecutar `./gradlew lintDebug` — no debe haber hallazgos de accesibilidad
   ni de permisos no declarados.
2. Verificar que el texto de `accessibility_service_config.xml` sigue
   describiendo fielmente el uso.
3. Confirmar que no hay llamadas a `performAction` ni `dispatchGesture`.
4. Verificar que la captura de pantalla solo ocurre tras el diálogo de
   consentimiento de MediaProjection y que las imágenes nunca salen del
   dispositivo (sección 3bis).
5. Completar la declaración en Play Console (Data safety): sin datos recolectados.
