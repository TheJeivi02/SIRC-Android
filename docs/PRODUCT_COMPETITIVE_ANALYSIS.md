# Análisis Competitivo del Producto

> Consolidación de las tres fuentes de información: **(1)** auditoría técnica del
> repositorio (Sprints 1–11, v1.0.0-rc1), **(2)** `docs/Informe Ejecutivo de
> Estrategia y Diseño…` y **(3)** investigación de mercado con verificación web
> (16-ago-2026; **actualizado por el LOOP Modelo Comercial Trial→Suscripción**,
> 16-ago-2026). Este documento es la FUENTE ÚNICA de datos competitivos; no
> duplica contenidos del informe estratégico.

> **Nota de precios**: toda cifra competitiva se registra con `VERIFIED` +
> fecha + fuente, siguiendo la regla del LOOP (no inventar precios). Sin precio
> público → `PRICE NOT PUBLICLY DISCLOSED` (no se completa con estimaciones).
> Prioridad de fuentes: (1) sitio oficial, (2) Google Play/App Store,
> (3) documentación oficial, (4) secundarias solo si no hay oficial. **No se usa
> Reddit como fuente primaria.** Los precios competitivos NO son precios de SIRC.

## 1. Universo analizado y nivel de verificación

| Competidor | País/Región | Foco | Nivel de verificación |
|---|---|---|---|
| **Ruta Rentable** | LATAM (15 países) | Calculadora automática de rentabilidad pre-aceptación | ✅ Web verificada (Google Play); **trial ~3 días**; precio `NOT PUBLICLY DISCLOSED` |
| **Motorista One (ON)** | Brasil | Toolbox multiapp + control financiero | ✅ Web verificada (R$29,90/mes; R$169,90/año) |
| **GigU** | EE. UU./global | Rentabilidad configurada por meta ($/hora, $/km) | ✅ Web verificada (USD 6,95/mes; prueba inicial) |
| **DecideRider** | Global/LATAM | Billetera virtual + IA (within app) | ✅ Ficha en Play; **trial ~14 días**; **CLP $3.490/mes** (VERIFIED, 16-ago-2026, oferta consultada) |
| **Viaje Rentable** | LATAM | Calculadora de viaje (aparece en búsquedas) | ⚠️ Mención web, sin ficha completa |
| **Radar de Viajes** | LATAM | Semáforo de rentabilidad en overlay | ⚠️ Mención web, sin ficha completa |
| **Autoindrive / Maxymo / Mystro** | Global | **Automatización de aceptación** (auto-accept, contra-ofertas) | ⚠️ **NO VERIFICADO** (solo referencias del usuario) |
| **Rinde** | LATAM | Reportes fiscales/contables (no asistencia en vivo) | ✅ Citado en informe estratégico (FUENTE 2) |

> **Nota de rigor**: `Autoindrive`, `Maxymo` y `Mystro` se citan en la FUENTE 3
> como referentes de **automatización de clics** (auto-aceptar/rechazar), pero su
> comportamiento NO ha sido verificado con fuente pública en esta iteración. Se
> marcan **NO VERIFICADO** y se usan solo como referencia conceptual del riesgo
> de baneo. Siempre que un dato no tenga verificación, se indica.

## 2. Matriz comparativa por capacidades

Leyenda: ✅ lo tiene · 🟡 parcial / limitado · ❌ no lo tiene · ❓ no verificado.

| Capacidad | SIRC (rc1) | Ruta Rentable | Motorista One | GigU | DecideRider | Autoindrive* |
|---|---|---|---|---|---|---|
| Overlay flotante de decisión | ✅ | 🟡 | ✅ | ✅ | ✅ | ✅ |
| Decisión <1 s estilo semáforo (UX; <3 s E2E) | ✅ | ✅ | ❌ (multiapp, sin filtro EPOH) | ✅ | 🟡 | ❓ |
| Solo lectura (sin clics) | ✅ | ✅ | ✅ | ✅ | ❌ (bots AFK/automatizador) | ❌ (auto-accept) |
| Cálculo ganancia neta real | ✅ | 🟡 manual | ✅ | ✅ | ✅ | ❓ |
| Métricas $/hora y $/km | ✅ | ✅ | ✅ | ✅ | 🟡 | ❓ |
| Objetivos/meta por hora o km | ✅ umbrales | ❌ | 🟡 metas día | ✅ por meta | ✅ | ❓ |
| Multi-app (Uber/DiDi/Cabify/InDrive) | ✅ arquitectura | ✅ Uber/DiDi/Cabify | ✅ Uber/99/InDrive | ❌ | ❓ | ❓ |
| Historial persistente + estadísticas | ✅ Room + dashboard | ❓ | ✅ | ✅ | ✅ | ❓ |
| Ahorro de batería / modo nocturno | 🟡 | ✅ modo nocturno | ❓ | ❓ | ❓ | ❓ |
| Exportar diagnóstico / validación | ✅ | ❌ | ❓ | ❓ | ❓ | ❌ |
| 100 % local (sin telemetría) | ✅ | ⚠️ no verificado | ⚠️ no verificado | ⚠️ no verificado | ⚠️ no verificado | ❌ |

\* Autoindrive/Maxymo/Mystro: referencias del usuario, sin verificación pública.

## 3. Lecturas clave del mercado

1. **El estándar de la categoría es "overlay + semáforo + rentabilidad"**. SIRC ya
   lo cumple en el nivel más alto de rigor (decisión derivada, no duplicada,
   <1 s UX / <3 s E2E, solo lectura).
2. **La automatización de clics es una trampa competitiva.** Varios actores
   (DecideRider con bots sin verificación concluyente, Autoindrive/Maxymo/Mystro
   referenciados) compiten con auto-accept o IA dentro de la app. SIRC **no debe
   imitarlos**: la arquitectura de solo lectura y la política anti-baneo
   (FUENTE 2, §3) son un diferenciador defendible y un requisito de Google Play.
3. **Brecha de mercado confirmada**: ninguna app verificada combina a la vez
   (a) decisión <1 s, (b) solo lectura estricto, (c) 100 % local y (d)
   multi-plataforma real. SIRC es la única con los cuatro pilares simultáneos.
4. **Ruta Rentable y Motorista One** muestran features aspiracionales que SIRC
   puede **adoptar/mejorar** (modo nocturno, gestión de vehículo, tendencias por
   día/semana/mes, inteligencia sobre conteo).
5. **El informe estratégico** (FUENTE 2) recomienda además: Play Integrity,
   umbrales dinámicos, modo anti-fatiga, ahorro de energía SOC-aware, dashboard
   de AHU y adaptación multi-plataforma automática. Nada de esto entra en
   conflicto con la arquitectura; todo es aditivo.

## 4. Riesgos y oportunidades priorizados

| Ítem | Tipo | Peso estratégico |
|---|---|---|
| Copiar automatización (auto-clic) | Riesgo | 🔴 Bloqueante (baneo + violación Play). Prohibido. |
| No lograr multi-app real | Riesgo | 🟠 Medio (pierde cohorte multi-app dominante del mercado). |
| No tener modo nocturno / ahorro batería | Oportunidad | 🟢 Alta (dolor real de jornada 12 h). |
| Dashboard/estadísticas profundas (AHU, tendencias) | Oportunidad | 🟢 Media (profesionalización del conductor). |
| Play Integrity + cumplimiento declarado | Oportunidad | 🟢 Alta (confianza + anti-baneo, diferenciador narrativo). |

> El detalle de QUÉ adoptar, mejorar o evitar está en
> `docs/PRODUCT_STRATEGY.md` (matriz de diferenciación).

## 5. Precios y pruebas de competidores (VERIFIED, 16-ago-2026)

Referencias competitivas verificadas. **No son precios de SIRC** (los precios
SIRC se deciden con la matriz de `SUBSCRIPTION_MODEL.md` §5bis). Formato:
`VERIFIED` (fecha) · `SOURCE`; sin precio público → `PRICE NOT PUBLICLY
DISCLOSED`; si existe información más actualizada en fuente oficial, priorizarla
sobre esta.

| Producto | Plan / Periodicidad | Precio (observado) | Trial | Fuente |
|---|---|---|---|---|
| Motorista One | Mensual | R$29,90 (~USD 5,5) | — | Web oficial / Play (BR) |
| Motorista One | Anual | R$169,90 (~USD 31) | — | Web oficial / Play (BR) |
| GigU | Monthly | USD 6,95 | Prueba inicial | Oferta internacional consultada |
| GigU | Anual | USD 49,95 (~USD 4,2/mes) | — | Oferta internacional consultada |
| DecideRider | Mensual | CLP $3.490 (~USD 3,6) | **~14 días** | Oferta consultada (Play/oficial) |
| Ruta Rentable | — | `PRICE NOT PUBLICLY DISCLOSED` | **~3 días (in-app)** | Google Play (LATAM) |
| Maxymo | Mensual | USD 4,99 | — | IAP publicado |
| Mystro | Mensual / Anual | USD 18,99 / USD 139,99 | — | Sitio oficial (US) |
| Viaje Rentable | Mensual | USD 4,99 (consumible/Play) | — | Play (AR) |
| Operdrive | Mensual / Anual | R$19,90 / R$199,00 | — | Play (BR) |

**Conversión a USD**: solo para análisis comparativo; no se inventan precios de
otros países. **Lectura para SIRC**: prueba gratuita observada en el nicho de
3 a 14 días (DecideRider = 14 días, cercano al modelo SIRC);
rango con precios ~USD 2,40–18,99/mes según región y presencia de automatización.