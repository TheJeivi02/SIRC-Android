# SPRINT 04 — Prueba manual (Plataforma de Captura)

> Objetivo: validar la infraestructura de captura (Sprint 4) en un dispositivo
> o emulador. No hay OCR ni parser real todavía: los snapshots son simulados
> (`FakeParser`).

## Requisitos

- APK de depuración (`app/build/outputs/apk/debug/app-debug.apk`) instalado.
- Dispositivo/emulador con Android 7.0+ (minSdk 24).
- Permiso de accesibilidad otorgado a **SIRC**.
- Tener instalada al menos una plataforma soportada (Uber, DiDi, Cabify o
  InDrive) para observar eventos reales. Sin ella, el panel mostrará estado
  pero sin eventos.

## Preparación (opcional pero recomendada)

1. Completar el onboarding (primera apertura) para entrar a la app principal.

## 1. Panel de depuración

1. Abre la app y navega al destino **Debug** (quinto ítem, icono de llave).
   - Si el ítem no aparece, desinstala/borra datos y revisa que el flag
     `DEBUG_PANEL` esté activo (default en dev).
2. Verifica la sección **Feature Flags**: deben aparecer los 5 flags
   (`ACCESSIBILITY`, `OVERLAY`, `CAPTURE`, `PARSER`, `DEBUG_PANEL`), todos
   activados.
3. Togglea `DEBUG_PANEL` a OFF: el ítem Debug debe desaparecer de la barra
   inferior. Vuelve a activarlo (puedes usar la ruta si sigues en la pantalla;
   si la dejaste, reabre la app).
4. Togglea `PARSER` a OFF y vuelve a ON: el estado "Parser" cambia en la
   sección **Estado de infraestructura**.

## 2. Estado de infraestructura

1. En **Estado de infraestructura** verifica:
   - **Accessibility**: 🟢 si el servicio de SIRC está activo en Ajustes >
     Accesibilidad; 🔴 si no.
   - **Overlay en ejecución**: 🟢 tras iniciar el overlay desde Inicio o
     Diagnóstico.
   - **Captura activa**: 🟢 cuando la captura está iniciada (default al abrir
     la app).
   - **Parser**: refleja el flag `PARSER`.

## 3. Flujo de captura con accesibilidad

1. Con la app SIRC en el panel **Debug** y la captura activa, abre **Uber**
   (o DiDi/Cabify/InDrive) con el servicio de accesibilidad habilitado.
2. Navega por la app de la plataforma (pantalla de inicio, búsqueda, ofertas).
   Cada cambio relevante de ventana debe registrar un evento.
3. Vuelve a SIRC → **Debug**:
   - **Eventos procesados** debe crecer.
   - **Eventos recientes** muestra los últimos eventos (paquete, tipo,
     nº de textos, "hace X s").
   - **Sesión activa** muestra el paquete de la plataforma con estado `ACTIVE`
     y el número de snapshots.
   - **Último snapshot** muestra un snapshot `FAKE` con valores simulados
     (monto 125, 8.5 km, 22 min) y su hora.
   - **Tiempo de procesamiento** muestra un valor pequeño (sub-milisegundo).
4. Cambia de plataforma (Uber → DiDi): debe abrirse una **nueva sesión**.

## 4. Control de captura

1. En la sección **Captura**, pulsa **Detener**: "Captura activa" pasa a 🔴 y
   "Captura en ejecución" se apaga.
2. Pulsa **Iniciar** para reanudar.
3. Pulsa **Limpiar**: se vacían eventos, snapshots y la sesión activa.

## 5. Toggle de CAPTURE

1. Con `CAPTURE` en OFF, abre la plataforma y navega: no deben registrarse
   eventos nuevos (la captura ignora todo).
2. Vuelve a ON y navega: los eventos reaparecen.

## 6. Memoria y logging

1. **Memoria aproximada** muestra un valor en MB que varía con el uso de la app
   (no debe crecer sin límite: el buffer de snapshots está acotado a 50 y los
   eventos recientes a 20).
2. Con `adb logcat -s OfferCapture:D`, al capturar deben aparecer líneas del
   coordinador. En un build release (minificado) no debe haber logs.

## Criterios de aceptación (verificación)

- [ ] El panel Debug muestra los 5 flags y sus toggles funcionan.
- [ ] Estado de accesibilidad/overlay/captura/parser se refleja correctamente.
- [ ] Abrir una plataforma con accesibilidad activa produce eventos, sesión y
      snapshot `FAKE`.
- [ ] Detener/limpiar captura funciona.
- [ ] `CAPTURE` y `PARSER` en OFF bloquean eventos/snapshots.
- [ ] La app y el overlay existentes siguen funcionando (regresión: overlay
      simulado cada 20 s, historial, diagnóstico).
- [ ] Sin accesibilidad activa, el panel muestra estado correcto sin eventos.
