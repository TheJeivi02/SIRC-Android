# TASK — Estado en vivo

> Actualiza este archivo al iniciar y terminar cada tarea relevante. Es el
> estado que cualquier agente lee primero para saber qué sigue sin re-leer el
> proyecto.

## Tarea actual

Verificación en emulador + **fix del crash del Overlay** (regresión de
`6d62dba`). El overlay vuelve a crashear en instalación limpia; se corrige la
propagación de owners de ViewTree (Lifecycle y SavedState) y se verifica la app
en el emulador `Pixel_7_API_35`.

## Objetivo

1. Ejecutar el emulador, compilar e instalar la app actualizada.
2. Reproducir y arreglar el crash al iniciar el overlay ("SIRC continúa
   fallando": la app se minimiza y se cierra).
3. Verificar que no quedan errores: overlay estable (iniciar/detener), teclado
   en todos los campos de Ajustes, checks de calidad en verde.

## Archivos involucrados

- `feature/overlay/.../OverlayService.kt` (fix del crash + ktlint)
- `core/capture/android/.../ProjectionLifecycleTest.kt` (fix ktlint preexistente)

## Progreso

- [x] Emulador `Pixel_7_API_35` ejecutado y app `assembleDebug` instalada.
- [x] Onboarding completado en emulador (Jeivi / Mexico / CDMX / MXN ·
      Corolla/Toyota/2020/Gasolina/12 L · 24.5 / 0.5 · Uber+DiDi · 5 / 150).
- [x] Permiso de overlay concedido; diagnóstico muestra overlay activo.
- [x] Crash reproducido en logcat (3 causas encadenadas):
      1. `ViewTreeLifecycleOwner.set` recibía `LifecycleRegistry` (no
         `LifecycleOwner`) → `IllegalArgumentException` capturada por try/catch,
         pero el ComposeView se agregaba sin owner → FATAL.
      2. Tras corregir (1), FATAL `Composed into the View which doesn't
         propagateViewTreeSavedStateRegistryOwner!` (Compose 1.7.6 exige
         `ViewTreeSavedStateRegistryOwner`).
      3. Los imports directos de `androidx.savedstate` fallan en compile time
         (metadata KMP, igual que `ViewTree*` de lifecycle).
- [x] Fix: `ViewTreeLifecycleOwner.set(view, lifecycleOwner)` (propiedad real,
      no el registry); `ViewTreeSavedStateRegistryOwner.set(view, proxy)` vía
      reflexión; `SavedStateRegistry` creado por reflexión con `isRestored=true`
      (no hay estado persistido en un overlay). Nada de `SavedStateRegistryController`.
- [x] Verificación funcional en emulador: overlay inicia sin FATAL
      ("Overlay en ejecución: Activo"), ciclo detener→iniciar OK, MainActivity
      permanece en primer plano.
- [x] Teclado verificado (`mInputShown=true`) en Costos del conductor (Costo por
      km) y Umbrales de decisión (Ganancia por km/hora) → el bug del teclado
      reportado NO se reproduce en el emulador (candidato a dispositivo/IME real).
- [x] Checks de calidad en verde: `ktlintCheck`, `lintDebug`, `assembleDebug`,
      `testDebugUnitTest`, `:domain:test`, `:core:platform:test`,
      `:core:capture:test`, `:feature:overlay:testDebugUnitTest`.
- [x] Fix de ktlint preexistente (`ProjectionLifecycleTest.kt` línea 9).
- [ ] Commit del fix (pendiente de confirmación del usuario).

## Notas / decisiones

- Los `ViewTree*` y las clases de `androidx.savedstate` NO se resuelven en
  compile time desde `:feature:overlay` (classpath KMP metadata). Toda la
  propagación de owners se hace por reflexión en `ensureOverlay()`.
- `LifecycleRegistry.createUnsafe(owner)` se mantiene en `init` con
  `lifecycleOwner` como propiedad `lateinit`; la reflexión usa `lifecycleOwner`
  (la interfaz), no el registry.
- `SavedStateRegistry` es `final` y su constructor es `internal` desde la
  metadata KMP; se instancia con `getDeclaredConstructor()` + `isAccessible`.
  `consumeRestoredStateForKey` lanza si `isRestored=false`, por eso se marca
  `isRestored=true` por reflexión (no hay estado real que restaurar).
- `SavedStateRegistryController` NO se usa (causa "Restarter must be created
  only during owner's initialization stage" en Service).
- El emulador arrancó con el paquete `com.sirc.app` deshabilitado (por el crash
  previo); hubo que `pm enable com.sirc.app` antes de `am start`.

## Verificación

- `.\gradlew.bat ktlintCheck --console=plain`
- `.\gradlew.bat lintDebug assembleDebug testDebugUnitTest :domain:test :core:platform:test :core:capture:test :feature:overlay:testDebugUnitTest --console=plain`
- Emulador: overlay inicia/detiene sin FATAL; `dumpsys window` muestra la
  ventana `TYPE_APPLICATION_OVERLAY` de `com.sirc.app`.

## Próximos pasos

1. Commitear el fix (confirmar con el usuario).
2. Si el usuario reporta de nuevo el teclado en dispositivo real, pedir
   `adb logcat` / reproducción con el IME real.
