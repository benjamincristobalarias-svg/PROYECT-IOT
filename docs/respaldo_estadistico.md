# Respaldo Estadístico y Justificación del Problema

**Proyecto:** Sistema IoT de monitoreo de salud estructural mediante análisis de frecuencia natural
**Asignatura:** TI3042 — Aplicaciones Móviles para IoT
**Áreas de impacto:** Seguridad estructural, Infraestructura habitacional y Protección civil

---

## 1. Definición del problema

En Chile, la evaluación del estado estructural de una edificación depende casi exclusivamente de la **inspección visual realizada por un profesional**, generalmente ejecutada después de que ocurre un evento sísmico o de que aparecen daños visibles como grietas, deformaciones o desprendimientos.

Este enfoque presenta tres limitaciones críticas:

1. **Es reactivo, no preventivo.** Se actúa cuando el daño ya existe y es visible. La degradación estructural progresiva (fatiga de materiales, corrosión de armaduras, aflojamiento de conexiones, microfisuración) avanza durante años sin manifestación externa.

2. **Depende de disponibilidad profesional.** Tras un sismo significativo, la demanda de inspecciones excede ampliamente la capacidad de respuesta de los equipos técnicos disponibles, dejando a miles de familias sin información para decidir si es seguro habitar su vivienda.

3. **No detecta daño interno.** Una estructura puede haber perdido capacidad resistente sin presentar ninguna señal visible en su terminación superficial.

**El problema central:** no existe, para la vivienda común, ningún mecanismo de monitoreo continuo y de bajo costo que permita saber si la estructura ha perdido rigidez.

---

## 2. Respaldo estadístico — Riesgo sísmico y daño estructural en Chile

### 2.1 Magnitud del riesgo

Chile es reconocido como uno de los países con mayor actividad sísmica del mundo. El terremoto del 27 de febrero de 2010 (27F), de magnitud **8,8 Mw**, se ubica entre los diez eventos sísmicos de mayor magnitud registrados a nivel mundial.

### 2.2 Consecuencias documentadas del 27F

| Indicador | Cifra |
|---|---|
| Personas afectadas | Más de 2.000.000 |
| Víctimas fatales | 525 |
| Viviendas dañadas (total) | Aproximadamente 1.500.000 |
| Viviendas con daño severo | Aproximadamente 500.000 |
| Viviendas destruidas | Más de 200.000 |
| Daño económico total | Cerca de US$ 30.000 millones |
| Equivalencia respecto del PIB nacional | 18% |

**Daño en infraestructura crítica (Informe Comisión Investigadora, Cámara de Diputados, 2011):**

| Tipo de infraestructura | Cantidad afectada |
|---|---|
| Puentes caídos | 200 |
| Hospitales dañados | 73 |
| Colegios afectados | 4.000 |
| Pueblos y comunidades golpeadas | Cerca de 900 |

**Daño estructural solo en la Provincia de Santiago:** se registraron **10.705 viviendas** y **560 construcciones de dimensión mayor** con daño estructural, en una zona donde la intensidad fue inferior a la de las regiones del Maule y Biobío.

### 2.3 El problema de la evaluación post-sismo

Tras un evento sísmico se requieren dos instancias: una **inspección y evaluación rápida** de los edificios existentes, y una **evaluación posterior más larga y detallada**. Ambas deben ser ejecutadas por profesionales técnicos —arquitectos, ingenieros— quienes determinan si las personas pueden acceder a sus viviendas con seguridad.

Este procedimiento es indispensable, pero su cuello de botella es evidente: con 1,5 millones de viviendas dañadas y una cantidad limitada de profesionales habilitados, el tiempo de espera para obtener un veredicto se extiende, y durante ese período las familias deben decidir sin información técnica.

### 2.4 Estado del parque habitacional existente

Según la **Encuesta CASEN 2022 (MINVU / Ministerio de Desarrollo Social y Familia)**, **1.263.576 viviendas** presentan déficit habitacional cualitativo —equivalente al **18,5% del total de 6.824.380 viviendas** del país— incluyendo requerimientos de **mejoramiento y conservación material** de muros, techo y piso. Este déficit se concentra en los **dos primeros quintiles de menores ingresos**.

La medición **CASEN 2024** identifica **1.239.815 viviendas** con al menos un requerimiento cualitativo.

Adicionalmente, las normas de construcción chilenas fueron **modificadas en 2011** a raíz de las lecciones del 27F. Esto implica que todo el parque construido con anterioridad responde a estándares previos a esa actualización.

---

## 3. Fundamento técnico de la solución

### 3.1 Principio físico

Toda estructura se comporta como un **resonador mecánico**, vibrando a frecuencias naturales determinadas por su geometría, su masa y su rigidez estructural.

El viento, el tránsito vehicular, la actividad de los ocupantes y el ruido ambiental excitan continuamente la estructura, pero **las frecuencias naturales permanecen estables mientras la estructura se mantiene íntegra**.

La relación clave es la siguiente:

> **Una disminución en la frecuencia natural indica pérdida de rigidez.**

Dado que la rigidez depende de las propiedades físicas de los elementos resistentes, cualquier daño —corrosión, aflojamiento de juntas, microfisuración, pérdida de arriostramiento— produce una variación medible en la respuesta dinámica de la estructura, **incluso antes de ser visible**.

### 3.2 Validez del método

Esta técnica corresponde a la disciplina de **Structural Health Monitoring (SHM)**, empleada profesionalmente en puentes, torres de transmisión, edificios patrimoniales e infraestructura crítica. La literatura técnica establece que el monitoreo continuo de vibraciones constituye un **indicador no intrusivo y fiable del estado estructural, sin requerir inspección física ni ensayos de carga**.

En aplicaciones documentadas, el sistema permite mantenimiento preventivo ante **cambios del orden del 5% en las frecuencias** detectadas.

### 3.3 Herramienta matemática

Para pasar de la señal de aceleración registrada en el tiempo a su contenido en frecuencias se emplea el **análisis de Fourier (FFT)**, técnica estándar en la disciplina para identificar cambios sutiles en los patrones de vibración que señalan anomalías estructurales.

---

## 4. Población afectada

| Grupo | Razón de la vulnerabilidad |
|---|---|
| **Habitantes de viviendas anteriores a 2011** | Construidas bajo normas previas a la actualización post-27F |
| **Hogares de los quintiles I y II** | Concentran el déficit habitacional cualitativo; sin capacidad de financiar una inspección estructural profesional |
| **Comunidades de edificios residenciales** | Una decisión de evacuación afecta simultáneamente a decenas o cientos de familias |
| **Población en zonas de alta sismicidad** | Todo el territorio nacional, con mayor exposición en las macrozonas centro y sur |
| **Usuarios de infraestructura crítica** | Hospitales, colegios y edificios públicos, cuyo daño compromete la respuesta ante la emergencia |

**Escala del problema:** considerando únicamente el parque habitacional con déficit cualitativo (1,26 millones de viviendas) y un promedio cercano a 3 personas por hogar, la población expuesta a habitar estructuras con estado de conservación deficiente supera los **3,5 millones de personas**.

---

## 5. Propuesta de solución

### 5.1 Descripción

Se propone un **sistema IoT de monitoreo continuo de salud estructural** compuesto por:

- Un **microcontrolador Arduino Uno** que ejecuta la adquisición y el procesamiento de la señal.
- Un **acelerómetro MPU6050**, fijado a un elemento estructural, que registra las vibraciones ambientales de la estructura.
- Un **algoritmo FFT** embebido que transforma la señal temporal en su espectro de frecuencias e identifica el pico correspondiente a la frecuencia natural dominante.
- Un **actuador de alerta (LED indicador y buzzer)** que se activa cuando la frecuencia detectada cae por debajo del umbral definido respecto de la línea base registrada.
- Una **aplicación móvil Android**, desarrollada en unidades posteriores, que permite visualizar la frecuencia actual, compararla con el histórico y recibir notificaciones.

### 5.2 Lógica de operación

1. **Fase de calibración:** el sistema registra la frecuencia natural de la estructura en su estado íntegro, estableciendo la **línea base**.
2. **Fase de monitoreo:** el sistema muestrea periódicamente las vibraciones ambientales y calcula la frecuencia dominante.
3. **Fase de evaluación:** compara la frecuencia actual contra la línea base. Una caída sostenida por sobre el umbral definido indica pérdida de rigidez.
4. **Fase de alerta:** activa el actuador y notifica al usuario mediante la aplicación móvil.

### 5.3 Cómo la propuesta incide sobre las estadísticas presentadas

| Situación documentada | Mecanismo de la propuesta | Efecto esperado |
|---|---|---|
| Evaluación post-sismo depende de profesionales escasos | Monitoreo automático y permanente, sin intervención humana | Entrega un primer indicador objetivo mientras llega la inspección profesional |
| El daño estructural interno no es visible | Medición de una propiedad física (rigidez) y no de una manifestación superficial | Detecta degradación antes de que aparezcan grietas |
| 1,26 millones de viviendas con déficit de conservación material | Dispositivo de bajo costo instalable en vivienda existente sin obra | Hace viable el monitoreo en el segmento que no puede costear una evaluación técnica |
| Parque construido bajo normas previas a 2011 | Seguimiento continuo del comportamiento real, no del diseño teórico | Permite priorizar cuáles estructuras requieren evaluación profesional |
| 2 millones de personas afectadas en un solo evento | Registro objetivo del estado pre y post evento | Aporta información para decidir evacuación y para priorizar la respuesta |

### 5.4 Ventaja diferencial

El monitoreo de salud estructural profesional existe y es confiable, pero su costo lo restringe a infraestructura crítica de alto valor. La propuesta **no reemplaza la evaluación de un ingeniero estructural**, sino que actúa como un **sistema de tamizaje continuo y de bajo costo** que indica cuándo esa evaluación se vuelve necesaria — trasladando la lógica del SHM industrial a la vivienda común.

### 5.5 Proyección del sistema

El mismo principio y hardware permite extender el proyecto hacia:

- **Detección de asentamiento diferencial**, mediante el seguimiento de la inclinación permanente registrada por el giróscopo del MPU6050.
- **Medición de deriva entre pisos**, incorporando un segundo nodo sensor en un nivel distinto de la estructura.
- **Registro de aceleración máxima durante eventos sísmicos**, para clasificación de daño post-evento.

Estas extensiones se declaran como trabajo futuro y no forman parte del alcance de la presente implementación.

---

## 6. Alineación con Objetivos de Desarrollo Sostenible (ONU)

**ODS 11 — Ciudades y comunidades sostenibles.** Meta 11.5: reducir significativamente el número de muertes y personas afectadas por los desastres, incluidos los relacionados con el agua, y las pérdidas económicas directas provocadas por los desastres en relación con el PIB mundial. El sistema aporta información temprana sobre integridad estructural, reduciendo la exposición de personas a edificaciones comprometidas.

**ODS 9 — Industria, innovación e infraestructura.** Meta 9.1: desarrollar infraestructuras fiables, sostenibles y resilientes. La propuesta traslada una tecnología de monitoreo industrial hacia una implementación accesible, contribuyendo a la resiliencia del parque construido existente.

**ODS 3 — Salud y bienestar (secundario).** Prevención de lesiones y muertes asociadas a colapso estructural.

---

## 7. Fuentes

1. Presidencia de la República de Chile (2013), citado en Sáez, E. et al. "Características del terreno de fundación de sitios con edificios dañados severamente en el terremoto del 27F". *Obras y Proyectos*, 2017. https://www.scielo.cl/scielo.php?script=sci_arttext&pid=S0718-28132017000100006
2. Comisión Especial Investigadora, Cámara de Diputados de Chile (2011). *Informe sobre el terremoto y maremoto del 27 de febrero de 2010.*
3. IDIEM, Universidad de Chile — declaraciones sobre cifras oficiales de daño del 27F, "A 14 años del 27F: ¿Cuáles son los avances y aprendizajes en Chile?" (2024).
4. Gobierno de Chile (2010) / OPS (2010). Estimación de daños económicos del 27F.
5. MINVU, Centro de Estudios Ciudad y Territorio. *Déficit Habitacional en Chile — CASEN 2022 y CASEN 2024.* https://centrodeestudios.minvu.gob.cl/deficit-habitacional/
6. Dewesoft. *Monitorización de la salud estructural de torres de transmisión* — fundamento de frecuencias naturales y pérdida de rigidez. https://dewesoft.com/es/blog/monitorizacion-del-estado-estructural-de-torres-de-transmision
7. Eastern Engineering Group. *Monitoreo de Salud Estructural: asegurando seguridad y longevidad.*
8. *Monitoreo de la Salud Estructural de un Puente Peatonal* (2025) — detección de daño mediante desplazamiento de frecuencias modales.
9. Naciones Unidas. *Objetivos de Desarrollo Sostenible — ODS 9 y ODS 11.*

---

**Nota metodológica:** las cifras del 27F varían levemente entre fuentes según el criterio de clasificación de daño empleado (viviendas dañadas, con daño severo o destruidas). Se recomienda citar la fuente específica junto a cada cifra en el informe final de la Unidad 4.
