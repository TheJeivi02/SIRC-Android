# Prompt de Generación de Informe Ejecutivo para NotebookLM — SIRC

Copia y pega el siguiente texto en NotebookLM una vez que haya procesado las fuentes y la investigación, para que genere el informe ejecutivo detallado y estratégico:

***

Genera un documento ejecutivo integral y detallado que sintetice los hallazgos de la investigación, estructurado rigurosamente para el diseño y evolución del producto **SIRC** (Sistema Inteligente de Rentabilidad para Conductores). 

El informe debe cubrir y profundizar en los siguientes apartados:

1. **Benchmarking Competitivo Amplio (Referentes Regionales y Globales):**
   - Toma como principales puntos de referencia a **Ruta Rentable**, **Motorista One** y **Rinde (de Alex Collazos)**, pero **no te limites exclusivamente a ellos**. Analiza el ecosistema global y regional de aplicaciones de asistencia para conductores de VTC/ridesharing (incluyendo herramientas de filtrado, overlays y calculadoras de rentabilidad en distintas regiones del mundo).
   - Identifica fortalezas, debilidades, flujos de usuario y propuestas de valor comunes en el sector.
   - Destaca qué características han hecho exitosas a estas herramientas y qué vacíos o frustraciones (como baneos, configuración compleja o interfaces saturadas) dejan sin resolver.
   - Propone mejoras e innovaciones que SIRC puede implementar para destacar y superar a la competencia, manteniendo su filosofía 100 % local, de ultra baja distracción y respetuosa de la privacidad.

2. **Necesidades Reales y Puntos de Fricción de los Conductores:**
   - Sintetiza las principales quejas, frustraciones y deseos expresados por conductores en foros, comunidades y reseñas de Google Play/App Store (ej. consumo excesivo de batería en turnos de 10-12 horas, dificultad en la configuración inicial de costos, falsos positivos o bloqueos de cuenta ante políticas de seguridad de Uber y Google Play).
   - Traduce cada dolor del usuario en un requisito técnico o de producto para SIRC.

3. **Estrategia de Seguridad, Cumplimiento y Resiliencia (Play Integrity & Uber Compliance):**
   - Analiza cómo abordar técnicamente las restricciones de seguridad de plataformas como Uber que exigen pasar los estándares de Google Play y bloquear herramientas intrusivas.
   - Define recomendaciones para garantizar que la app pase las validaciones de `Play Integrity API` y mantenga su arquitectura de servicios de accesibilidad estrictamente de *solo lectura* (sin gestos ni automatizaciones prohibidas) sin comprometer la cuenta del conductor.

4. **Optimización de Rendimiento y Arquitectura (Batería y Velocidad <3s):**
   - Propone mejores prácticas de desarrollo Android (Kotlin/Compose) para asegurar que el pipeline de captura (MediaProjection + OCR + Parsers) y el overlay flotante operen con un consumo mínimo de CPU y batería durante jornadas continuas de trabajo.

5. **Backlog de Funcionalidades Recomendadas y Diferenciadores Clave:**
   - Lista un conjunto priorizado de características y mejoras a corto, mediano y largo plazo (por ejemplo: personalización avanzada de umbrales, dashboard estadístico de ganancias por hora/kilómetro, modos de ahorro de energía, adaptación a múltiples plataformas de transporte).

6. **Conclusiones Prácticas para el Prototipo y la Hoja de Ruta:**
   - Cierra con directrices claras y accionables que sirvan como referencia directa para la arquitectura, el diseño de la interfaz (UI/UX de baja distracción) y el desarrollo de los próximos Sprints de SIRC.
