# Prompt de Investigación para NotebookLM — SIRC

Copia y pega el siguiente texto completo en NotebookLM para realizar la investigación profunda basada en el contexto y la visión estratégica de SIRC:

***

Quiero realizar una investigación profunda para crear una aplicación Android funcional centrada en [

# Contexto Técnico y de Producto: SIRC (Sistema Inteligente de Rentabilidad para Conductores)

## 1. Descripción General del Proyecto
SIRC es una aplicación nativa para Android (desarrollada en Kotlin, Jetpack Compose y Material 3) diseñada para conductores de aplicaciones de transporte (Uber, DiDi, Cabify, InDrive). Su función principal es analizar ofertas de viajes en tiempo real y mostrar mediante un overlay flotante una recomendación con código de colores (semáforo) y métricas derivadas clave (ganancia neta estimada, tarifa por hora, tarifa por kilómetro) en **menos de 3 segundos**, ayudando al conductor a aceptar o rechazar ofertas de forma instantánea sin distraerse.

## 2. Arquitectura y Restricciones Técnicas
- **100% Local y Privado:** Sin backend, sin telemetría, sin envío de datos de pantalla a servidores externos. Todo el procesamiento ocurre en el dispositivo del usuario.
- **Captura y Accesibilidad:** Utiliza servicios de accesibilidad (estrictamente de *solo lectura*, sin realizar gestos ni `performAction`) para capturar textos de pantalla y MediaProjection (con Foreground Service de tipo `specialUse`) para capturar frames de pantalla cuando se requiere OCR visual (ML Kit).
- **Arquitectura Modular (Clean Architecture + MVVM):** Dividido en múltiples módulos (`:domain`, `:core:platform`, `:core:capture`, `:feature:overlay`, `:feature:history`, etc.) para garantizar que las reglas de negocio y los motores de cálculo sean independientes de la interfaz y la plataforma.
- **Rendimiento y Batería Extremos:** Diseñado para uso continuo en turnos de 10-12 horas, empleando mecanismos de *debounce* (400ms), caché de frames por hash, y gestión eficiente de recursos para minimizar drásticamente el consumo de batería y CPU en segundo plano.

## 3. Retos Críticos y Objetivos Actuales
- **Cumplimiento de Seguridad de Google Play y Play Integrity:** Blindar la aplicación para cumplir rigurosamente con las políticas de Google Play y superar los controles de integridad (Play Integrity API), asegurando que la app no sea catalogada falsamente como maliciosa o intrusiva.
- **Resistencia a Restricciones de Uber (Anti-Third-Party / Seguridad):** Sobrevivir al comunicado y políticas de Uber que exigen que cualquier app complementaria pase los más altos estándares de seguridad de Google Play, manteniendo la legalidad y el cumplimiento normativo (al ser de solo lectura y no interferir con la app de transporte).
- **Resiliencia ante Cambios de UI:** Mantener un sistema de parsers especializados por tipo de pantalla (`UberRequestParser`, `UberRadarParser`, etc.) que se adapte ágilmente a los cambios de interfaz de las plataformas de transporte.

## 4. Referencias Competitivas a Analizar
Para inspirar mejoras y nuevas características a futuro, el sistema debe compararse y analizar las mejores prácticas de los siguientes referentes del mercado:
1. **Ruta Rentable** (https://rutarentable.app/)
2. **Motorista One** (https://www.motoristaone.com.br/)
3. **Rinde** (de Alex Collazos)

## 5. Propósito de la Consulta en NotebookLM
Este documento sirve como contexto base para que NotebookLM analice la arquitectura, las restricciones de seguridad (Google Play / Uber), las características de los referentes mencionados, y sugiera mejoras, estrategias normativas y nuevas funcionalidades adaptadas a la filosofía 100% local y de alta velocidad de SIRC.

]. 

Necesito que organices toda la información en torno a cuatro pilares analíticos y tácticos:

1. Fundamentos expertos del sector.
Reúne estudios, informes, papers y referencias fiables que expliquen qué funciona realmente en este sector, qué principios de economía del transporte y UX de baja distracción son importantes, y qué debería tener en cuenta una solución bien planteada para conductores de VTC/ridesharing.

2. Análisis de apps de éxito del sector.
Investiga aplicaciones actuales líderes o muy valoradas dentro de este nicho, con especial atención a **Ruta Rentable**, **Motorista One** y **Rinde** (de Alex Collazos). Identifica funcionalidades, enfoques, patrones comunes, propuestas de valor y elementos que han demostrado funcionar en conversión y retención.

3. Necesidades reales de los usuarios.
Analiza reviews, foros, comunidades de conductores y valoraciones en tiendas de apps. Detecta problemas frecuentes, funciones que echan en falta, frustraciones (como consumo excesivo de batería, bloqueos de cuenta, falsos positivos de seguridad de Uber/Google Play o dificultad de configuración inicial), deseos y mejoras solicitadas.

4. Tendencias, oportunidades y posicionamiento del nicho.
Investiga tendencias actuales, subnichos en crecimiento, perfiles de usuario más interesantes, oportunidades poco cubiertas, problemas emergentes y posibles ángulos diferenciales para posicionar a SIRC con una propuesta de valor única, ultra rápida (<3s), segura y respetuosa de la privacidad.
