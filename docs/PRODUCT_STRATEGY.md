# Estrategia de Producto SIRC

> Documento de dirección estratégica consolidado a partir de las TRES fuentes:
> **(1)** auditoría técnica del repositorio (Sprints 1–11, v1.0.0-rc1),
> **(2)** `docs/Informe Ejecutivo de Estrategia y Diseño…`,
> **(3)** `docs/PRODUCT_COMPETITIVE_ANALYSIS.md` (investigación de mercado
> verificada). Define la ruta de producto: diferenciación, prioridades,
> arquitectura futura, roadmap y el próximo Sprint decidido.

## 1. Posicionamiento estratégico

**SIRC = la app de decisión de rentabilidad legalista, instantánea y local-first.**

Pilares innegociables (derivan de FUENTE 1 + FUENTE 2 + FUENTE 3):

1. **Solo lectura, cero automatización.** Prohibido auto-clic, gestos,
   `performAction`, aceptar/rechazar por el conductor, contra-ofertas. La
   automatización es el mayor riesgo de baneo y viola la política de Play.
2. **Decisión <3 s con mínima carga cognitiva.** Una mirada, color semáforo,
   información derivada, sin duplicar lo que muestra la plataforma.
3. **LOCAL-FIRST (procesamiento local, servicios remotos mínimos).**
   El procesamiento de ofertas (OCR, parsing, evaluación, overlay, historial)
   es **100 % local y nunca sube datos de pantalla**; identidad, suscripción,
   entitlement e integridad usan servicios remotos mínimos (ver
   `docs/SECURITY_MODEL.md`). Reemplaza la frase "100 % local" para reflejar
   el modelo comercial (ver §1bis).
4. **Multi-plataforma real** (Uber, DiDi, InDrive, Cabify) como ruta de
   dominancia de la cohorte multi-app.
5. **Rendimiento y batería como seguridad vial** (jornadas de 12 h).
6. **Aplicación de pago por suscripción** (nueva restricción formal). El APK se
   considera manipulable; la autorización de features premium se decide en
   backend, no en el cliente (ver `docs/SECURITY_MODEL.md`).

### 1bis. LOCAL-FIRST vs "100 % local" (decisión del Roadmap Gate)

Tras el análisis de coherencia (seguridad × suscripción), **SIRC pasa de
"100 % local" a "LOCAL-FIRST"** con esta separación explícita:

| | Qué incluye | Ejemplos | ¿Dejaría de ser local? |
|---|---|---|---|
| **Procesamiento local** | Todo el pipeline de datos del conductor | OCR, parsing, evaluación, ganancia, $/h, $/km, recomendación, overlay, historial, dashboard | **Nunca**. Ninguna oferta capturada sale del dispositivo. |
| **Servicios remotos mínimos** | Solo estado comercial/de cuenta | Autenticación, suscripción, entitlement, verificación de compra (Play), integridad (Play Integrity), RTDN, recuperación de cuenta | Sí, y es aceptado: es el coste del modelo de negocio. |

Sin "100 % local" literal seguimos ganando el único "local" que importa para la
privacidad y el <3 s: **los datos de pantalla no salen del dispositivo**.

## 2. Matriz de diferenciación (ADOPTAR / MEJORAR / EVITAR / DIFERENCIAR)

Basada en la brecha detectada en `PRODUCT_COMPETITIVE_ANALYSIS.md` y las
recomendaciones de la FUENTE 2.

### ADOPTAR (features validados en el mercado)

| Feature | Origen | Nota de diseño |
|---|---|---|
| Modo nocturno con adaptación de contraste | Ruta Rentable | Contraste alta fidelidad, visión diurna/nocturna (FUENTE 2, §2.2). |
| Tendencias por día/semana/mes | Ruta Rentable | Ampliación del dashboard actual (`HistoryStatsCalculator`). |
| Gestión de vehículo / costos completos | Motorista One, informe | `DriverVehicle`/`DriverConfig` ya modelan costos; ampliar a ng todo el toolkit si se justifica. |
| Semáforo por meta configurable ($/h, $/km) | GigU / umbrales SIRC | Ya existe (`DriverConfig.thresholds`); confirma la dirección correcta. |

### MEJORAR (transformar lo actual en ventaja)

| Feature | Estado actual | Mejora dirigida |
|---|---|---|
| Overlay de decisión | Componentes semáforo ya funcionan | Reforzar principio de **una fijación ocular por 2 min** (FUENTE 2, §2.2): menos densidad, revisit do de layout. |
| Consumo de batería | Jornada optimizada, sin modo SOC-aware | **Ahorro de energía inteligente** (tier medio/bajo), refresh 1 Hz si el coche está detenido, OCR solo ante cambio de layout (FUENTE 2, §4.2). |
| Umbrales | Estáticos por km/hora | **Umbrales dinámicos** con resalte visual inmediato (FUENTE 2, §5.1). |

### EVITAR (prohibido por arquitectura/legal/política)

| Práctica | Por qué se evita |
|---|---|
| Auto-clic / auto-aceptar / contra-ofertas | Riesgo de baneo permanente; viola política de Google Play de accesibilidad; contradice la regla SOLO lectura. |
| Spoofing de GPS / rutas fraudulentas | Causal de desactivación (FUENTE 2, §3); SIRC alinea con ruta más eficiente. |
| Telemetría / backend | Rompe el pilar 100 % local y agrega riesgo de fuga de datos de pantalla. |

### DIFERENCIAR (donde SIRC gana sin que la competencia lo tenga)

| Diferenciador | Estado | Ventaja competitiva |
|---|---|---|
| Decisión <3 s + solo lectura simultáneos | Logrado | Ninguna app verificada combina ambos. |
| 100 % local declarado y auditable | Logrado | Confianza y cero dependencia de red. |
| Pipeline descriptor-driven multi-plataforma | Logrado (arq.) | Agregar plataforma = nuevo descriptor, no código. |
| Compatibilidad Google Play proactiva (Policy-exempt accessibility) | Documentada | `GOOGLE_PLAY_COMPLIANCE.md`; base para narrativa de confianza. |
| Play Integrity + declaración anti-baneo (futuro) | Pendiente P1 | Diferenciador de marca frente a automatizadores. |

## 3. Product Gap Analysis (estado actual → meta de producto)

| Dimensión | Estado (rc1) | Meta producto | Gap |
|---|---|---|---|
| Plataformas soportadas | Uber sólido; DiDi/InDrive/Cabify por descriptor | 4 plataformas en producción | Descriptores + datasets + test para las 3 restantes |
| Modo nocturno / contraste | Tema M3 fijo | Adaptación diurna/nocturna | Tema dinámico + sensor de luz (solo si justificado) |
| Dashboard | Historial + estadísticas básicas | AHU, tendencias diarias/semanales | `HistoryStatsCalculator` ampliado + más gráficos |
| Salud de batería | Optimizado, sin SOC-aware | Modo ahorro inteligente SOC-aware | Heurística de temperatura/estado de carga (futuro) |
| Integridad | Sin Play Integrity | Validación Strong + declaración de uso legítimo | Integrar Play Integrity API (Hito 1 FUENTE 2) |
| Anti-fatiga | No existe | Modo anti-fatiga (alertas suaves por tiempo conectado) | Nuevo módulo/pantalla (futuro) |

## 4. Prioridades de producto (P0–P3) — REVISADAS (Roadmap Gate)

Prioridad revisada al cruzar las tres fuentes **+ el modelo de suscripción y
seguridad**. La seguridad comercial se clasifica según riesgo real: NO se pone
en P0 (la evidencia de una beta sin cobro no la justifica aún), pero TAMPOCO
deja de estar antes del lanzamiento público (no queda en P3).

### P0 — Robustez y cumplimiento (antes de la beta; supervivencia)

- Entregar la beta controlada del núcleo de producto: overlay <3 s, OCR,
  estabilidad Android 10–15, plataformas soportadas en ALPHA.
- Política de Play y anti-baneo intactos (solo lectura, sin clics).
- `BETA_READINESS.md` §2 cumplida (checklist de entrada a beta).

### P1 — Lanzamiento comercial seguro (necesario para el lanzamiento público; E1b)

1. **Entitlement server + Play Billing (suscripciones)** con verificación de
   token en servidor y RTDN (revocación en tiempo real).
2. **Play Integrity (Standard)** como señal combinada (appRecognition +
   licensing + deviceIntegrity tiered) — **no como barrera de suscripción**.
3. **Play Integrity + entitlement offline con TTL corto** (DOC `SECURITY_MODEL`).
4. **Cierre de descriptores multi-plataforma** (DiDi, InDrive, Cabify) para el
   lanzamiento.

### P2 — Diferenciación de ciclo medio (después del lanzamiento)

5. **Modo nocturno / contraste adaptativo** (adoptar de Ruta Rentable).
6. **Dashboard de AHU** y tendencias por día/semana/mes (FUENTE 2, §5.2).
7. **Ahorro de energía inteligente** (SOC/temperatura; refresh 1 Hz detenido).

### P3 — Profesionalización (largo plazo)

8. **Modo anti-fatiga** (alertas suaves por tiempo conectado).
9. **Ecosistema Lite/Pro** con compartición segura de datos de rentabilidad
   (FUENTE 2, §3.2, Key Sharing API — cuando exista el segundo producto).
10. **Android 16 Ready** (Safer Intents, Ordered Broadcasts priority) —
    mantenimiento continuo.

## 5. Arquitectura de producto futura (bloques conceptuales)

Vista conceptual de cómo evoluciona el mapa de módulos actual sin romper
arquitectura (dependencias hacia adentro; `:domain`, `:core:platform`,
`:core:capture` Kotlin puro).

```
┌─────────────── app / feature-ui ───────────────┐
│  onboarding · settings · history/dashboard     │
│  overlay (decisión <3s) · [anti-fatiga]        │
└──────┬──────────────────────────────────────────┘
       ▼
┌─────────────── feature:overlay (orquestación) ─┐
│  accesibilidad sololectura · captura · pipeline│
└──────┬──────────────────────────────────────────┘
       ▼
┌ core:capture · core:capture:android (Kotlin puro) ┐
│  CaptureInput unificado · OCR · caché · debounce  │
└──────┬─────────────────────────────────────────────┘
       ▼
┌────────────── core:platform (Kotlin puro) ──────┐
│  PlatformDetectionEngine · descriptor registry  │
│  (Uber/DiDi/InDrive/Cabify) · parser            │
└──────┬───────────────────────────────────────────┘
       ▼
┌─────────────────── domain (Kotlin puro) ────────┐
│  ProfitEngine · evaluación · reglas · AHU       │
│  [Play Integrity wrapper · SOC manager (futuro)]│
└──────────────────────────────────────────────────┘
```

Bloques futuros (marcados con [ ]) se incorporan como nuevos contratos en
`:domain` + implementaciones Android en `:feature:*` o `:data`, respetando la
separación actual. Play Integrity y el ahorro SOC-aware se modelarán detrás de
interfaces de dominio para mantener los módulos puras testeables.

## 6. Roadmap por etapas (cruzando las 3 fuentes + Roadmap Gate)

> Revisado por el gate de coherencia (16-ago-2026): E1 se divide en E1a/E1b
> para no desplegar la seguridad comercial antes de validar el núcleo, pero
> tampoco dejarla para el final. Detalle completo en `docs/ROADMAP.md` y
> `docs/BETA_READINESS.md`.

| Etapa | Alcance | Fuentes que la justifican |
|---|---|---|
| **E0 — Cierre técnico (completado, Sprint 11)** | Remediación, auditoría, RC1 verde. | FUENTE 1 (estado real). |
| **E1a — Beta controlada (núcleo de producto)** | Beta cerrada sin monetización: overlay <3 s en campo, OCR, estabilidad, Play track; **Sprint 12**. | FUENTE 1 (RC1 listo); gate: validar producto antes de monetizar. |
| **E1b — Integración comercial** | Suscripción (Play Billing) + entitlement server + Play Integrity (Standard) + RTDN + backend de cuenta. | FUENTE 2 Hito 1; `SECURITY_MODEL.md` (T1–T14). |
| **E2 — Crecimiento multi-plataforma** | Descriptores DiDi/InDrive/Cabify en producción + modo nocturno + umbrales dinámicos. | FUENTE 3 (multi-app); FUENTE 2 §5.1. |
| **E3 — Diferenciación** | Dashboard AHU/tendencias + ahorro energía SOC-aware + modo anti-fatiga. | FUENTE 2 §5.2 y §4.2; FUENTE 3. |
| **E4 — Expansión** | Ecosistema Lite/Pro + Android 16 + mercado LATAM más amplio. | FUENTE 2 §3.2 y §6.2. |

## 7. Decisión del próximo Sprint (justificada por las 3 fuentes + gate)

**Sprint 12 = "Beta controlada: validación núcleo de producto (E1a)"**,
**reemplaza** la propuesta anterior "beta + Play Integrity".

Justificación del gate (detalle en `docs/BETA_READINESS.md` §4 y
`docs/SECURITY_MODEL.md` §12):

- **A. ¿Es el siguiente?** Sí: el RC1 permite validar en campo. Pero se
  **refina**: Play Integrity/Billing/backend pasan a **E1b**, construidos con
  los datos reales de la beta y sin riesgo de monetizar un producto no validado.
- **B. Antes de la beta**: E0 ✅ + checklist `BETA_READINESS.md` §2 + Play
  Console internal/closed track + métricas locales.
- **C. En Sprint 12**: instrumentación de beta (métricas de decisión local,
  encuesta, reporte de bugs), pulido de UX de errores, primeros ajustes de
  estabilidad.
- **D. Después (E1b/E3)**: entitlement, backend, Billing, RTDN, Play Integrity
  enforcement, anti-fatiga, AHU.
- **E. Pruebas obligatorias**: overlay <3 s cronometrado en ruta, OCR en ≥3
  dispositivos (Android 10–15), jornada ≥8 h sin crash, cada plataforma en ≥1
  dispositivo real, revocación de MediaProjection sin crash.

> NOTA: es una **decisión de planificación** registrada en `docs/ROADMAP.md`,
> `docs/BETA_READINESS.md` y `TASK.md`. No se implementa código hasta que la
> tarea se abra explícitamente (regla R16 de `.ai/RULES.md`).

## 8. Guardrails estratégicos para agentes

1. **Nunca** proponer auto-clic, auto-aceptar, gestos ni automatización de
   interacción con otras apps (regla R9 ampliada; riesgo de baneo).
2. La **prioridad de producto es P0/P1**: cualquier feature que no provenga de
   las prioridades documentadas se considera fuera de roadmap y requiere
   aprobación explícita. La seguridad comercial vive en **P1 (E1b)**, no en P0
   ni P3.
3. Los módulos `:domain`, `:core:platform`, `:core:capture` son Kotlin puro y
   testeables; Play Integrity / entitlement / backend / SOC se exponen como
   **contratos de dominio** (`EntitlementRepository`, etc.), jamás lógica Android
   en el núcleo.
4. **Nunca** confiar en el cliente para autorización premium (principio del
   `SECURITY_MODEL.md`): el APK se considera manipulable.
5. Todo dato de pantalla/ocfertas permanece local; el backend solo trata estado
   comercial/de cuenta (LOCAL-FIRST).
6. Toda decisión que modifique la ruta (orden de prioridades) debe registrarse
   aquí y en `.ai/DECISIONS.md`.