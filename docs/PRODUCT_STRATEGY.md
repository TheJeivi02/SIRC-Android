# Estrategia de Producto SIRC

> Documento de dirección estratégica consolidado a partir de las TRES fuentes:
> **(1)** auditoría técnica del repositorio (Sprints 1–11, v1.0.0-rc1),
> **(2)** `docs/Informe Ejecutivo de Estrategia y Diseño…`,
> **(3)** `docs/PRODUCT_COMPETITIVE_ANALYSIS.md` (investigación de mercado
> verificada). Define la ruta de producto: diferenciación, prioridades,
> arquitectura futura, roadmap y el próximo Sprint decidido.

## 1. Posicionamiento estratégico

**SIRC = la app de decisión de rentabilidad legalista, instantánea y 100 % local.**

Pilares innegociables (derivan de FUENTE 1 + FUENTE 2 + FUENTE 3):

1. **Solo lectura, cero automatización.** Prohibido auto-clic, gestos,
   `performAction`, aceptar/rechazar por el conductor, contra-ofertas. La
   automatización es el mayor riesgo de baneo y viola la política de Play.
2. **Decisión <3 s con mínima carga cognitiva.** Una mirada, color semáforo,
   información derivada, sin duplicar lo que muestra la plataforma.
3. **100 % local.** Sin telemetría, sin backend, sin subir pantallas. Es
   diferenciador frente a la mayoría del mercado.
4. **Multi-plataforma real** (Uber, DiDi, InDrive, Cabify) como ruta de
   dominancia de la cohorte multi-app.
5. **Rendimiento y batería como seguridad vial** (jornadas de 12 h).

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

## 4. Prioridades de producto (P0–P3)

Prioridades redefinidas al cruzar las tres fuentes. La P0 es de supervivencia
(no negociable), P1 es la ruta inmediata tras RC1, P2 y P3 se desprenden del
informe estratégico.

### P0 — Robustez y cumplimiento (antes de cualquier feature nueva)

- Todo lo que garantice beta estable en dispositivos reales (Android 10–15).
- Política de Play y anti-baneo intactos (solo lectura, sin clics).
- Sin regresiones de rendimiento/batería.

### P1 — Ruta inmediata (valor directo al conductor)

1. **Lanzar beta cerrada** con cohorte real (validar overlay <3 s en campo).
2. **Play Integrity (Strong)** + alineación con declaración de accesibilidad
   (FUENTE 2, §3.1).
3. **Cierre de descriptores multi-plataforma** (DiDi, InDrive, Cabify) con
   datasets y tests, para escalar la cohorte multi-app.
4. **Umbrales dinámicos** con resalte visual (FUENTE 2, §5.1).

### P2 — Diferenciación de ciclo medio

5. **Modo nocturno / contraste adaptativo** (adoptar de Ruta Rentable).
6. **Dashboard de AHU** y tendencias por día/semana/mes (FUENTE 2, §5.2;
   ampliación del dashboard actual).
7. **Ahorro de energía inteligente** (SOC/temperatura; refresh 1 Hz detenido).

### P3 — Profesionalización (largo plazo)

8. **Modo anti-fatiga** (alertas suaves por tiempo conectado).
9. **Ecosistema Lite/Pro** con compartición segura de datos de rentabilidad
   (FUENTE 2, §3.2, Key Sharing API — solo cuando exista el segundo producto).
10. **Android 16 Ready** (Safer Intents, Ordered Broadcasts priority).

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

## 6. Roadmap por etapas (cruzando las 3 fuentes)

| Etapa | Alcance | Fuentes que la justifican |
|---|---|---|
| **E0 — Cierre técnico (completado, Sprint 11)** | Remediación, auditoría, RC1 verde. | FUENTE 1 (estado real). |
| **E1 — Lanzamiento controlado** | Beta cerrada real + Play Integrity + Play Policy hardening. Comunica el diferenciador "solo lectura". | FUENTE 2 Hito 1; FUENTE 3 (riesgo de automatizadores como narrativa). |
| **E2 — Crecimiento multi-plataforma** | Descriptores DiDi/InDrive/Cabify + modo nocturno + umbrales dinámicos. | FUENTE 3 (multi-app dominante del mercado); FUENTE 2 §5.1. |
| **E3 — Diferenciación** | Dashboard AHU/tendencias + ahorro energía SOC-aware + modo anti-fatiga. | FUENTE 2 §5.2 y §4.2; FUENTE 3 (Ruta Rentable/Motorista One como referencia). |
| **E4 — Expansión** | Ecosistema Lite/Pro + Android 16 + mercado LATAM más amplio. | FUENTE 2 §3.2 y §6.2. |

## 7. Decisión del próximo Sprint (justificada por las 3 fuentes)

**Sprint 12 = "Lanzamiento controlado (beta cerrada + Play Integrity)"**,
primer entregable de la etapa E1.

Justificación cruzada:

- **FUENTE 1 (técnica)**: RC1 está endurecido y en verde (Sprint 11). El
  siguiente paso lógico es **poner el producto frente a conductores reales y
  validar el overlay <3 s en campo**; sin Play Integrity el riesgo de
  coexistencia con herramientas fraudulentas permanece.
- **FUENTE 2 (estratégica)**: el Hito 1 prescribe textualmente *"Validación de
  Integridad y Core: Play Integrity (Strong) y arquitectura de accesibilidad
  Read-Only"*, y el §3 hace de la integridad la prioridad uno.
- **FUENTE 3 (mercado)**: la narrativa ganadora frente a automatizadores es
  "legal, solo lectura, <3 s". Lanzar una beta cerrada con integridad declarada
  convierte esa narrativa en proof point, no en promesa.

> NOTA: este Sprint 12 es una **decisión de planificación** registrada en
> `docs/ROADMAP.md` y `TASK.md`. No se implementa código hasta que la tarea se
> abra explícitamente (regla R16 de `.ai/RULES.md`).

## 8. Guardrails estratégicos para agentes

1. **Nunca** proponer auto-clic, auto-aceptar, gestos ni automatización de
   interacción con otras apps (regla R9 ampliada; riesgo de baneo).
2. La **prioridad de producto es P0/P1**: cualquier feature que no provenga de
   las prioridades documentadas se considera fuera de roadmap y requiere
   aprobación explícita.
3. Los módulos `:domain`, `:core:platform`, `:core:capture` son Kotlin puro y
   testeables; Play Integrity / SOC / datos de sensor se exponen como contratos
   de dominio.
4. Toda decisión que modifique la ruta (orden de prioridades) debe registrarse
   aquí y en `.ai/DECISIONS.md`.