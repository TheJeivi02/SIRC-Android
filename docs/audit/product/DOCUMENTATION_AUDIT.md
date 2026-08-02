# Auditoría de Documentación — SIRC

> Auditoría completa de documentación. Rol: Technical Writer.
> Solo evidencia y recomendaciones — sin modificaciones de código. Fecha: 2026-08-01.
> Método: lectura de los 26 archivos Markdown del repo + verificación de KDoc y
> comentarios en 197 archivos `.kt` (168 `src/main`, 29 `src/test`/`androidTest`);
> cruce con `git log`, `settings.gradle.kts` y `.github/workflows/ci.yml`.

**Convención de severidad**: `CRITICA` = documentación que induce a error operativo o
de release; `ALTA` = documentación desactualizada que contradice el estado real;
`MEDIA` = lagunas o deuda mantenible; `BAJA` = mejora.
**Convención de prioridad**: `P0` = inmediato; `P1` = antes del siguiente release;
`P2` = siguiente iteración; `P3` = backlog.

---

## Resumen ejecutivo

| Dimensión | Veredicto |
|---|---|
| README | ⚠️ Sólido y bien escrito; desactualizado en módulos (8 vs 11) e índice de docs |
| CHANGELOG | ❌ Falta cabecera `v0.5.0`; contenido del Sprint 4 anidado bajo v0.6.0 |
| ROADMAP | ⚠️ Coherente con sprints; desactualizado en conteo de módulos |
| ARCHITECTURE | ✅ Completo (494 líneas); ⚠️ afirma que flujo legacy fue "eliminado" y no fue |
| CONTEXT (.ai) | ✅ El más actualizado (11 módulos, fechas, referencias cruzadas) |
| DECISIONS (.ai) | ✅ Extenso (754 líneas); sin fecha de última revisión |
| PROJECT.md | ❌ "Fecha de referencia: v0.1.0" — 9 versiones desactualizado |
| Comentarios / KDoc | ⚠️ 70.6% de cobertura; 0 en settings/onboarding/data/use-cases |
| Tests de doc | ❌ CI no valida formato de CHANGELOG ni enlaces del README |

**Veredicto global**: la documentación del proyecto es notablemente superior a la media
(26 archivos, planes de prueba por sprint 1:1 con versiones, decisión y arquitectura bien
documentadas, 0 TODOs en el código). Los defectos materiales son **puntuales pero
peligrosos**: (1) el CHANGELOG perdió la cabecera v0.5.0 y fusionó dos sprints bajo v0.6.0,
(2) README/AGENTS/ROADMAP/PROJECT declaran 8 módulos cuando el proyecto tiene 11, y
(3) PROJECT.md referencia v0.1.0 estando en v1.0.0-rc1. En KDoc, los módulos de UI
(`:feature:settings`, `:feature:onboarding`) y `:data` tienen cobertura nula o mínima.

---

## 1. Inventario de documentación

### 1.1 Raíz

| Archivo | Líneas | Estado |
|---|---|---|
| `README.md` | 91 | ⚠️ Actualizar módulos e índice |

### 1.2 `.ai/` — contexto para agentes IA

| Archivo | Líneas | Estado |
|---|---|---|
| `.ai/AGENTS.md` | 59 | ⚠️ "8 módulos" (línea 10) |
| `.ai/CONTEXT.md` | 335 | ✅ Más actualizado (11 módulos, línea 240) |
| `.ai/DECISIONS.md` | 754 | ✅ Extenso; sin fecha de revisión |
| `.ai/RULES.md` | 59 | ✅ Estable |

### 1.3 `docs/`

| Archivo | Líneas | Estado |
|---|---|---|
| `ARCHITECTURE.md` | 494 | ✅ Completo; ⚠️ afirmación legacy |
| `CHANGELOG.md` | 571 | ❌ Cabecera v0.5.0 faltante |
| `CODING_STANDARDS.md` | 119 | ✅ |
| `GOOGLE_PLAY_COMPLIANCE.md` | 100 | ✅ |
| `KNOWN_ISSUES.md` | 79 | ✅ |
| `PERFORMANCE_REPORT.md` | 80 | ✅ |
| `PROJECT.md` | 89 | ❌ Fecha de referencia v0.1.0 |
| `RELEASE_NOTES_RC1.md` | 90 | ✅ |
| `ROADMAP.md` | 311 | ⚠️ "8 módulos" (línea 10) |
| `TEST_REPORT.md` | 88 | ✅ |

### 1.4 `docs/audit/` — auditorías previas

| Archivo | Líneas | Fecha |
|---|---|---|
| `ARCHITECTURE_AUDIT.md` | 530 | 2026-08-01 |
| `ANDROID_AUDIT.md` | 458 | 2026-08-01 |

### 1.5 `docs/testing/` — planes de prueba por sprint

| Archivo | Líneas | Versión |
|---|---|---|
| `BETA_TEST_PLAN.md` | 123 | v1.0.0-beta |
| `SPRINT_04_MANUAL_TEST.md` | 93 | v0.5.0 |
| `SPRINT_05_MANUAL_TEST.md` | 75 | v0.6.0 |
| `SPRINT_06_MANUAL_TEST.md` | 95 | v0.7.0 |
| `SPRINT_07_MANUAL_TEST.md` | 101 | v0.8.0 |
| `SPRINT_08_MANUAL_TEST.md` | 99 | v0.9.0 |
| `SPRINT_09_MANUAL_TEST.md` | 128 | v1.0.0-beta |
| `SPRINT_10_MANUAL_TEST.md` | 106 | v1.0.0-rc1 |

### 1.6 Datasets

| Archivo | Líneas | Estado |
|---|---|---|
| `core/capture/src/test/resources/test-images/README.md` | 46 | ✅ Referenciado en 7 documentos |

**Total: 26 archivos Markdown.**

---

## 2. CHANGELOG

### DOC-2.1 — Cabecera `## [v0.5.0]` faltante; contenido del Sprint 4 anidado bajo v0.6.0 — CRITICA
- **Resumen**: El CHANGELOG tiene 10 cabeceras de versión pero **falta v0.5.0**. El
  contenido del Sprint 4 (módulo `:core:capture`, Feature Flags, panel de depuración,
  DI en `CaptureModule`) está anidado bajo la sección v0.6.0 con un `### Añadido`
  huérfano (líneas 368–419), después del `### Corregido` de v0.6.0.
- **Evidencia**: `docs/CHANGELOG.md:331` (`## [v0.6.0]`), `:365` ("ya quedaron resueltas
  en v0.5.0"), `:368-419` (contenido del Sprint 4 bajo v0.6.0), `:421` (`## [v0.4.0]`).
  Git lo confirma: `git log` tiene `30aeaf9 feat: plataforma de captura y panel de
  depuración (v0.5.0)`.
- **Riesgo**: Alto. El changelog declara v0.6.0 con contenido de dos sprints, rompe el
  mapeo versiones↔sprints↔commits que el resto de la documentación mantiene 1:1, y una
  futura release (o un agente IA) puede atribuir trabajo al sprint equivocado.
- **Severidad**: CRITICA
- **Recomendación**: Restaurar `## [v0.5.0] — 2026-07-31` antes de la sección v0.6.0 y
  mover el bloque 368–419 bajo esa cabecera; corregir el `### Añadido` huérfano.
- **Prioridad**: P1

### DOC-2.2 — El CHANGELOG no sigue un formato semver/Keep a Changelog estricto — BAJA
- **Resumen**: Los encabezados usan `## [vX.Y.Z]` con guion `- 2026-07-31` (en vez del
  `–` de Keep a Changelog) y no hay vínculo de "diff" entre versiones ni secciones
  `[Unreleased]`.
- **Evidencia**: `docs/CHANGELOG.md:6,56,114,...`.
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Adoptar el formato Keep a Changelog completo (Unreleased, enlaces
  de diff) o documentar la convención propia en `CODING_STANDARDS.md`.
- **Prioridad**: P3

---

## 3. Módulos declarados vs reales

### DOC-3.1 — README/AGENTS/ROADMAP declaran 8 módulos; el proyecto tiene 11 — ALTA
- **Resumen**: `settings.gradle.kts` registra **11 módulos** (`:app`, `:domain`,
  `:data`, `:core:ui`, `:core:platform`, `:core:capture`, `:core:capture:android`,
  `:feature:overlay`, `:feature:settings`, `:feature:history`, `:feature:onboarding`).
  Tres documentos dicen "8" y omiten `:core:capture`, `:core:capture:android` y
  `:feature:onboarding`.
- **Evidencia**: `README.md:42-51` (lista 8), `.ai/AGENTS.md:10` ("8 módulos"),
  `docs/ROADMAP.md:10` ("8 módulos"). Correctos: `.ai/CONTEXT.md:240` ("11 módulos"),
  `docs/CHANGELOG.md:409` ("10 módulos" → 11), `docs/audit/ARCHITECTURE_AUDIT.md:6,170`.
- **Riesgo**: Alto. Un nuevo integrante (o un agente IA) diseñará dependencias y
  comandos con una topología incorrecta.
- **Severidad**: ALTA
- **Recomendación**: Actualizar la lista de módulos en README, AGENTS.md y ROADMAP;
  el README además describe `:core:capture` como futuro "sin OCR" cuando ya existe la
  implementación Android (`:core:capture:android`).
- **Prioridad**: P1

### DOC-3.2 — README no indexa 12 documentos existentes — MEDIA
- **Resumen**: La sección "Documentación" del README (líneas 81–91) lista 9
  documentos y omite: `KNOWN_ISSUES.md`, `PERFORMANCE_REPORT.md`, `RELEASE_NOTES_RC1.md`,
  `TEST_REPORT.md`, los 2 de `docs/audit/`, los 8 de `docs/testing/` y el
  `test-images/README.md`.
- **Evidencia**: `README.md:81-91`.
- **Riesgo**: Medio. Documentos nuevos y valiosos quedan invisibles; un recién llegado
  no sabe que existen `ARCHITECTURE_AUDIT.md` ni los manuales de prueba.
- **Severidad**: MEDIA
- **Recomendación**: Estructurar el índice por categorías (proyecto / arquitectura /
  calidad / testing / audit / agentes IA) con una línea por documento.
- **Prioridad**: P2

---

## 4. PROJECT.md

### DOC-4.1 — "Fecha de referencia: v0.1.0" estando en v1.0.0-rc1 — ALTA
- **Resumen**: `PROJECT.md` declara que deriva del estado del repositorio "en v0.1.0",
  pero el proyecto está en v1.0.0-rc1 (CHANGELOG) y su descripción del MVP ya no
  incluye la plataforma de captura ni el onboarding.
- **Evidencia**: `docs/PROJECT.md:4` ("Fecha de referencia: v0.1.0").
- **Riesgo**: Alto. Es el documento de entrada del proyecto; su versión de referencia
  es 9 releases anterior y contradice a `CONTEXT.md`, `ARCHITECTURE.md` y el propio
  CHANGELOG.
- **Severidad**: ALTA
- **Recomendación**: Actualizar la fecha de referencia a v1.0.0-rc1, el estado actual
  (sección líneas 40–89) y la lista de módulos.
- **Prioridad**: P1

---

## 5. ARCHITECTURE.md

### DOC-5.1 — Afirma que el flujo legacy fue "eliminado en RC1" cuando sigue activo — ALTA
- **Resumen**: `ARCHITECTURE.md:293-294` declara que el flujo legacy
  (`SircAccessibilityService → AccessibilityWindowObserver`) fue "eliminado en RC1".
  La auditoría de arquitectura (ARC-1.1) y el código muestran que ambos pipelines
  siguen activos.
- **Evidencia**: `docs/ARCHITECTURE.md:293-294`; `feature/overlay/src/main/AndroidManifest.xml:11-35`
  declara los dos AccessibilityServices.
- **Riesgo**: Alto. Documentación que describe un sistema que no es el real; decisiones
  de evolución futura se tomarían sobre una premisa falsa.
- **Severidad**: ALTA
- **Recomendación**: Corregir la sección para reflejar el estado real (2 pipelines) o
  referenciar `docs/audit/ARCHITECTURE_AUDIT.md` como fuente de verdad del estado.
- **Prioridad**: P1

### DOC-5.2 — No referencia `docs/audit/ARCHITECTURE_AUDIT.md` — MEDIA
- **Resumen**: ARCHITECTURE.md describe la arquitectura ideal del sprint 1; las
  auditorías del 2026-08-01 contienen la verdad verificada del estado actual, pero
  ARCHITECTURE.md no las enlaza.
- **Evidencia**: `docs/ARCHITECTURE.md` (índice de referencias) no menciona `docs/audit/*`.
- **Riesgo**: Medio. Divergencia documentada "ideal vs real" sin puente explícito.
- **Severidad**: MEDIA
- **Recomendación**: Agregar una sección "Estado verificado (auditorías)" con enlace a
  los dos audits.
- **Prioridad**: P2

---

## 6. CONTEXT.md (agentes IA)

### DOC-6.1 — CONTEXT.md es el documento más consistente del repo — ✅
- **Resumen**: Único documento de arranque con 11 módulos, fechas y referencias
  cruzadas correctas; coherente con CHANGELOG, ROADMAP y el dataset de imágenes.
- **Evidencia**: `.ai/CONTEXT.md:240` (11 módulos); referencias al dataset en `:166,216,248`.
- **Riesgo**: Ninguno.
- **Severidad**: — (conservar)
- **Recomendación**: Convertir a CONTEXT.md en la fuente de verdad del conteo de
  módulos y alinear README/AGENTS/ROADMAP a él.
- **Prioridad**: —

---

## 7. DECISIONS.md

### DOC-7.1 — Sin fecha de última revisión ni tabla de decisiones recientes — MEDIA
- **Resumen**: DECISIONS.md (754 líneas) documenta decisiones con contexto, pero no
  tiene una fecha de revisión ni un índice de decisiones por fecha; no es evidente si
  cubre las decisiones de RC1 (pipeline moderno, captura, onboarding).
- **Evidencia**: `.ai/DECISIONS.md` (estructura sin metadatos de fecha global).
- **Riesgo**: Medio. Sin fecha, un lector no sabe si una decisión documentada está
  vigente o fue revertida.
- **Severidad**: MEDIA
- **Recomendación**: Añadir "Última revisión: 2026-08-01" y una entrada ADR para el
  pipeline moderno y la plataforma de captura.
- **Prioridad**: P2

---

## 8. ROADMAP

### DOC-8.1 — Desactualizado en conteo de módulos y sin fecha de corte — MEDIA
- **Resumen**: ROADMAP dice "8 módulos" (línea 10); el proyecto tiene 11. No tiene
  fecha de generación, aunque los sprints (1–10 + beta + rc) son coherentes con
  CHANGELOG y los manuales de prueba.
- **Evidencia**: `docs/ROADMAP.md:10`; sprints mapean 1:1 con `docs/testing/SPRINT_0X_*`.
- **Riesgo**: Medio (consistencia).
- **Severidad**: MEDIA
- **Recomendación**: Corregir el conteo; añadir estado del sprint actual (10 completado,
  RC1 listo).
- **Prioridad**: P2

---

## 9. Cobertura KDoc y comentarios

### 9.1 Métricas globales (197 archivos `.kt`)

| Métrica | Valor |
|---|---|
| Total archivos `.kt` (excl. `build/`) | 197 |
| — `src/main` | 168 |
| — `src/test` + `androidTest` | 29 |
| Archivos con KDoc (`/**`) | 139 (70.6%) |
| — en `src/main` | 137 |
| — en tests | 2 |
| Archivos **sin ningún comentario** | 51 (28 main, 23 test) |
| Comentarios `//` | 24 en 10 archivos |
| TODO / FIXME / HACK / XXX / BUG | **0** |

### DOC-9.1 — `:feature:settings` y `:feature:onboarding` sin KDoc (0/2 y 0/3) — MEDIA
- **Resumen**: Los 5 archivos de UI de Ajustes y Onboarding no tienen ningún KDoc ni
  comentario.
- **Evidencia**: `feature/settings/src/main/kotlin/com/sirc/feature/settings/*.kt` (2),
  `feature/onboarding/src/main/kotlin/com/sirc/feature/onboarding/*.kt` (3).
- **Riesgo**: Medio. Son los módulos con más interacción de datos (guardado, validación)
  y sin contrato documentado.
- **Severidad**: MEDIA
- **Recomendación**: Documentar `SettingsViewModel`, `OnboardingViewModel` (estado,
  transiciones, validación) y sus `*Screen`.
- **Prioridad**: P2

### DOC-9.2 — `:data` con 10 de 14 archivos sin comentarios — MEDIA
- **Resumen**: `SircDatabase`, los 3 DAOs, `DatabaseModule`, `RepositoryModule`, las 2
  entidades y 2 repositorios no tienen KDoc ni comentarios.
- **Evidencia**: `data/src/main/kotlin/com/sirc/data/**` (4 de 14 con KDoc).
- **Riesgo**: Medio. Es la capa que versiona esquemas de Room y migraciones; sin
  documentación, el porqué de una migración se pierde.
- **Severidad**: MEDIA
- **Recomendación**: Documentar entidades (tabla, claves, índices) y la estrategia de
  migración en `SircDatabase`.
- **Prioridad**: P2

### DOC-9.3 — 6 archivos de `:domain` sin comentarios: justo los use-cases marcados como muertos — MEDIA
- **Resumen**: `OverlayConfigRepository.kt`, `GetDriverConfigUseCase.kt`,
  `GetOverlayConfigUseCase.kt`, `OfferHistoryUseCases.kt`, `SaveDriverConfigUseCase.kt`
  y `SaveOverlayConfigUseCase.kt` carecen de KDoc. Coinciden exactamente con los
  use-cases sin call sites identificados en ARC-1.2.
- **Evidencia**: `domain/src/main/kotlin/com/sirc/domain/usecase/**` y
  `domain/src/main/kotlin/com/sirc/domain/repository/**`.
- **Riesgo**: Medio. Código muerto sin documentación confirma su estado de
  abandono y dificulta decidir eliminarlo.
- **Severidad**: MEDIA
- **Recomendación**: Documentar o eliminar (ver ARC-1.2); en todo caso, marcar el
  estado "obsoleto/sin call sites" en KDoc.
- **Prioridad**: P2

### DOC-9.4 — `ProfitEngine.kt` no documenta la fórmula en KDoc — MEDIA
- **Resumen**: El KDoc de clase dice "función pura" pero no documenta la fórmula de
  rentabilidad (`profit = total − costPerTrip − distance×costPerKm −
  duration×costPerMinute`), solo derivable leyendo el cuerpo (líneas 25–36).
- **Evidencia**: `domain/src/main/kotlin/com/sirc/domain/engine/ProfitEngine.kt:12`.
- **Riesgo**: Medio. Es el corazón del negocio; un cambio de fórmula sin documentar
  rompería el contrato silenciosamente.
- **Severidad**: MEDIA
- **Recomendación**: Documentar fórmula, unidades, y el mapeo a `ProfitState`
  (umbrales) en el KDoc de clase.
- **Prioridad**: P1

### DOC-9.5 — `:core:platform` y `:core:capture*` con 100% de cobertura KDoc — ✅
- **Resumen**: Los extractores multi-plataforma y la plataforma de captura tienen KDoc
  en todos sus archivos (10/10, 31/31, 6/6).
- **Evidencia**: `core/platform/**`, `core/capture/**`, `core/capture/android/**`.
- **Riesgo**: Ninguno. Es el estándar a imitar por los demás módulos.
- **Severidad**: — (conservar)
- **Recomendación**: Mantener; extender la práctica a `:data`, `:feature:settings` y
  `:feature:onboarding`.
- **Prioridad**: —

### DOC-9.6 — Cobertura de tests sin KDoc (2 de 29 archivos) — BAJA
- **Resumen**: Los tests documentan poco su intención (solo 2 de 29 con KDoc), aunque
  los nombres de test del estilo `given_when_then` mitigan la necesidad.
- **Evidencia**: `src/test` y `src/androidTest` (29 archivos, 2 con `/**`).
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Documentar solo los fixtures complejos y las invariantes que
  verifican los tests de motores.
- **Prioridad**: P3

---

## 10. Consistencia cruzada verificada (positivos)

| Cruz | Resultado |
|---|---|
| Git ↔ CHANGELOG ↔ ROADMAP (10 sprints + beta + rc) | ✅ Coherente, salvo DOC-2.1 |
| Manuales de prueba ↔ versiones | ✅ 1:1 (`SPRINT_04`→v0.5.0 … `SPRINT_10`→v1.0.0-rc1) |
| Dataset de imágenes ↔ 7 documentos | ✅ Referencias consistentes |
| README ↔ CI | ✅ Comandos idénticos (`testDebugUnitTest`, `ktlintCheck`, `lintDebug`, `assembleDebug`) |
| `CODING_STANDARDS.md` ↔ código | ✅ Sin violaciones documentadas visibles |
| `GOOGLE_PLAY_COMPLIANCE.md` ↔ manifest | ✅ Solo-lectura, `canPerformGestures="false"` |

---

## Anexo A — Recomendaciones priorizadas

| ID | Hallazgo | Sev | Prioridad |
|---|---|---|---|
| DOC-2.1 | CHANGELOG: cabecera v0.5.0 faltante | CRITICA | P1 |
| DOC-3.1 | Módulos 8 vs 11 (README/AGENTS/ROADMAP) | ALTA | P1 |
| DOC-4.1 | PROJECT.md "referencia v0.1.0" | ALTA | P1 |
| DOC-5.1 | ARCHITECTURE: "legacy eliminado" falso | ALTA | P1 |
| DOC-9.4 | ProfitEngine sin fórmula en KDoc | MEDIA | P1 |
| DOC-3.2 | README sin índice de 12 docs | MEDIA | P2 |
| DOC-5.2 | ARCHITECTURE sin enlace a audits | MEDIA | P2 |
| DOC-7.1 | DECISIONS sin fecha de revisión | MEDIA | P2 |
| DOC-8.1 | ROADMAP conteo módulos | MEDIA | P2 |
| DOC-9.1 | KDoc settings/onboarding | MEDIA | P2 |
| DOC-9.2 | KDoc :data (10/14) | MEDIA | P2 |
| DOC-9.3 | KDoc use-cases muertos | MEDIA | P2 |
| DOC-2.2 | Formato CHANGELOG | BAJA | P3 |
| DOC-9.6 | KDoc en tests | BAJA | P3 |

## Anexo B — Acciones sugeridas de una pasada (P1)

1. **CHANGELOG**: insertar `## [v0.5.0] — 2026-07-31` y mover las líneas 368–419 bajo ella.
2. **README**: actualizar el bloque de módulos (líneas 42–51) a 11 y el índice de
   documentación (líneas 81–91).
3. **AGENTS.md / ROADMAP**: "8 módulos" → "11 módulos".
4. **PROJECT.md**: "Fecha de referencia: v0.1.0" → "v1.0.0-rc1" y actualizar estado/módulos.
5. **ARCHITECTURE.md**: corregir la afirmación sobre el flujo legacy.
6. **ProfitEngine.kt**: añadir la fórmula al KDoc (único cambio de código sugerido, y
   es solo comentario).
