# EXECUTIVE SUMMARY — Revisión de Release (Pre-Beta)

**Rol:** Release Manager
**Fecha:** 2026-08-01
**Alcance:** Consolidación de las 9 auditorías del 2026-08-01
**Método:** Solo lectura y clasificación. No se generan nuevas recomendaciones; se
consolida la evidencia y las prioridades declaradas por cada auditoría.

---

## 1. Fuentes

| # | Auditoría | Archivo |
|---|---|---|
| 1 | Arquitectura | `docs/audit/ARCHITECTURE_AUDIT.md` |
| 2 | Android | `docs/audit/ANDROID_AUDIT.md` |
| 3 | Calidad (QA) | `QA_AUDIT.md` |
| 4 | Rendimiento | `PERFORMANCE_AUDIT.md` |
| 5 | Estabilidad | `STABILITY_AUDIT.md` |
| 6 | Seguridad | `SECURITY_AUDIT.md` |
| 7 | Google Play | `GOOGLE_PLAY_AUDIT.md` |
| 8 | UX | `docs/audit/UX_AUDIT.md` |
| 9 | Documentación | `docs/audit/DOCUMENTATION_AUDIT.md` |
| — | Check-list pre-release | `CHECKLIST_PRE_RELEASE.md` (deriva de Seguridad y Google Play) |

---

## 2. Veredicto global consolidado

**El release NO es aprobable para Beta en su estado actual.** Las nueve
auditorías coinciden en que la arquitectura de base, la disciplina de corrutinas,
el dominio puro y el tratamiento de permisos son sólidos, pero existen
**bloqueantes de comportamiento, de Google Play, de seguridad y de seguridad
vial** que deben resolverse antes de distribuir la Beta:

1. **Doble pipeline de captura activo en producción** — `SircAccessibilityService`
   (legacy) y `CaptureAccessibilityService` (moderno) corren en paralelo con
   recorridos duplicados y persistencia doble (ARC-1.1, DUP-10.1, ACC-7.1,
   P-P08, S-S16, GP-4). Severidad **ALTA/CRÍTICA** según auditoría.
2. **Fuga de proyección MediaProjection al morir el FGS** — `MediaProjectionService`
   no implementa `onDestroy` (MPR-6.1). **CRÍTICO**.
3. **`android:exported="false"` en los AccessibilityService sin verificación en
   dispositivo** — puede impedir el vínculo de `system_server` y romper toda la
   captura (S-S14). **CRÍTICO — requiere verificación de hardware inmediata**.
4. **Detección de pantalla acoplada a vocabulario español+Uber** — bloquea el
   objetivo multi-plataforma (DiDi/Bolt/Lyft/Cabify/InDrive) (SCA-11.1).
   **CRÍTICO**.
5. **Declaración de Data Safety "sin datos recolectados" es falsa** — la app sí
   recopila contenido de pantalla, perfil del conductor e historial (GP-1).
   **CRÍTICO** para Google Play (rechazo/despublicación).
6. **Contraste del indicador de decisión insuficiente para conducción** —
   `ProfitIndicator` falla AA (2.0–3.9:1) (UX-2.1). **CRÍTICO** para seguridad vial.
7. **Panel de depuración (con texto OCR) disponible en release** (S-3), **BD Room
   sin cifrar** (S-1) y **`allowBackup="true"` sin exclusión** (S-2). **ALTO**.
8. **Play**: sin política de privacidad (GP-2), sin prominent disclosure in-app
   (GP-3), dos AccessibilityServices redundantes (GP-4), FGS `specialUse` siempre
   encendido (GP-5). **ALTO**.

---

## 3. Métricas consolidadas

**191 hallazgos accionables** clasificados de las 9 auditorías (los puntos
positivos se listan en §7 y no entran en el backlog).

### 3.1 Por severidad consolidada

| Severidad | Cantidad |
|---|---|
| CRÍTICO | 6 |
| ALTO | 38 |
| MEDIO | 96 |
| BAJO | 51 |
| **Total** | **191** |

### 3.2 Por ventana de corrección

| Ventana | Cantidad | Criterio (prioridad en auditorías) |
|---|---|---|
| Debe corregirse antes de Beta | **69** | P0 + P1 (GP-1..GP-5, S-1..S-3 por severidad) |
| Puede esperar a v1.1 | **79** | P2 |
| Puede esperar a v2.0 | **43** | P3 |
| **Total** | **191** | |

### 3.3 Por auditoría

| Auditoría | CRÍTICO | ALTO | MEDIO | BAJO | Total |
|---|---|---|---|---|---|
| Arquitectura (ARC/SOL/MOD/…) | 1 | 11 | 24 | 12 | 48 |
| Android (CMP/NAV/LIF/…) | 1 | 1 | 20 | 14 | 36 |
| QA (H/M/B-QA) | 0 | 3 | 4 | 2 | 9 |
| Rendimiento (P-P) | 0 | 4 | 8 | 4 | 16 |
| Estabilidad (S-S) | 1 | 3 | 11 | 6 | 21 |
| Seguridad (S-) | 0 | 3 | 2 | 0 | 5 |
| Google Play (GP-) | 1 | 4 | 3 | 0 | 8 |
| UX (UX-) | 1 | 6 | 16 | 11 | 34 |
| Documentación (DOC-) | 1 | 3 | 8 | 2 | 14 |
| **Total** | **6** | **38** | **96** | **51** | **191** |

---

## 4. Bloqueantes antes de Beta — CRÍTICOS (6)

| ID | Hallazgo | Auditoría |
|---|---|---|
| SCA-11.1 | Detección de pantalla es vocabulario español+Uber; otras plataformas jamás se parsean | Arquitectura |
| MPR-6.1 | `MediaProjectionService` sin `onDestroy` → fuga de proyección/VirtualDisplay/ImageReader | Android |
| S-S14 | `android:exported="false"` en AccessibilityServices puede impedir el vínculo del sistema (verificar en dispositivo) | Estabilidad |
| GP-1 | Declaración de Data Safety "sin datos recolectados" es falsa | Google Play |
| UX-2.1 | Contraste del indicador de decisión (2.0–3.9:1) falla AA en conducción | UX |
| DOC-2.1 | CHANGELOG sin cabecera v0.5.0; contenido del Sprint 4 anidado bajo v0.6.0 | Documentación |

---

## 5. Bloques de trabajo prioritarios (antes de Beta)

Agrupación de los 69 hallazgos que deben corregirse antes de Beta (detalle
completo en `PRE_BETA_BACKLOG.md` §1).

| Bloque | Hallazgos principales |
|---|---|
| **A. Unificar la ruta de captura** (eliminar pipeline legacy, un solo AccessibilityService) | ARC-1.1, DUP-10.1, PKG-7.1, ACC-7.1, ACC-7.4, LIF-3.1, S-S16, GP-4 |
| **B. Robustez MediaProjection** (liberación, canal, PNG, hash, memoria) | MPR-6.1, MPR-6.2, MPR-6.3, BRT-12.2, BRT-12.3, P-P01, P-P02, S-S10 |
| **C. Seguridad de datos y privacidad** | S-1, S-2, S-3, GP-1, GP-2, GP-3, S-4, S-5 |
| **D. Google Play — acceso mínimo y FGS** | GP-4, GP-5, ACC-7.5, S-S14 (verificación) |
| **E. Integridad del negocio** (isConfigured, umbrales, moneda multi-plataforma) | ARC-1.3, DUP-10.6, SCA-11.2, SCA-11.3, SCA-11.4, SCA-11.5, SCA-11.7, ABS-8.2, DEAD-9.1 |
| **F. Estabilidad del overlay y del estado** | S-S04, S-S10, FLW-4.1, FLW-4.2, CMP-1.1, CMP-1.3, CMP-1.5, VM-9.3, CO-5.1 |
| **G. UX y seguridad vial** | UX-2.1, UX-2.2, UX-2.3, UX-2.4, UX-1.1, UX-3.2, UX-11.1, UX-11.2, UX-12.1, UX-4.1, UX-6.1 |
| **H. Calidad de verificación (tests)** | H-QA-01, H-QA-02, H-QA-03 |
| **I. Documentación veraz** | DOC-2.1, DOC-3.1, DOC-4.1, DOC-5.1, DOC-9.4 |

---

## 6. Deuda diferida

### v1.1 (79 hallazgos — P2)
- God-classes y sobre-acoplamiento de presentación (SOL-2.2..2.5, MOD-3.1..3.3,
  SIZ-5.1, MIX-6.1..6.2, VM-9.1, VM-9.2, FGS-8.4).
- Formatters y localización estructural (PKG-7.2 → P1; DUP-10.4, S-S07, S-S08,
  UX-8.1, UX-9.1, UX-1.3, DOC-3.2).
- WorkManager y trim de historial (WKM-10.1); memoria en pantallas grandes
  (MPR-6.5, S-S06); robustez FGS/overlay (S-S02, S-S05, S-S22, S-S23, S-S24);
  edge cases numéricos y multi-idioma en QA (M-QA-04..07).
- Ver en `PRE_BETA_BACKLOG.md` §2.

### v2.0 (43 hallazgos — P3)
- Código muerto menor, sobre-abstracción, navegación type-safe, compatibilidad
  menor, KDoc de tests, formato CHANGELOG.
- Ver en `PRE_BETA_BACKLOG.md` §3.

---

## 7. Fortalezas verificadas (sin acción requerida)

- **Dominio puro**: `:domain` es JVM pura sin Android; motores 100 % testeables.
- **Dirección de dependencias correcta**: `data → domain`, `core/* → domain`;
  sin ciclos a nivel de módulo (COH-4.1).
- **Tratamiento de permisos ejemplar para la categoría**: accessibility read-only
  (`canPerformGestures="false"`), FGS correctos para Android 14/15, sin `INTERNET`,
  app 100 % offline, sin secretos embebidos (ACC-7.6, FGS-8.1, GP-§5).
- **Corrutinas**: sin `GlobalScope`; scopes cancelados en `onDestroy`;
  `SupervisorJob` (CO-5.3).
- **Memoria acotada**: buffers 32/50/100/500/100/20; bitmaps e `Image.close()`
  en `finally` en la mayoría de rutas.
- **Room disciplinado**: `exportSchema=true`, esquemas versionados, migraciones
  1→2→3 con test de migración.
- **Semáforo de decisión consistente en toda la app** (color+texto, respeta
  daltonismo) (UX-10.1).
- **Cobertura de dominio notable**: 26 archivos de test unitario, 181 casos.
- **Documentación superior a la media**: 26 archivos Markdown, planes de prueba
  por sprint 1:1 con versiones.

---

## 8. Recomendación de Release Manager

1. **No abrir la Beta** hasta resolver los 6 hallazgos CRÍTICOS y los bloques
   A–I (§5).
2. **Ejecutar de inmediato la verificación en dispositivo** de S-S14
   (accesibilidad con `exported="false"`) — es el único hallazgo CRÍTICO cuya
   confirmación depende de hardware y puede invalidar o confirmar toda la vía de
   captura.
3. **Tratar los hallazgos de Play (GP-1..GP-5) como deuda de release, no de
   iteración**: condicionan la publicación de la Beta en el canal cerrado.
4. Usar `PRE_BETA_BACKLOG.md` como el registro único de trazabilidad
   hallazgo → severidad → ventana de corrección.

---

*Fin de EXECUTIVE_SUMMARY.md. Documento de consolidación; sin modificaciones de
código y sin nuevas recomendaciones.*
