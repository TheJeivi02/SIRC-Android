### Informe Ejecutivo de Estrategia y Diseño: Evolución del Producto SIRC (Sistema Inteligente de Rentabilidad para Conductores)

#### 1\. Análisis Comparativo y Benchmarking de Referentes (Regional y Global)

En la actual economía de plataformas (GIG economy), la eficiencia del conductor no se mide simplemente por el número de viajes, sino por la optimización de métricas críticas como el  **Earnings Per Online Hour (EPOH)**  y la  **Active Hour Utilization (AHU)** . El ecosistema Android para VTC está saturado de herramientas de asistencia rudimentarias que a menudo introducen riesgos de seguridad o fricción cognitiva. Para que SIRC (Sistema Inteligente de Rentabilidad para Conductores) logre una penetración de mercado efectiva, es vital ejecutar un benchmarking técnico y estratégico que nos permita diferenciar un "overlay informativo" de una "herramienta de arquitectura resiliente" que maximice el ROI sin comprometer la integridad de la cuenta del socio conductor.

##### 1.1. Evaluación de Competidores Directos e Indirectos

El análisis de referentes revela que, aunque existen soluciones para la gestión de costos, la mayoría falla en la ejecución técnica en tiempo real y en la protección contra algoritmos de detección de fraude.| Referente | Diferenciador Clave | Impacto en la Rentabilidad del Conductor || \------ | \------ | \------ || **Ruta Rentable** | Estimación manual de costos fijos y variables. | Mejora la consciencia financiera, pero su alta carga cognitiva reduce el AHU debido a la entrada manual de datos. || **Motorista One** | Interfaz multiapp mediante overlays básicos. | Facilita el "multi-apping", pero carece de filtrado por EPOH, resultando en la aceptación de viajes de baja rentabilidad. || **Rinde** | Consolidación de reportes fiscales y contables. | Optimiza el margen neto post-operativo, pero no asiste en la toma de decisiones crítica durante la conducción. |  
A nivel global, el éxito de las herramientas de asistencia radica en el  **parsing pasivo de datos** . SIRC capitalizará esta tendencia utilizando arquitecturas de solo lectura para evitar la detección por parte de Uber/Didi, garantizando que el filtrado de ofertas se base en ganancias reales por kilómetro y tiempo.

##### 1.2. Identificación de Brechas de Mercado y Oportunidades de SIRC

SIRC llenará los vacíos estratégicos dejados por la competencia mediante los siguientes pilares técnicos:

* **Integridad y Cumplimiento:**  A diferencia de las apps que manipulan el GPS, SIRC se alinea con la política de Uber contra el "aumento deliberado de tiempo o distancia". Nuestra lógica de ruteo prioriza la eficiencia, asegurando que la optimización de ganancias no se interprete como una ruta fraudulentamente larga.  
* **Filosofía de "Ultra Baja Distracción":**  Reducción drástica del  *Fixation Count* . Mientras otros inundan la pantalla, SIRC respeta las Áreas de Interés (AOI) críticas como el velocímetro.  
* **Resiliencia Energética:**  Implementación de Shared Memory (JSSEC 4.11.4.2) para minimizar ciclos de CPU, vital para dispositivos de gama media en jornadas de 12 horas.  
* **Privacidad Total:**  Arquitectura basada en servicios de accesibilidad de solo lectura que prohíbe gestos automáticos, evitando alertas de "comportamiento robótico" en los sistemas de seguridad de la plataforma.

#### 2\. Diagnóstico de Necesidades y Fricciones Críticas del Conductor

Como Arquitecto Principal, entiendo que el rendimiento técnico es una característica de seguridad. Alinear el desarrollo con el "dolor" operativo del usuario es imperativo para reducir el Churn Rate (tasa de abandono); una app que falla en un momento crítico de decisión puede significar la pérdida de una oferta de alta rentabilidad o, peor aún, un riesgo vial.

##### 2.1. Mapeo de Puntos de Fricción Operativa

Basándonos en el análisis de comunidades VTC y estudios de ergonomía digital, traducimos las quejas recurrentes en especificaciones técnicas de alta fidelidad.| Dolor del Usuario | Requisito Técnico para SIRC || \------ | \------ || **Fatiga por Decisión:**  Segundos para evaluar rentabilidad de oferta. | Overlay de semáforo instantáneo (ROI basado en EPOH). || **Riesgo de Baneo:**  Miedo a ser detectado por "apps de terceros". | Implementación de Play Integrity API con certificación de binario legítimo. || **Drenaje Térmico:**  El teléfono se calienta y cierra la navegación. | Pipeline de OCR optimizado mediante Dispatchers.Default en Kotlin Coroutines. || **Distracción Visual:**  Demasiados elementos compitiendo con la carretera. | Diseño de interfaz periférica que respeta el AOI del velocímetro y entorno. |

##### 2.2. Análisis de la Carga Cognitiva y Seguridad Vial

Según el estudio de atención visual (arXiv), en conducción manual los conductores mantienen el  **75.4%**  de su atención en la carretera. Sin embargo, durante las transiciones de oferta, la atención se desplaza críticamente hacia el  **velocímetro y el entorno** . SIRC debe evitar competir con estos elementos.**Principios de Diseño de Interfaz para Seguridad Visual:**

1. **Umbral de Fijación:**  El overlay de SIRC no debe requerir más de  **una fijación visual por cada intervalo de 2 minutos** , a menos que ingrese una nueva oferta.  
2. **Ubicación Periférica:**  El diseño debe dejar libre el área visual del velocímetro (AOI central) para mantener la consciencia situacional del conductor.  
3. **Contraste de Alta Fidelidad:**  Adaptación dinámica para visión diurna/nocturna, eliminando la necesidad de que el conductor ajuste el brillo manualmente.La resiliencia de la app es tanto técnica como regulatoria; proteger la atención del conductor es proteger su herramienta de trabajo y su vida.

#### 3\. Estrategia de Seguridad, Cumplimiento y Resiliencia (Anti-Baneo)

La integridad de la plataforma es la prioridad uno. De acuerdo con las políticas de fraude de Uber, la manipulación de datos de GPS y la realización de rutas deshonestas son causales de desactivación permanente. SIRC garantiza el cumplimiento proactivo mediante una arquitectura no intrusiva.

##### 3.1. Alineación con Play Integrity y Políticas de Uber

SIRC se certifica como una herramienta de productividad legal mediante:

* **Strong Integrity Check:**  Implementación de la Play Integrity API para detectar firmas de root o bootloaders desbloqueados, asegurando que SIRC no coexista con apps de GPS Spoofing que comprometan la cuenta del usuario.  
* **Shortest Path Alignment:**  Nuestra lógica de rentabilidad se ajusta al estándar de Uber de "ruta más eficiente", evitando cualquier sugerencia de alargamiento de ruta fraudulento.**Checklist de Cumplimiento Técnico (Architect Grade):**  
*   **Validación de Binario:**  Verificación de firma en cada arranque mediante API de integridad de Google.  
*   **Aislamiento de Sensores:**  Cero acceso a MOCK\_LOCATION o inyección de Intents de ubicación.  
*   **Seguridad de Intents:**  Implementación de "Safer Intents" (JSSEC 4.7.3.6) para evitar ataques de redirección de Intents en Android 16\.  
*   **No-Click Policy:**  Deshabilitación técnica de cualquier función de auto-clic en la interfaz de la app de transporte.

##### 3.2. Arquitectura de Accesibilidad de Solo Lectura

SIRC utiliza el AccessibilityService exclusivamente para el parsing de texto, actuando como un sensor pasivo. Según JSSEC 5.6.3.8, utilizaremos la Key Sharing API para compartir datos de rentabilidad de forma segura entre las versiones "Lite" y "Pro" del ecosistema SIRC.**Diagrama de Flujo de Datos No Intrusivo:**  Pantalla (Uber) \-\> MediaProjection / AccessibilityService (READ-ONLY) \-\> Parser Engine (Kotlin Dispatchers.Default) \-\> Logic ROI Filter \-\> Jetpack Compose Overlay (SIRC UI)Este flujo garantiza que no existan clics automáticos que activen las alarmas de seguridad de Uber por comportamiento robótico.

#### 4\. Optimización de Arquitectura y Rendimiento Energético

En turnos de 12 horas, la eficiencia energética es una característica de seguridad vial. Un dispositivo lento es un conductor distraído.

##### 4.1. Pipeline de Captura y Procesamiento Eficiente

Para asegurar un procesamiento de ofertas en \<1s sin bloquear el Main Thread, prescribo la siguiente secuencia técnica:

1. **MediaProjection Capture:**  Obtención de frame asíncrono en un Bitmap Buffer.  
2. **Shared Memory Allocation:**  Almacenamiento del buffer en memoria compartida (JSSEC 4.11.4.2) para evitar serialización costosa entre el servicio de fondo y el motor de análisis.  
3. **OCR Processing:**  Ejecución mediante ML Kit utilizando Dispatchers.Default (Coroutines) para maximizar el uso de núcleos de CPU sin lag.  
4. **ROI Algorithm:**  Cálculo instantáneo de ganancias por km y tiempo basado en umbrales de usuario.  
5. **Jetpack Compose Overlay:**  Actualización reactiva de la UI en el Dispatchers.Main.

##### 4.2. Estrategia de Conservación de Batería

Optimizaciones de bajo nivel para extender la vida útil de la batería en Android:

* **Adaptive Refresh Rate:**  Reducción de la tasa de refresco del overlay a 1Hz si el GPS detecta que el vehículo está detenido.  
* **Zero-Serialization:**  Uso de Shared Memory para reducir el consumo de CPU en la transferencia de datos entre procesos.  
* **OCR Triggering:**  El motor de OCR solo se despierta tras detectar un cambio de layout en la app de Uber, evitando escaneos redundantes.  
* **Doze Mode Compatibility:**  Registro del servicio como "Foreground Service" de alta prioridad pero optimizado para respetar los límites de batería del sistema.  
* **Thermal Throttling Prevention:**  Reducción automática de la complejidad del parsing si la temperatura del SoC excede los 40°C.

#### 5\. Backlog de Funcionalidades y Diferenciadores Clave

Priorizamos funcionalidades que impactan directamente en el EPOH (Ingresos por Hora Online) con el menor esfuerzo cognitivo posible.

##### 5.1. Funcionalidades Críticas (Corto Plazo)

* **Umbrales de Rentabilidad Dinámicos:**  Definición de ganancia mínima por km/hora con resalte visual inmediato.  
* **Overlay de Decisión "Semáforo":**  Un indicador visual que no requiere lectura de cifras, solo reconocimiento de color (Verde \= ROI Óptimo).  
* **Modo Anti-Fatiga:**  Alertas visuales suaves basadas en el tiempo de conexión acumulado.

##### 5.2. Diferenciadores Estratégicos (Mediano/Largo Plazo)

**Matriz de Impacto vs. Esfuerzo:**| Funcionalidad | Esfuerzo | Impacto | Justificación Estratégica || \------ | \------ | \------ | \------ || **Adaptación Multi-plataforma Automática** | Alto | Alto | Fundamental para dominar el mercado de conductores "multi-app". || **Ahorro de Energía Inteligente (SOC-Aware)** | Medio | Alto | Diferenciador crítico para conductores con dispositivos de gama media/baja. || **Dashboard de AHU (Active Hour Utilization)** | Medio | Medio | Herramienta de análisis para profesionalizar la labor del conductor. |

#### 6\. Conclusiones y Hoja de Ruta (Roadmap)

SIRC no es simplemente una capa visual; es una infraestructura de datos diseñada para la resiliencia en un entorno de alta presión operativa. Nuestra visión integra la seguridad del código JSSEC con la psicología de atención del conductor para crear la herramienta definitiva de rentabilidad.

##### 6.1. Directrices de Diseño UI/UX (Golden Rules)

1. **Fixation Count Control:**  El overlay jamás requerirá más de una fijación ocular para interpretar la rentabilidad.  
2. **High Contrast/Low Density:**  Pocos elementos, máximo contraste (adaptación diurna/nocturna).  
3. **Large AOI Controls:**  Áreas táctiles simplificadas para facilitar el uso sin precisión milimétrica.  
4. **Respect for Environment:**  El overlay debe situarse en zonas que no obstruyan el velocímetro ni los espejos.  
5. **Haptic Feedback:**  Vibraciones cortas para ofertas que superan el umbral de "Alta Rentabilidad", reduciendo la necesidad de mirar la pantalla.

##### 6.2. Cronograma de Implementación Técnica (Roadmap)

* **Hito 1 (Meses 1-2): Validación de Integridad y Core:**  Implementación de Play Integrity (Strong) y arquitectura de accesibilidad Read-Only.  
* **Hito 2 (Meses 3-4): MVP de Performance:**  Pipeline de OCR optimizado con Shared Memory y primer overlay de Jetpack Compose.  
* **Hito 3 (Meses 5-6): Lanzamiento de Backlog y Android 16 Ready:**  Despliegue de filtrado avanzado y auditoría de compatibilidad para Android 16 (Ordered Broadcasts priority y Safer Intents).SIRC se consolida como el estándar de oro en movilidad VTC, reafirmando nuestro compromiso con la ética del conductor, la seguridad vial y la excelencia técnica en el ecosistema Android.\# Informe Ejecutivo de Estrategia y Diseño: Evolución del Producto SIRC (Sistema Inteligente de Rentabilidad para Conductores)

#### 1\. Análisis Comparativo y Benchmarking de Referentes (Regional y Global)

En la actual economía de plataformas (GIG economy), la eficiencia del conductor no se mide simplemente por el número de viajes, sino por la optimización de métricas críticas como el  **Earnings Per Online Hour (EPOH)**  y la  **Active Hour Utilization (AHU)** . El ecosistema Android para VTC está saturado de herramientas de asistencia rudimentarias que a menudo introducen riesgos de seguridad o fricción cognitiva. Para que SIRC (Sistema Inteligente de Rentabilidad para Conductores) logre una penetración de mercado efectiva, es vital ejecutar un benchmarking técnico y estratégico que nos permita diferenciar un "overlay informativo" de una "herramienta de arquitectura resiliente" que maximice el ROI sin comprometer la integridad de la cuenta del socio conductor.

##### 1.1. Evaluación de Competidores Directos e Indirectos

El análisis de referentes revela que, aunque existen soluciones para la gestión de costos, la mayoría falla en la ejecución técnica en tiempo real y en la protección contra algoritmos de detección de fraude.| Referente | Diferenciador Clave | Impacto en la Rentabilidad del Conductor || \------ | \------ | \------ || **Ruta Rentable** | Estimación manual de costos fijos y variables. | Mejora la consciencia financiera, pero su alta carga cognitiva reduce el AHU debido a la entrada manual de datos. || **Motorista One** | Interfaz multiapp mediante overlays básicos. | Facilita el "multi-apping", pero carece de filtrado por EPOH, resultando en la aceptación de viajes de baja rentabilidad. || **Rinde** | Consolidación de reportes fiscales y contables. | Optimiza el margen neto post-operativo, pero no asiste en la toma de decisiones crítica durante la conducción. |  
A nivel global, el éxito de las herramientas de asistencia radica en el  **parsing pasivo de datos** . SIRC capitalizará esta tendencia utilizando arquitecturas de solo lectura para evitar la detección por parte de Uber/Didi, garantizando que el filtrado de ofertas se base en ganancias reales por kilómetro y tiempo.

##### 1.2. Identificación de Brechas de Mercado y Oportunidades de SIRC

SIRC llenará los vacíos estratégicos dejados por la competencia mediante los siguientes pilares técnicos:

* **Integridad y Cumplimiento:**  A diferencia de las apps que manipulan el GPS, SIRC se alinea con la política de Uber contra el "aumento deliberado de tiempo o distancia". Nuestra lógica de ruteo prioriza la eficiencia, asegurando que la optimización de ganancias no se interprete como una ruta fraudulentamente larga.  
* **Filosofía de "Ultra Baja Distracción":**  Reducción drástica del  *Fixation Count* . Mientras otros inundan la pantalla, SIRC respeta las Áreas de Interés (AOI) críticas como el velocímetro.  
* **Resiliencia Energética:**  Implementación de Shared Memory (JSSEC 4.11.4.2) para minimizar ciclos de CPU, vital para dispositivos de gama media en jornadas de 12 horas.  
* **Privacidad Total:**  Arquitectura basada en servicios de accesibilidad de solo lectura que prohíbe gestos automáticos, evitando alertas de "comportamiento robótico" en los sistemas de seguridad de la plataforma.

#### 2\. Diagnóstico de Necesidades y Fricciones Críticas del Conductor

Como Arquitecto Principal, entiendo que el rendimiento técnico es una característica de seguridad. Alinear el desarrollo con el "dolor" operativo del usuario es imperativo para reducir el Churn Rate (tasa de abandono); una app que falla en un momento crítico de decisión puede significar la pérdida de una oferta de alta rentabilidad o, peor aún, un riesgo vial.

##### 2.1. Mapeo de Puntos de Fricción Operativa

Basándonos en el análisis de comunidades VTC y estudios de ergonomía digital, traducimos las quejas recurrentes en especificaciones técnicas de alta fidelidad.| Dolor del Usuario | Requisito Técnico para SIRC || \------ | \------ || **Fatiga por Decisión:**  Segundos para evaluar rentabilidad de oferta. | Overlay de semáforo instantáneo (ROI basado en EPOH). || **Riesgo de Baneo:**  Miedo a ser detectado por "apps de terceros". | Implementación de Play Integrity API con certificación de binario legítimo. || **Drenaje Térmico:**  El teléfono se calienta y cierra la navegación. | Pipeline de OCR optimizado mediante Dispatchers.Default en Kotlin Coroutines. || **Distracción Visual:**  Demasiados elementos compitiendo con la carretera. | Diseño de interfaz periférica que respeta el AOI del velocímetro y entorno. |

##### 2.2. Análisis de la Carga Cognitiva y Seguridad Vial

Según el estudio de atención visual (arXiv), en conducción manual los conductores mantienen el  **75.4%**  de su atención en la carretera. Sin embargo, durante las transiciones de oferta, la atención se desplaza críticamente hacia el  **velocímetro y el entorno** . SIRC debe evitar competir con estos elementos.**Principios de Diseño de Interfaz para Seguridad Visual:**

1. **Umbral de Fijación:**  El overlay de SIRC no debe requerir más de  **una fijación visual por cada intervalo de 2 minutos** , a menos que ingrese una nueva oferta.  
2. **Ubicación Periférica:**  El diseño debe dejar libre el área visual del velocímetro (AOI central) para mantener la consciencia situacional del conductor.  
3. **Contraste de Alta Fidelidad:**  Adaptación dinámica para visión diurna/nocturna, eliminando la necesidad de que el conductor ajuste el brillo manualmente.La resiliencia de la app es tanto técnica como regulatoria; proteger la atención del conductor es proteger su herramienta de trabajo y su vida.

#### 3\. Estrategia de Seguridad, Cumplimiento y Resiliencia (Anti-Baneo)

La integridad de la plataforma es la prioridad uno. De acuerdo con las políticas de fraude de Uber, la manipulación de datos de GPS y la realización de rutas deshonestas son causales de desactivación permanente. SIRC garantiza el cumplimiento proactivo mediante una arquitectura no intrusiva.

##### 3.1. Alineación con Play Integrity y Políticas de Uber

SIRC se certifica como una herramienta de productividad legal mediante:

* **Strong Integrity Check:**  Implementación de la Play Integrity API para detectar firmas de root o bootloaders desbloqueados, asegurando que SIRC no coexista con apps de GPS Spoofing que comprometan la cuenta del usuario.  
* **Shortest Path Alignment:**  Nuestra lógica de rentabilidad se ajusta al estándar de Uber de "ruta más eficiente", evitando cualquier sugerencia de alargamiento de ruta fraudulento.**Checklist de Cumplimiento Técnico (Architect Grade):**  
*   **Validación de Binario:**  Verificación de firma en cada arranque mediante API de integridad de Google.  
*   **Aislamiento de Sensores:**  Cero acceso a MOCK\_LOCATION o inyección de Intents de ubicación.  
*   **Seguridad de Intents:**  Implementación de "Safer Intents" (JSSEC 4.7.3.6) para evitar ataques de redirección de Intents en Android 16\.  
*   **No-Click Policy:**  Deshabilitación técnica de cualquier función de auto-clic en la interfaz de la app de transporte.

##### 3.2. Arquitectura de Accesibilidad de Solo Lectura

SIRC utiliza el AccessibilityService exclusivamente para el parsing de texto, actuando como un sensor pasivo. Según JSSEC 5.6.3.8, utilizaremos la Key Sharing API para compartir datos de rentabilidad de forma segura entre las versiones "Lite" y "Pro" del ecosistema SIRC.**Diagrama de Flujo de Datos No Intrusivo:**  Pantalla (Uber) \-\> MediaProjection / AccessibilityService (READ-ONLY) \-\> Parser Engine (Kotlin Dispatchers.Default) \-\> Logic ROI Filter \-\> Jetpack Compose Overlay (SIRC UI)Este flujo garantiza que no existan clics automáticos que activen las alarmas de seguridad de Uber por comportamiento robótico.

#### 4\. Optimización de Arquitectura y Rendimiento Energético

En turnos de 12 horas, la eficiencia energética es una característica de seguridad vial. Un dispositivo lento es un conductor distraído.

##### 4.1. Pipeline de Captura y Procesamiento Eficiente

Para asegurar un procesamiento de ofertas en \<1s sin bloquear el Main Thread, prescribo la siguiente secuencia técnica:

1. **MediaProjection Capture:**  Obtención de frame asíncrono en un Bitmap Buffer.  
2. **Shared Memory Allocation:**  Almacenamiento del buffer en memoria compartida (JSSEC 4.11.4.2) para evitar serialización costosa entre el servicio de fondo y el motor de análisis.  
3. **OCR Processing:**  Ejecución mediante ML Kit utilizando Dispatchers.Default (Coroutines) para maximizar el uso de núcleos de CPU sin lag.  
4. **ROI Algorithm:**  Cálculo instantáneo de ganancias por km y tiempo basado en umbrales de usuario.  
5. **Jetpack Compose Overlay:**  Actualización reactiva de la UI en el Dispatchers.Main.

##### 4.2. Estrategia de Conservación de Batería

Optimizaciones de bajo nivel para extender la vida útil de la batería en Android:

* **Adaptive Refresh Rate:**  Reducción de la tasa de refresco del overlay a 1Hz si el GPS detecta que el vehículo está detenido.  
* **Zero-Serialization:**  Uso de Shared Memory para reducir el consumo de CPU en la transferencia de datos entre procesos.  
* **OCR Triggering:**  El motor de OCR solo se despierta tras detectar un cambio de layout en la app de Uber, evitando escaneos redundantes.  
* **Doze Mode Compatibility:**  Registro del servicio como "Foreground Service" de alta prioridad pero optimizado para respetar los límites de batería del sistema.  
* **Thermal Throttling Prevention:**  Reducción automática de la complejidad del parsing si la temperatura del SoC excede los 40°C.

#### 5\. Backlog de Funcionalidades y Diferenciadores Clave

Priorizamos funcionalidades que impactan directamente en el EPOH (Ingresos por Hora Online) con el menor esfuerzo cognitivo posible.

##### 5.1. Funcionalidades Críticas (Corto Plazo)

* **Umbrales de Rentabilidad Dinámicos:**  Definición de ganancia mínima por km/hora con resalte visual inmediato.  
* **Overlay de Decisión "Semáforo":**  Un indicador visual que no requiere lectura de cifras, solo reconocimiento de color (Verde \= ROI Óptimo).  
* **Modo Anti-Fatiga:**  Alertas visuales suaves basadas en el tiempo de conexión acumulado.

##### 5.2. Diferenciadores Estratégicos (Mediano/Largo Plazo)

**Matriz de Impacto vs. Esfuerzo:**| Funcionalidad | Esfuerzo | Impacto | Justificación Estratégica || \------ | \------ | \------ | \------ || **Adaptación Multi-plataforma Automática** | Alto | Alto | Fundamental para dominar el mercado de conductores "multi-app". || **Ahorro de Energía Inteligente (SOC-Aware)** | Medio | Alto | Diferenciador crítico para conductores con dispositivos de gama media/baja. || **Dashboard de AHU (Active Hour Utilization)** | Medio | Medio | Herramienta de análisis para profesionalizar la labor del conductor. |

#### 6\. Conclusiones y Hoja de Ruta (Roadmap)

SIRC no es simplemente una capa visual; es una infraestructura de datos diseñada para la resiliencia en un entorno de alta presión operativa. Nuestra visión integra la seguridad del código JSSEC con la psicología de atención del conductor para crear la herramienta definitiva de rentabilidad.

##### 6.1. Directrices de Diseño UI/UX (Golden Rules)

1. **Fixation Count Control:**  El overlay jamás requerirá más de una fijación ocular para interpretar la rentabilidad.  
2. **High Contrast/Low Density:**  Pocos elementos, máximo contraste (adaptación diurna/nocturna).  
3. **Large AOI Controls:**  Áreas táctiles simplificadas para facilitar el uso sin precisión milimétrica.  
4. **Respect for Environment:**  El overlay debe situarse en zonas que no obstruyan el velocímetro ni los espejos.  
5. **Haptic Feedback:**  Vibraciones cortas para ofertas que superan el umbral de "Alta Rentabilidad", reduciendo la necesidad de mirar la pantalla.

##### 6.2. Cronograma de Implementación Técnica (Roadmap)

* **Hito 1 (Meses 1-2): Validación de Integridad y Core:**  Implementación de Play Integrity (Strong) y arquitectura de accesibilidad Read-Only.  
* **Hito 2 (Meses 3-4): MVP de Performance:**  Pipeline de OCR optimizado con Shared Memory y primer overlay de Jetpack Compose.  
* **Hito 3 (Meses 5-6): Lanzamiento de Backlog y Android 16 Ready:**  Despliegue de filtrado avanzado y auditoría de compatibilidad para Android 16 (Ordered Broadcasts priority y Safer Intents).SIRC se consolida como el estándar de oro en movilidad VTC, reafirmando nuestro compromiso con la ética del conductor, la seguridad vial y la excelencia técnica en el ecosistema Android.

