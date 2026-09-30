/* ============================================================
   SISTEMA IoT DE MONITOREO DE SALUD ESTRUCTURAL
   VERSION ARDUINO UNO (un solo nodo, sin Bluetooth)

   Asignatura : TI3042 - Aplicaciones Moviles para IoT
   Hardware   : Arduino Uno + MPU6050 + potenciometro + boton
                + LED verde + LED rojo + buzzer

   PRINCIPIO: toda estructura vibra a una frecuencia natural que
   depende de su masa y su rigidez. Si la rigidez disminuye por
   dano, la frecuencia natural baja. El sistema calcula esa
   frecuencia con una FFT y la compara contra una linea base.

   DIFERENCIAS RESPECTO DE LA VERSION ESP32
   - Ventana de 128 muestras en lugar de 512: los 2 KB de RAM del
     ATmega328P no alcanzan para mas. Resolucion: 200/128 = 1,56 Hz.
   - La linea base se guarda en la EEPROM interna del Uno.
   - El MPU6050 se lee directamente por I2C con la libreria Wire,
     sin librerias externas, para ahorrar memoria.
   - Sin Bluetooth: el Uno no lo trae integrado.

   CONEXIONES
     MPU6050 VCC -> 5V        MPU6050 GND -> GND
     MPU6050 SDA -> A4        MPU6050 SCL -> A5
     Potenciometro: extremos a 5V y GND, centro -> A0
     Boton: una pata -> D2, pata diagonal -> GND
     LED verde: D6 -> resistencia 220 -> pata larga, pata corta -> GND
     LED rojo : D7 -> resistencia 220 -> pata larga, pata corta -> GND
     Buzzer: + -> D8, - -> GND

   Monitor serie a 115200 baudios.
   ============================================================ */

#include <Wire.h>        // Comunicacion I2C (incluida en el IDE)
#include <EEPROM.h>      // Memoria no volatil del Uno (incluida)
#include <arduinoFFT.h>  // Transformada Rapida de Fourier

// ---------------- MODO DE OPERACION ----------------
// true  : el potenciometro genera una senal de frecuencia conocida
//         (validacion con senal patron). Es el modo de la demo.
// false : se leen las vibraciones reales del MPU6050.
const bool MODO_SIMULACION = true;

// ---------------- IDENTIFICACION DEL NODO ----------------
// Cada prototipo lleva un ID unico. Con varias estructuras
// monitoreadas, la app usa este ID para saber de donde viene
// cada medicion. Cambiarlo en cada placa: NODO-01, NODO-02...
const char ID_NODO[] = "NODO-01";

// ---------------- SEGUNDO POTENCIOMETRO (opcional) ----------------
// Solo en simulacion: un segundo potenciometro en A1 escala la
// amplitud de la vibracion para demostrar el estado AMARILLO
// (sismo). Dejar en false si no esta conectado: un pin analogico
// al aire lee ruido y activaria el amarillo al azar.
const bool USAR_POTE_AMPLITUD = true;
#define PIN_POTE_AMP  A1

// ---------------- PINES ----------------
#define PIN_POTE      A0
#define PIN_BOTON      2
#define PIN_LED_VERDE  6
#define PIN_LED_ROJO   7
#define PIN_BUZZER     8

// ---------------- MPU6050 ----------------
const uint8_t MPU_DIR    = 0x68;  // Direccion I2C del sensor
const uint8_t REG_PWR    = 0x6B;  // Registro de energia (despertar)
const uint8_t REG_ACEL_X = 0x3B;  // Primer byte del eje X del acelerometro

// ---------------- PARAMETROS DE MUESTREO ----------------
// Debe ser potencia de 2: la FFT divide la senal en mitades.
const uint16_t SAMPLES = 128;

// Por Nyquist se observan frecuencias menores a Fs/2 = 100 Hz.
const float SAMPLING_FREQUENCY = 200.0;

// Periodo entre muestras: 1 / 200 Hz = 5000 microsegundos
const unsigned long PERIODO_MUESTREO = 5000;

// Caida que dispara la alerta. En el Uno se usa 10% y no 5%:
// con resolucion de 1,56 Hz, un solo "escalon" de la FFT a 20 Hz
// ya representa casi un 8%, y un umbral de 5% daria falsas alarmas.
const float UMBRAL_CAIDA = 0.10;

// Amplitud minima del pico para considerar valida la medicion.
const float AMPLITUD_MINIMA = 50.0;

// Amplitud sobre la cual la vibracion se considera fuerte (sismo).
// En reposo la senal patron entrega un pico cercano a 35000; este
// umbral corresponde a ~1,7 veces ese valor. En modo sensor real
// debe recalibrarse segun la estructura.
const float AMPLITUD_SISMO = 60000.0;

// Rango de la frecuencia simulada con el potenciometro
const float FREC_SIM_MIN = 5.0;
const float FREC_SIM_MAX = 40.0;

// Posicion en la EEPROM donde se guarda la linea base
const int DIR_EEPROM = 0;

// Tiempo que hay que mantener el pulsador para recalibrar.
// Evita que un toque accidental (o malintencionado) registre como
// "sana" una estructura ya danada y borre la alerta.
const unsigned long TIEMPO_RECALIBRAR = 3000;  // milisegundos

// ---------------- VARIABLES GLOBALES ----------------
// En el Uno un float ocupa 4 bytes: 128 x 4 x 2 = 1 KB de RAM,
// la mitad de toda la memoria disponible.
float vReal[SAMPLES];
float vImag[SAMPLES];

ArduinoFFT<float> FFT = ArduinoFFT<float>(vReal, vImag, SAMPLES, SAMPLING_FREQUENCY);

float frecuenciaBase = 0.0;

// ============================================================
//  SETUP
// ============================================================
void setup() {
  Serial.begin(115200);

  pinMode(PIN_LED_VERDE, OUTPUT);
  pinMode(PIN_LED_ROJO, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_BOTON, INPUT_PULLUP);  // Resistencia interna: sin apretar = HIGH

  // F("...") guarda el texto en la memoria de programa y no en la
  // RAM. En el Uno es indispensable para no quedarse sin memoria.
  Serial.println(F("=== MONITOR ESTRUCTURAL - ARDUINO UNO ==="));

  if (MODO_SIMULACION) {
    Serial.println(F("Modo simulacion: el potenciometro fija la frecuencia."));
  } else {
    iniciarMPU();
    Serial.println(F("Modo sensor real: leyendo MPU6050."));
  }

  // Recuperar la linea base desde la EEPROM.
  // Una EEPROM nunca escrita devuelve un valor invalido, por eso se
  // valida que este en un rango razonable antes de usarla.
  EEPROM.get(DIR_EEPROM, frecuenciaBase);
  if (isnan(frecuenciaBase) || frecuenciaBase <= 0 || frecuenciaBase > 100) {
    frecuenciaBase = 0.0;
    Serial.println(F("Sin linea base. Presionar el boton para calibrar."));
  } else {
    Serial.print(F("Linea base recuperada: "));
    Serial.print(frecuenciaBase, 2);
    Serial.println(F(" Hz"));
  }

  Serial.println(F("---------------------------------------"));
}

// ============================================================
//  LOOP
// ============================================================
void loop() {

  // 1. ADQUISICION
  if (MODO_SIMULACION) generarSenalSimulada();
  else                 capturarMuestras();

  // 2. PROCESAMIENTO
  float amplitud = 0.0;
  float frecuencia = calcularFrecuencia(&amplitud);

  // 3. EVALUACION
  if (amplitud < AMPLITUD_MINIMA) {
    Serial.println(F("Sin excitacion suficiente para evaluar."));
    enviarTrama("REPOSO", 0.0, 0.0, amplitud);
    estadoNormal();
  } else {
    Serial.print(F("Frecuencia dominante: "));
    Serial.print(frecuencia, 2);
    Serial.print(F(" Hz | Amplitud: "));
    Serial.println(amplitud, 0);

    if (botonMantenido()) {
      calibrar(frecuencia);
      enviarTrama("CALIBRADO", frecuencia, 0.0, amplitud);
    } else if (frecuenciaBase > 0) {
      evaluarEstado(frecuencia, amplitud);
    } else {
      enviarTrama("SIN_BASE", frecuencia, 0.0, amplitud);
    }
  }

  Serial.println(F("---------------------------------------"));
  delay(1000);
}

// ============================================================
//  INICIALIZACION DEL MPU6050 POR I2C
// ============================================================
void iniciarMPU() {
  Wire.begin();
  // El sensor arranca en modo reposo. Escribir 0 en el registro
  // de energia lo despierta.
  Wire.beginTransmission(MPU_DIR);
  Wire.write(REG_PWR);
  Wire.write(0);
  Wire.endTransmission();
}

// Lee el eje X del acelerometro: dos bytes (alto y bajo) que se
// combinan en un entero de 16 bits con signo.
int16_t leerAceleracionX() {
  Wire.beginTransmission(MPU_DIR);
  Wire.write(REG_ACEL_X);
  Wire.endTransmission(false);
  Wire.requestFrom(MPU_DIR, (uint8_t)2);
  int16_t alto = Wire.read();
  int16_t bajo = Wire.read();
  return (alto << 8) | bajo;
}

// ============================================================
//  SENAL PATRON (modo simulacion)
// ============================================================
void generarSenalSimulada() {
  // ADC del Uno: 10 bits, valores de 0 a 1023.
  int lectura = analogRead(PIN_POTE);
  float frecuenciaSimulada = FREC_SIM_MIN +
      (lectura / 1023.0) * (FREC_SIM_MAX - FREC_SIM_MIN);

  // Factor de amplitud: 1x en reposo, hasta 3x con el segundo
  // potenciometro (simula la intensidad de un sismo).
  float factorAmp = 1.0;
  if (USAR_POTE_AMPLITUD) factorAmp = 1.0 + 2.0 * (analogRead(PIN_POTE_AMP) / 1023.0);

  Serial.print(F("[SIM] Frecuencia inyectada: "));
  Serial.print(frecuenciaSimulada, 2);
  Serial.println(F(" Hz"));

  for (uint16_t i = 0; i < SAMPLES; i++) {
    float t = (float)i / SAMPLING_FREQUENCY;
    vReal[i] = 1000.0 * factorAmp * sin(2.0 * PI * frecuenciaSimulada * t) + random(-80, 80);
    vImag[i] = 0.0;
  }
  eliminarComponenteContinua();
}

// ============================================================
//  CAPTURA REAL A FRECUENCIA CONSTANTE
// ============================================================
void capturarMuestras() {
  unsigned long tSiguiente = micros();
  for (uint16_t i = 0; i < SAMPLES; i++) {
    vReal[i] = leerAceleracionX();
    vImag[i] = 0.0;
    // No se usa delay(): acumularia el tiempo de lectura y las
    // muestras dejarian de estar equiespaciadas.
    tSiguiente += PERIODO_MUESTREO;
    while (micros() < tSiguiente) { }
  }
  eliminarComponenteContinua();
}

// ============================================================
//  ELIMINACION DE LA COMPONENTE CONTINUA
// ============================================================
void eliminarComponenteContinua() {
  // La gravedad es constante y generaria un pico enorme en 0 Hz.
  float media = 0.0;
  for (uint16_t i = 0; i < SAMPLES; i++) media += vReal[i];
  media /= SAMPLES;
  for (uint16_t i = 0; i < SAMPLES; i++) vReal[i] -= media;
}

// ============================================================
//  FFT Y BUSQUEDA DEL PICO
// ============================================================
float calcularFrecuencia(float *amplitud) {
  FFT.windowing(FFTWindow::Hamming, FFTDirection::Forward);  // Evita fuga espectral
  FFT.compute(FFTDirection::Forward);                        // Tiempo -> frecuencia
  FFT.complexToMagnitude();                                  // Espectro de amplitudes

  uint16_t indicePico = 0;
  float maximo = 0.0;
  for (uint16_t i = 3; i < SAMPLES / 2; i++) {   // Se saltan los bins cercanos a 0 Hz
    if (vReal[i] > maximo) {
      maximo = vReal[i];
      indicePico = i;
    }
  }
  *amplitud = maximo;
  return (indicePico * SAMPLING_FREQUENCY) / SAMPLES;   // Bin -> Hz
}

// ============================================================
//  PULSADOR PROTEGIDO
// ============================================================
// Devuelve true solo si el pulsador se mantiene apretado durante
// TIEMPO_RECALIBRAR. Un toque corto se ignora.
bool botonMantenido() {
  if (digitalRead(PIN_BOTON) == HIGH) return false;
  Serial.println(F("Mantener el pulsador 3 s para recalibrar..."));
  unsigned long inicio = millis();
  while (digitalRead(PIN_BOTON) == LOW) {
    if (millis() - inicio >= TIEMPO_RECALIBRAR) return true;
  }
  Serial.println(F("Recalibracion cancelada: pulsador soltado antes de tiempo."));
  return false;
}

// ============================================================
//  CALIBRACION
// ============================================================
void calibrar(float frecuencia) {
  frecuenciaBase = frecuencia;
  EEPROM.put(DIR_EEPROM, frecuenciaBase);   // Persiste ante cortes de energia

  Serial.print(F(">>> LINEA BASE REGISTRADA: "));
  Serial.print(frecuenciaBase, 2);
  Serial.println(F(" Hz"));

  for (int i = 0; i < 3; i++) {
    tone(PIN_BUZZER, 1500, 100);
    digitalWrite(PIN_LED_VERDE, HIGH);
    delay(150);
    digitalWrite(PIN_LED_VERDE, LOW);
    delay(150);
  }
}

// ============================================================
//  EVALUACION Y ACTUADORES
// ============================================================
void evaluarEstado(float frecuencia, float amplitud) {
  float caida = (frecuenciaBase - frecuencia) / frecuenciaBase;

  Serial.print(F("Variacion respecto de linea base: "));
  Serial.print(caida * 100.0, 1);
  Serial.println(F(" %"));

  // Prioridad de los estados: ROJO > AMARILLO > VERDE.
  // ROJO: la frecuencia cayo (perdio rigidez) -> posible dano.
  // AMARILLO: la estructura vibra fuerte pero su frecuencia se
  //   mantiene -> esta siendo sacudida (sismo), no hay dano aun.
  if (caida > UMBRAL_CAIDA) {
    Serial.println(F("*** ALERTA: posible perdida de rigidez ***"));
    enviarTrama("ALERTA", frecuencia, caida * 100.0, amplitud);
    estadoAlerta();
  } else if (amplitud > AMPLITUD_SISMO) {
    Serial.println(F("!!! Vibracion fuerte: posible sismo !!!"));
    enviarTrama("VIBRACION", frecuencia, caida * 100.0, amplitud);
    estadoVibracion();
  } else {
    Serial.println(F("Estructura dentro de parametros."));
    enviarTrama("NORMAL", frecuencia, caida * 100.0, amplitud);
    estadoNormal();
  }
}

// ============================================================
//  TRAMA PARA LA APP
// ============================================================
// Formato: ID;ESTADO;frecuencia;linea_base;variacion;amplitud
// El ID permite que la app distinga entre varias estructuras.
// Hoy sale por el puerto serie; con un modulo HC-05 en TX/RX
// la misma linea llega por Bluetooth sin cambiar el formato.
void enviarTrama(const char* estado, float frecuencia, float variacion, float amplitud) {
  Serial.print(F("TRAMA:"));
  Serial.print(ID_NODO);           Serial.print(';');
  Serial.print(estado);            Serial.print(';');
  Serial.print(frecuencia, 2);     Serial.print(';');
  Serial.print(frecuenciaBase, 2); Serial.print(';');
  Serial.print(variacion, 1);      Serial.print(';');
  Serial.println(amplitud, 0);
}

void estadoNormal() {
  digitalWrite(PIN_LED_VERDE, HIGH);
  digitalWrite(PIN_LED_ROJO, LOW);
  noTone(PIN_BUZZER);
}

// Amarillo con dos LEDs: verde y rojo encendidos juntos, mas un
// pitido corto. Se distingue de la alerta (solo rojo, pitido largo).
void estadoVibracion() {
  digitalWrite(PIN_LED_VERDE, HIGH);
  digitalWrite(PIN_LED_ROJO, HIGH);
  tone(PIN_BUZZER, 1200, 80);
}

void estadoAlerta() {
  digitalWrite(PIN_LED_VERDE, LOW);
  digitalWrite(PIN_LED_ROJO, HIGH);
  tone(PIN_BUZZER, 2000, 300);
}
