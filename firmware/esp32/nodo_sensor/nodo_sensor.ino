/* ============================================================
   NODO SENSOR  (maestro Bluetooth)
   Sistema IoT de monitoreo de salud estructural

   Asignatura : TI3042 - Aplicaciones Moviles para IoT
   Unidad     : 1 - Evaluacion Sumativa 1
   Hardware   : ESP32 DevKit + MPU6050 + boton

   ROL DE ESTE NODO
   Se instala sobre la estructura monitoreada. Adquiere las
   vibraciones, calcula la frecuencia natural mediante FFT,
   la compara contra la linea base y transmite el resultado
   por Bluetooth al NODO BASE, que es quien alerta al usuario.

   POR QUE DOS NODOS
   En una implementacion real el sensor debe ir fijado al
   elemento estructural (una viga, un muro), mientras que la
   alerta debe producirse donde se encuentra la persona. La
   separacion fisica entre medicion y notificacion es la razon
   por la que el sistema requiere comunicacion inalambrica.

   ARQUITECTURA BLUETOOTH
   Este nodo actua como MAESTRO: inicia la conexion buscando al
   nodo base por su nombre. El nodo base actua como esclavo y
   permanece a la espera.
   ============================================================ */

#include <Wire.h>
#include <Adafruit_MPU6050.h>
#include <Adafruit_Sensor.h>
#include <arduinoFFT.h>
#include <Preferences.h>
#include <BluetoothSerial.h>   // Bluetooth Classic (SPP) del ESP32

// ---------------- MODO DE OPERACION ----------------
// true  : genera una senal senoidal interna de frecuencia conocida.
//         Sirve para validar el algoritmo sin hardware (senal patron).
// false : lee las vibraciones reales del MPU6050.
const bool MODO_SIMULACION = true;

// ---------------- IDENTIFICACION BLUETOOTH ----------------
// Nombre propio de este dispositivo y nombre del nodo al que
// se debe conectar. Deben coincidir con los del nodo base.
const char* NOMBRE_PROPIO = "NodoSensor";
const char* NOMBRE_DESTINO = "NodoBase";

// ---------------- IDENTIFICACION DEL NODO ----------------
// Con varias estructuras monitoreadas, cada nodo sensor lleva un
// ID unico que viaja en cada trama. Cambiarlo en cada placa.
const char* ID_NODO = "NODO-01";

// ---------------- CONFIGURACION DE PINES ----------------
// El MPU6050 usa I2C: SDA=GPIO21 y SCL=GPIO22 por defecto.
#define PIN_BOTON   4    // Registra la linea base
#define PIN_POTE   34    // Solo en simulacion: ajusta la frecuencia
#define PIN_POTE_AMP 35   // Solo en simulacion: ajusta la amplitud (sismo)
#define PIN_LED_EST 2    // LED interno: indica estado del enlace

// ---------------- PARAMETROS DE MUESTREO ----------------
// SAMPLES debe ser potencia de 2: la FFT lo exige por como divide
// la senal recursivamente en mitades.
const uint16_t SAMPLES = 512;

// Frecuencia de muestreo en Hz. Por el teorema de Nyquist solo
// podemos observar frecuencias menores a Fs/2 = 100 Hz.
const double SAMPLING_FREQUENCY = 200.0;

// Resolucion en frecuencia = Fs / SAMPLES = 200/512 = 0.39 Hz
// Duracion de la ventana     = SAMPLES / Fs = 2.56 segundos

// Periodo entre muestras en microsegundos: 1/200 Hz = 5000 us
const unsigned long PERIODO_MUESTREO = round(1000000.0 / SAMPLING_FREQUENCY);

// Caida porcentual de frecuencia que dispara la alerta.
// La literatura de Structural Health Monitoring usa el orden del 5%.
const double UMBRAL_CAIDA = 0.05;

// Amplitud minima del pico para considerar valida la medicion.
// Evita reportar frecuencias calculadas a partir de puro ruido.
const double AMPLITUD_MINIMA = 50.0;

// Amplitud sobre la cual la vibracion se considera fuerte (sismo).
// En reposo la senal patron entrega un pico cercano a 133000 con
// 512 muestras; el umbral es ~1,7 veces ese valor. En modo sensor
// real debe recalibrarse segun la estructura.
const double AMPLITUD_SISMO = 230000.0;

// Segundo potenciometro para demostrar el estado AMARILLO. Dejar en
// false si no esta conectado: un pin al aire lee ruido.
const bool USAR_POTE_AMPLITUD = false;

// Tiempo que hay que mantener el pulsador para recalibrar.
const unsigned long TIEMPO_RECALIBRAR = 3000;  // milisegundos

// Rango de frecuencias simuladas (solo en MODO_SIMULACION)
const double FREC_SIM_MIN = 5.0;
const double FREC_SIM_MAX = 40.0;

// ---------------- OBJETOS GLOBALES ----------------
Adafruit_MPU6050 mpu;
Preferences memoria;
BluetoothSerial SerialBT;

// Arreglos de trabajo de la FFT.
// Ocupan 512 * 8 bytes * 2 = 8 KB de RAM. En un Arduino Uno (2 KB)
// esto seria imposible; en el ESP32 (520 KB) no hay problema.
double vReal[SAMPLES];
double vImag[SAMPLES];

ArduinoFFT<double> FFT = ArduinoFFT<double>(vReal, vImag, SAMPLES, SAMPLING_FREQUENCY);

double frecuenciaBase = 0.0;   // Linea base de la estructura sana
bool   enlaceActivo   = false; // Estado del enlace Bluetooth
double ultimaAmplitud = 0.0;   // Amplitud del pico de la ultima medicion

// ============================================================
//  SETUP
// ============================================================
void setup() {
  Serial.begin(115200);
  delay(1000);

  pinMode(PIN_BOTON, INPUT_PULLUP);  // Boton a GND, sin resistencia externa
  pinMode(PIN_LED_EST, OUTPUT);

  Serial.println("=== NODO SENSOR ===");

  // --- Inicializacion del sensor ---
  if (MODO_SIMULACION) {
    Serial.println("Modo simulacion: senal generada internamente.");
  } else {
    if (!mpu.begin()) {
      Serial.println("ERROR: MPU6050 no detectado. Revisar SDA/SCL.");
      while (1) delay(100);
    }
    mpu.setAccelerometerRange(MPU6050_RANGE_2_G);   // Rango mas sensible
    mpu.setFilterBandwidth(MPU6050_BAND_44_HZ);     // Filtro pasa-bajos interno
    Wire.setClock(400000);                          // I2C rapido
    Serial.println("Modo sensor real: MPU6050 operativo.");
  }

  // --- Recuperacion de la linea base desde memoria no volatil ---
  memoria.begin("estructura", false);
  frecuenciaBase = memoria.getDouble("fbase", 0.0);

  if (frecuenciaBase > 0) {
    Serial.print("Linea base recuperada: ");
    Serial.print(frecuenciaBase, 2);
    Serial.println(" Hz");
  } else {
    Serial.println("Sin linea base. Presionar el boton para calibrar.");
  }

  // --- Inicio del Bluetooth en modo maestro ---
  // El parametro true indica que este dispositivo inicia la
  // conexion (maestro) en lugar de esperarla (esclavo).
  SerialBT.begin(NOMBRE_PROPIO, true);
  Serial.println("Bluetooth iniciado como maestro.");

  conectarNodoBase();
}

// ============================================================
//  LOOP
// ============================================================
void loop() {

  // Si se perdio el enlace, se intenta reconectar antes de medir.
  if (!SerialBT.connected()) {
    enlaceActivo = false;
    digitalWrite(PIN_LED_EST, LOW);
    Serial.println("Enlace perdido. Reintentando...");
    conectarNodoBase();
    return;
  }

  // --- 1. ADQUISICION ---
  if (MODO_SIMULACION) {
    generarSenalSimulada();
  } else {
    capturarMuestras();
  }

  // --- 2. PROCESAMIENTO ---
  double amplitudPico = 0.0;
  double frecuencia = calcularFrecuencia(&amplitudPico);
  ultimaAmplitud = amplitudPico;

  // --- 3. EVALUACION Y ENVIO ---
  if (amplitudPico < AMPLITUD_MINIMA) {
    Serial.println("Sin excitacion suficiente para evaluar.");
    enviarTrama("REPOSO", 0.0, frecuenciaBase, 0.0);
  } else {
    Serial.print("Frecuencia dominante: ");
    Serial.print(frecuencia, 2);
    Serial.print(" Hz | Amplitud: ");
    Serial.println(amplitudPico, 1);

    // Calibracion por boton: fija la frecuencia actual como referencia
    if (botonMantenido()) {
      calibrar(frecuencia);
      enviarTrama("CALIBRADO", frecuencia, frecuenciaBase, 0.0);
    }
    else if (frecuenciaBase > 0) {
      // Caida relativa respecto de la linea base
      double caida = (frecuenciaBase - frecuencia) / frecuenciaBase;

      Serial.print("Variacion respecto de linea base: ");
      Serial.print(caida * 100.0, 1);
      Serial.println(" %");

      // Prioridad: ROJO (dano) > AMARILLO (sismo) > VERDE.
      if (caida > UMBRAL_CAIDA) {
        Serial.println("*** ALERTA: posible perdida de rigidez ***");
        enviarTrama("ALERTA", frecuencia, frecuenciaBase, caida * 100.0);
      } else if (amplitudPico > AMPLITUD_SISMO) {
        Serial.println("!!! Vibracion fuerte: posible sismo !!!");
        enviarTrama("VIBRACION", frecuencia, frecuenciaBase, caida * 100.0);
      } else {
        Serial.println("Estructura dentro de parametros.");
        enviarTrama("NORMAL", frecuencia, frecuenciaBase, caida * 100.0);
      }
    } else {
      enviarTrama("SIN_BASE", frecuencia, 0.0, 0.0);
    }
  }

  Serial.println("---------------------------------------");
  delay(1000);
}

// ============================================================
//  CONEXION CON EL NODO BASE
// ============================================================
void conectarNodoBase() {
  Serial.print("Conectando con ");
  Serial.print(NOMBRE_DESTINO);
  Serial.println("...");

  // connect() busca el dispositivo por nombre y establece el
  // enlace. Devuelve true si la conexion fue exitosa.
  if (SerialBT.connect(NOMBRE_DESTINO)) {
    enlaceActivo = true;
    digitalWrite(PIN_LED_EST, HIGH);   // LED fijo = enlace establecido
    Serial.println("Enlace Bluetooth establecido.");
  } else {
    enlaceActivo = false;
    digitalWrite(PIN_LED_EST, LOW);
    Serial.println("No se pudo conectar. Nuevo intento en 3 s.");
    delay(3000);
  }
}

// ============================================================
//  ENVIO DE LA TRAMA DE DATOS
// ============================================================
void enviarTrama(const char* estado, double frecuencia,
                 double base, double variacion) {

  // Formato de trama delimitado por punto y coma:
  //   ID;ESTADO;frecuencia;linea_base;variacion;amplitud
  // El ID permite que el receptor (o la app) distinga entre
  // varias estructuras monitoreadas al mismo tiempo.
  // Se elige un formato de texto plano y delimitado porque es
  // legible, facil de depurar y trivial de dividir en el nodo
  // receptor, sin requerir librerias de serializacion.
  String trama = String(ID_NODO) + ";" +
                 String(estado) + ";" +
                 String(frecuencia, 2) + ";" +
                 String(base, 2) + ";" +
                 String(variacion, 1) + ";" +
                 String(ultimaAmplitud, 0);

  SerialBT.println(trama);   // println agrega el salto de linea
                             // que el receptor usa como fin de trama

  Serial.print("[TX] ");
  Serial.println(trama);
}

// ============================================================
//  GENERACION DE SENAL PATRON (solo en MODO_SIMULACION)
// ============================================================
void generarSenalSimulada() {

  // El potenciometro representa la rigidez de la estructura.
  // Se mapea su lectura (0-4095, ADC de 12 bits del ESP32) al
  // rango de frecuencias que se desea simular.
  int lectura = analogRead(PIN_POTE);
  double frecuenciaSimulada = FREC_SIM_MIN +
      (lectura / 4095.0) * (FREC_SIM_MAX - FREC_SIM_MIN);

  // Factor de amplitud: 1x en reposo, hasta 3x con el segundo
  // potenciometro (simula la intensidad de un sismo).
  double factorAmp = 1.0;
  if (USAR_POTE_AMPLITUD) factorAmp = 1.0 + 2.0 * (analogRead(PIN_POTE_AMP) / 4095.0);

  Serial.print("[SIM] Frecuencia inyectada: ");
  Serial.print(frecuenciaSimulada, 2);
  Serial.println(" Hz");

  for (uint16_t i = 0; i < SAMPLES; i++) {
    double t = (double)i / SAMPLING_FREQUENCY;
    double senal = 1000.0 * factorAmp * sin(2.0 * PI * frecuenciaSimulada * t);
    double ruido = random(-80, 80);
    vReal[i] = senal + ruido;
    vImag[i] = 0.0;
  }

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

  // Transformada: del dominio del tiempo al de la frecuencia.
  FFT.compute(FFTDirection::Forward);

  // Magnitud de cada componente compleja: sqrt(real^2 + imag^2).
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
//  PULSADOR PROTEGIDO
// ============================================================
// Solo recalibra si el pulsador se mantiene TIEMPO_RECALIBRAR ms.
// Evita que un toque registre como sana una estructura danada.
bool botonMantenido() {
  if (digitalRead(PIN_BOTON) == HIGH) return false;
  Serial.println("Mantener el pulsador 3 s para recalibrar...");
  unsigned long inicio = millis();
  while (digitalRead(PIN_BOTON) == LOW) {
    if (millis() - inicio >= TIEMPO_RECALIBRAR) return true;
    delay(10);
  }
  Serial.println("Recalibracion cancelada: pulsador soltado antes de tiempo.");
  return false;
}

// ============================================================
//  CALIBRACION: registra la linea base de la estructura sana
// ============================================================
void calibrar(double frecuencia) {
  frecuenciaBase = frecuencia;
  memoria.putDouble("fbase", frecuenciaBase);   // Guarda en NVS

  Serial.print(">>> LINEA BASE REGISTRADA: ");
  Serial.print(frecuenciaBase, 2);
  Serial.println(" Hz");
}
