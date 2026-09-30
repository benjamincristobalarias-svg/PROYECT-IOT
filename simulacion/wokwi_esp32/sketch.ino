/* ============================================================
   SISTEMA IoT DE MONITOREO DE SALUD ESTRUCTURAL
   Deteccion de perdida de rigidez por analisis de frecuencia natural

   Asignatura : TI3042 - Aplicaciones Moviles para IoT
   Unidad     : 1 - Introduccion a IoT y Smart Devices
   Hardware   : ESP32 DevKit + MPU6050 + LEDs + Buzzer
   Simulacion : Wokwi

   PRINCIPIO: toda estructura vibra a una frecuencia natural que
   depende de su masa y su rigidez. Si la rigidez disminuye por
   dano, la frecuencia natural tambien disminuye. El sistema mide
   esa frecuencia y la compara contra una linea base guardada.

   -----------------------------------------------------------
   NOTA SOBRE LOS DOS MODOS DE OPERACION
   -----------------------------------------------------------
   MODO_SIMULACION = true
     El programa genera internamente una senal senoidal de
     frecuencia conocida en lugar de leer el acelerometro.
     Se emplea para VALIDAR EL ALGORITMO: si se inyecta una
     senal de 20 Hz y el sistema reporta 20 Hz, queda demostrado
     que la cadena de adquisicion, la FFT y la deteccion del pico
     funcionan correctamente. Esta tecnica se denomina validacion
     con senal patron y permite verificar el procesamiento antes
     de confiar en datos reales.
     El potenciometro conectado a GPIO34 permite variar la
     frecuencia simulada, representando el cambio de rigidez de
     la estructura.

   MODO_SIMULACION = false
     El programa lee las vibraciones reales del MPU6050 montado
     sobre la estructura fisica.
   ============================================================ */

#include <Wire.h>              // Comunicacion I2C con el sensor
#include <Adafruit_MPU6050.h>  // Driver del acelerometro
#include <Adafruit_Sensor.h>   // Capa comun de sensores Adafruit
#include <arduinoFFT.h>        // Transformada Rapida de Fourier
#include <Preferences.h>       // Memoria no volatil del ESP32 (NVS)

// ---------------- MODO DE OPERACION ----------------
const bool MODO_SIMULACION = true;

// ---------------- CONFIGURACION DE PINES ----------------
// El MPU6050 usa I2C: SDA=GPIO21 y SCL=GPIO22 por defecto en ESP32.
#define PIN_LED_VERDE   18   // Estado normal
#define PIN_LED_ROJO    19   // Estado de alerta
#define PIN_BUZZER      23   // Alerta sonora
#define PIN_BOTON        4   // Boton para registrar la linea base
#define PIN_POTE        34   // Solo en simulacion: ajusta la frecuencia

// ---------------- PARAMETROS DE MUESTREO ----------------
// SAMPLES debe ser potencia de 2: la FFT lo exige por como divide
// la senal recursivamente en mitades.
const uint16_t SAMPLES = 512;

// Frecuencia de muestreo en Hz. Por el teorema de Nyquist solo
// podemos observar frecuencias menores a Fs/2 = 100 Hz.
const double SAMPLING_FREQUENCY = 200.0;

// Resolucion en frecuencia = Fs / SAMPLES = 200/512 = 0.39 Hz
// Es el minimo cambio de frecuencia que el sistema puede distinguir.
// Duracion de la ventana = SAMPLES / Fs = 2.56 segundos

// Periodo entre muestras en microsegundos: 1/200 Hz = 5000 us
const unsigned long PERIODO_MUESTREO = round(1000000.0 / SAMPLING_FREQUENCY);

// Caida porcentual de frecuencia que dispara la alerta.
// La literatura de Structural Health Monitoring usa el orden del 5%.
const double UMBRAL_CAIDA = 0.05;

// Amplitud minima del pico para considerar la medicion valida.
// Evita que el sistema "detecte" frecuencias a partir de puro ruido
// cuando la estructura esta en reposo.
const double AMPLITUD_MINIMA = 50.0;

// Rango de frecuencias simuladas (solo en MODO_SIMULACION)
const double FREC_SIM_MIN = 5.0;
const double FREC_SIM_MAX = 40.0;

// ---------------- OBJETOS GLOBALES ----------------
Adafruit_MPU6050 mpu;
Preferences memoria;

// Arreglos de trabajo de la FFT.
// vReal guarda la senal; vImag es la parte imaginaria (inicia en 0).
// Ocupan 512 * 8 bytes * 2 = 8 KB de RAM. En un Arduino Uno (2 KB)
// esto seria imposible; en el ESP32 (520 KB) no representa problema.
double vReal[SAMPLES];
double vImag[SAMPLES];

// Objeto FFT (sintaxis de arduinoFFT v2.x)
ArduinoFFT<double> FFT = ArduinoFFT<double>(vReal, vImag, SAMPLES, SAMPLING_FREQUENCY);

double frecuenciaBase = 0.0;  // Linea base de la estructura sana

// ============================================================
//  SETUP - se ejecuta una sola vez al encender o resetear
// ============================================================
void setup() {
  Serial.begin(115200);
  delay(1000);

  pinMode(PIN_LED_VERDE, OUTPUT);
  pinMode(PIN_LED_ROJO, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_BOTON, INPUT_PULLUP);  // Boton a GND, sin resistencia externa

  if (MODO_SIMULACION) {
    Serial.println("=== MODO SIMULACION ===");
    Serial.println("Senal senoidal generada internamente.");
    Serial.println("El potenciometro representa la rigidez de la estructura.");
  } else {
    // Inicializacion del sensor por I2C
    if (!mpu.begin()) {
      Serial.println("ERROR: MPU6050 no detectado. Revisar SDA/SCL.");
      while (1) {
        digitalWrite(PIN_LED_ROJO, !digitalRead(PIN_LED_ROJO));
        delay(200);  // Parpadeo rapido = fallo de hardware
      }
    }
    // Rango del acelerometro. +-2G es el mas sensible: las
    // vibraciones estructurales son pequenas.
    mpu.setAccelerometerRange(MPU6050_RANGE_2_G);

    // Filtro pasa-bajos interno: deja pasar la banda de interes
    // y atenua el ruido de alta frecuencia.
    mpu.setFilterBandwidth(MPU6050_BAND_44_HZ);

    // I2C a 400 kHz para que cada lectura demore menos que el
    // periodo de muestreo y no se pierda la cadencia.
    Wire.setClock(400000);

    Serial.println("=== MODO SENSOR REAL ===");
  }

  // Recuperacion de la linea base guardada en memoria no volatil.
  // Sobrevive a un corte de energia o a un reset.
  memoria.begin("estructura", false);
  frecuenciaBase = memoria.getDouble("fbase", 0.0);

  if (frecuenciaBase > 0) {
    Serial.print("Linea base recuperada: ");
    Serial.print(frecuenciaBase, 2);
    Serial.println(" Hz");
  } else {
    Serial.println("Sin linea base. Presionar el boton para calibrar.");
  }

  Serial.println("Sistema iniciado.");
  Serial.println("---------------------------------------");
}

// ============================================================
//  LOOP - se ejecuta ciclicamente despues de setup()
// ============================================================
void loop() {

  // --- 1. ADQUISICION ---
  if (MODO_SIMULACION) {
    generarSenalSimulada();
  } else {
    capturarMuestras();
  }

  // --- 2. PROCESAMIENTO ---
  double amplitudPico = 0.0;
  double frecuencia = calcularFrecuencia(&amplitudPico);

  // --- 3. EVALUACION ---
  if (amplitudPico < AMPLITUD_MINIMA) {
    // Estructura en reposo: no hay vibracion suficiente para medir.
    Serial.println("Sin excitacion suficiente para evaluar.");
    estadoNormal();
  } else {
    Serial.print("Frecuencia dominante: ");
    Serial.print(frecuencia, 2);
    Serial.print(" Hz | Amplitud: ");
    Serial.println(amplitudPico, 1);

    // Calibracion por boton: fija la frecuencia actual como referencia
    if (digitalRead(PIN_BOTON) == LOW) {
      calibrar(frecuencia);
    }
    else if (frecuenciaBase > 0) {
      evaluarEstado(frecuencia);
    }
  }

  Serial.println("---------------------------------------");
  delay(1000);
}

// ============================================================
//  GENERACION DE SENAL PATRON (solo en MODO_SIMULACION)
// ============================================================
void generarSenalSimulada() {

  // El potenciometro representa la rigidez de la estructura.
  // Se mapea su lectura (0-4095 en el ADC de 12 bits del ESP32)
  // al rango de frecuencias que se desea simular.
  int lectura = analogRead(PIN_POTE);
  double frecuenciaSimulada = FREC_SIM_MIN +
      (lectura / 4095.0) * (FREC_SIM_MAX - FREC_SIM_MIN);

  Serial.print("[SIM] Frecuencia inyectada: ");
  Serial.print(frecuenciaSimulada, 2);
  Serial.println(" Hz");

  // Construccion de la senal: una senoidal de amplitud conocida
  // mas una pequena componente de ruido aleatorio, para representar
  // condiciones mas cercanas a una medicion real.
  for (uint16_t i = 0; i < SAMPLES; i++) {
    double t = (double)i / SAMPLING_FREQUENCY;   // Instante de la muestra
    double senal = 1000.0 * sin(2.0 * PI * frecuenciaSimulada * t);
    double ruido = random(-80, 80);
    vReal[i] = senal + ruido;
    vImag[i] = 0.0;
  }

  // Se aplica la misma eliminacion de componente continua que en
  // el modo real, para que ambos caminos sean equivalentes.
  eliminarComponenteContinua();
}

// ============================================================
//  CAPTURA DE MUESTRAS A FRECUENCIA CONSTANTE (modo real)
// ============================================================
void capturarMuestras() {
  sensors_event_t a, g, temp;
  unsigned long tSiguiente = micros();

  for (uint16_t i = 0; i < SAMPLES; i++) {
    mpu.getEvent(&a, &g, &temp);

    // Se usa el eje X: debe coincidir con la direccion en que
    // la estructura oscila. Ajustar segun el montaje del sensor.
    vReal[i] = a.acceleration.x;
    vImag[i] = 0.0;

    // Espera activa hasta el instante exacto de la proxima muestra.
    // NO se usa delay() porque acumula error: el tiempo de lectura
    // del sensor se sumaria al retardo y el muestreo dejaria de ser
    // uniforme. La FFT asume intervalos identicos entre muestras.
    tSiguiente += PERIODO_MUESTREO;
    while (micros() < tSiguiente) { /* espera */ }
  }

  eliminarComponenteContinua();
}

// ============================================================
//  ELIMINACION DE LA COMPONENTE CONTINUA (DC)
// ============================================================
void eliminarComponenteContinua() {
  // El acelerometro mide tambien la gravedad, que es un valor
  // constante. Sin restarla, la FFT devolveria un pico enorme en
  // 0 Hz que enmascararia la frecuencia natural real.
  double media = 0.0;
  for (uint16_t i = 0; i < SAMPLES; i++) media += vReal[i];
  media /= SAMPLES;
  for (uint16_t i = 0; i < SAMPLES; i++) vReal[i] -= media;
}

// ============================================================
//  CALCULO DE LA FRECUENCIA DOMINANTE MEDIANTE FFT
// ============================================================
double calcularFrecuencia(double *amplitud) {

  // Ventana de Hamming: suaviza los extremos de la senal capturada.
  // Sin ventana, el corte abrupto al inicio y final introduce
  // frecuencias falsas en el espectro (fuga espectral).
  FFT.windowing(FFTWindow::Hamming, FFTDirection::Forward);

  // Transformada: pasa la senal del dominio del tiempo al de la
  // frecuencia. Entrega numeros complejos en vReal y vImag.
  FFT.compute(FFTDirection::Forward);

  // Magnitud de cada componente compleja: sqrt(real^2 + imag^2).
  // El resultado es el espectro de amplitudes.
  FFT.complexToMagnitude();

  // Busqueda del pico dominante, ignorando los primeros bins
  // (frecuencias muy bajas, residuo de la componente DC).
  uint16_t indicePico = 0;
  double maximo = 0.0;
  for (uint16_t i = 3; i < SAMPLES / 2; i++) {
    if (vReal[i] > maximo) {
      maximo = vReal[i];
      indicePico = i;
    }
  }

  *amplitud = maximo;

  // Conversion de indice de bin a frecuencia en Hz.
  // Cada bin representa (Fs / SAMPLES) Hz.
  return (indicePico * SAMPLING_FREQUENCY) / SAMPLES;
}

// ============================================================
//  CALIBRACION: registra la linea base de la estructura sana
// ============================================================
void calibrar(double frecuencia) {
  frecuenciaBase = frecuencia;
  memoria.putDouble("fbase", frecuenciaBase);  // Guarda en NVS

  Serial.print(">>> LINEA BASE REGISTRADA: ");
  Serial.print(frecuenciaBase, 2);
  Serial.println(" Hz");

  // Confirmacion audible y visual
  for (int i = 0; i < 3; i++) {
    tone(PIN_BUZZER, 1500, 100);
    digitalWrite(PIN_LED_VERDE, HIGH);
    delay(150);
    digitalWrite(PIN_LED_VERDE, LOW);
    delay(150);
  }
}

// ============================================================
//  EVALUACION DEL ESTADO ESTRUCTURAL
// ============================================================
void evaluarEstado(double frecuencia) {

  // Caida relativa respecto de la linea base
  double caida = (frecuenciaBase - frecuencia) / frecuenciaBase;

  Serial.print("Variacion respecto de linea base: ");
  Serial.print(caida * 100.0, 1);
  Serial.println(" %");

  if (caida > UMBRAL_CAIDA) {
    Serial.println("*** ALERTA: posible perdida de rigidez ***");
    estadoAlerta();
  } else {
    Serial.println("Estructura dentro de parametros.");
    estadoNormal();
  }
}

void estadoNormal() {
  digitalWrite(PIN_LED_VERDE, HIGH);
  digitalWrite(PIN_LED_ROJO, LOW);
  noTone(PIN_BUZZER);
}

void estadoAlerta() {
  digitalWrite(PIN_LED_VERDE, LOW);
  digitalWrite(PIN_LED_ROJO, HIGH);
  tone(PIN_BUZZER, 2000, 300);
}
