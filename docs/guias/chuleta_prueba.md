# Chuleta para la prueba — Monitor Estructural

**Wokwi:** https://wokwi.com/projects/474167699552138241

---

## El proyecto en 30 segundos

> Toda estructura vibra a una frecuencia natural que depende de su masa y su rigidez. Cuando se daña, pierde rigidez y esa frecuencia **baja**, aunque el daño todavía no se vea. Nuestro sistema mide la vibración con un acelerómetro, calcula la frecuencia con una FFT en un ESP32, la compara con la de la estructura sana, y si cae más de un 5% activa una alerta.

**Problema:** en Chile el estado de una estructura solo se revisa por inspección visual, después del daño. El 27F dejó 1,5 millones de viviendas dañadas y no había inspectores para todas.

**ODS:** 11 (meta 11.5, reducir afectados por desastres) y 9 (meta 9.1, infraestructura resiliente).

---

## Preguntas probables

**¿Qué es la frecuencia natural?**
La frecuencia a la que vibra sola una estructura cuando la golpeas. Depende de masa y rigidez. Si baja la rigidez, baja la frecuencia.

**¿Qué hace la FFT?**
Pasa la señal del tiempo a la frecuencia. Me dice qué frecuencias tiene la vibración. La más fuerte (el pico) es la frecuencia natural.

**¿Por qué ESP32 y no Arduino Uno?**
La FFT necesita guardar dos arreglos grandes. El Uno tiene 2 KB de RAM, solo alcanza para 128 muestras y la resolución queda muy gruesa. El ESP32 tiene 520 KB, y además trae Bluetooth y WiFi integrados.

**¿Por qué 512 muestras?**
La FFT exige potencia de 2 porque divide la señal en mitades. Con 512 muestras a 200 Hz tengo resolución de **0,39 Hz** (200 ÷ 512).

**¿Qué es el teorema de Nyquist?**
Hay que muestrear al menos al doble de la frecuencia que quiero ver. A 200 Hz veo hasta 100 Hz.

**¿Por qué no usan `delay()` para muestrear?**
Porque acumula error: el tiempo de leer el sensor se suma al delay y las muestras dejan de estar parejas. La FFT necesita intervalos idénticos. Por eso uso `micros()`.

**¿Por qué restan la media antes de la FFT?**
El acelerómetro también mide la gravedad, que es constante. Si no la resto, aparece un pico gigante en 0 Hz que tapa la frecuencia real.

**¿Para qué la ventana de Hamming?**
Suaviza los bordes de la señal capturada. Sin ella, el corte brusco crea frecuencias falsas (fuga espectral).

**¿Para qué sirve el potenciómetro?**
Simula el cambio de rigidez. Genera una señal de frecuencia conocida para validar que el algoritmo funciona: si inyecto 28,7 Hz y el sistema detecta 28,5 Hz, el error es menor que la resolución.

**¿Qué es I2C?**
Protocolo de comunicación con dos cables: SDA (datos) y SCL (reloj). En el ESP32 son D21 y D22. El MPU6050 responde en la dirección **0x68**.

**¿Por qué el LED lleva resistencia?**
Para limitar la corriente. Sin ella se quema el LED o el pin del ESP32.

**¿Por qué el botón no lleva resistencia?**
Uso `INPUT_PULLUP`, que activa una resistencia interna del ESP32. Sin apretar lee HIGH, apretado lee LOW.

**¿Dónde se guarda la línea base?**
En la memoria no volátil del ESP32, con la librería `Preferences`. No se borra si se corta la luz.

**¿Qué hace `setup()` y `loop()`?**
`setup()` corre una sola vez al encender (configuración). `loop()` se repite para siempre (medir, calcular, evaluar).

---

## Sobre los dos nodos (ES1)

**¿Por qué dos microcontroladores?**
El sensor tiene que ir pegado a la viga, pero la alerta tiene que sonar donde está la persona. Esa separación obliga a la comunicación inalámbrica.

**¿Cómo se comunican?**
Bluetooth Classic, perfil SPP (emula un puerto serial). El nodo sensor es **maestro** (inicia la conexión), el nodo base es **esclavo** (espera).

**¿Qué mandan?**
Una trama de texto: `ESTADO;frecuencia;línea_base;variación`. El salto de línea marca el final.

**¿Por qué Bluetooth y no WiFi?**
No necesita router, consume menos, y Android lo soporta nativo para la app de la Unidad 2.

**¿Qué pasa si se corta el enlace?**
El nodo base lo detecta y parpadean los dos LEDs. No se queda en verde, porque en un sistema de alerta la falta de datos no puede parecer normalidad.

**¿Por qué no está el Bluetooth en Wokwi?**
Wokwi no simula el enlace entre dos ESP32. Por eso la comunicación se demuestra en físico.

---

## Seguridad

**¿Cuál es la brecha más grave?**
Que alguien inyecte una trama "NORMAL" falsa y **oculte** una alerta real. Es peor que leer los datos. Mitigación: código de verificación en cada trama.

**Otras:** comunicación sin cifrado, suplantación del nodo base (mitigación: PIN y MAC), recalibración no autorizada con el botón, manipulación física.

---

## Limitación que hay que reconocer

**¿Y si le ponen peso encima?**
La frecuencia también baja, porque depende de masa y rigidez. El sistema no distingue peso de daño. Los sistemas profesionales lo compensan midiendo varios modos de vibración y la temperatura. Está declarado en el informe.

---

## Pines

| Pieza | Pin |
|---|---|
| MPU6050 SDA / SCL | D21 / D22 |
| Potenciómetro (centro) | D34 |
| Botón | D4 |
| LED verde / rojo | D18 / D19 |
| Buzzer | D23 |

---

## Antes de irte

- [ ] Nombres de la dupla en los dos Word
- [ ] Subir EF1 y ES1 al AAI
- [ ] Apretar **"Enviar tarea"** en cada una (si no, queda en borrador)
- [ ] Preguntar al profe: Bluetooth en Wokwi, y si el informe es la guía ES1
