# Análisis Competitivo del Producto

> Consolidación de las tres fuentes de información: **(1)** auditoría técnica del
> repositorio (Sprints 1–11, v1.0.0-rc1), **(2)** `docs/Informe Ejecutivo de
> Estrategia y Diseño…` y **(3)** investigación de mercado con verificación web
> (16-ago-2026). Este documento es la FUENTE ÚNICA de datos competitivos; no
> duplica contenidos del informe estratégico.

## 1. Universo analizado y nivel de verificación

| Competidor | País/Región | Foco | Nivel de verificación |
|---|---|---|---|
| **Ruta Rentable** | LATAM (15 países) | Calculadora automática de rentabilidad pre-aceptación | ✅ Web verificada (Google Play) |
| **Motorista One (ON)** | Brasil | Toolbox multiapp + control financiero | ✅ Web verificada |
| **GigU** | EE. UU./global | Rentabilidad configurada por meta ($/hora, $/km) | ✅ Web verificada |
| **Viaje Rentable** | LATAM | Calculadora de viaje (aparece en búsquedas) | ⚠️ Mención web, sin ficha completa |
| **Radar de Viajes** | LATAM | Semáforo de rentabilidad en overlay | ⚠️ Mención web, sin ficha completa |
| **DecideRider** | Global | Billetera virtual + IA + bots (AFK/automatización) | ⚠️ Ficha en Play; claims de bots sin verificación |
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
| Decisión <3 s estilo semáforo | ✅ | ✅ | ❌ (multiapp, sin filtro EPOH) | ✅ | 🟡 | ❓ |
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
   <3 s, solo lectura).
2. **La automatización de clics es una trampa competitiva.** Varios actores
   (DecideRider con bots, Autoindrive/Maxymo/Mystro) compiten con auto-accept.
   SIRC **no debe imitarlos**: la arquitectura de solo lectura y la política
   anti-baneo (FUENTE 2, §3) son un diferenciador defendible y un requisito de
   Google Play.
3. **Brecha de mercado confirmada**: ninguna app verificada combina a la vez
   (a) decisión <3 s, (b) solo lectura estricto, (c) 100 % local y (d)
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