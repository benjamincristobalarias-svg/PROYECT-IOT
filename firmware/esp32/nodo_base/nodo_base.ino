/* ============================================================
   NODO BASE  (esclavo Bluetooth)
   Sistema IoT de monitoreo de salud estructural

   Asignatura : TI3042 - Aplicaciones Moviles para IoT
   Unidad     : 1 - Evaluacion Sumativa 1
   Hardware   : ESP32 DevKit + LED verde + LED rojo + buzzer

   ROL DE ESTE NODO
   Se ubica donde se encuentra el usuario. No mide nada: recibe
   por Bluetooth el resultado de la evaluacion realizada por el
   NODO SENSOR y acciona los actuadores correspondientes.

   ARQUITECTURA BLUETOOTH
   Este nodo actua como ESCLAVO: publica su nombre y permanece a
   la espera de que el nodo sensor inicie la conexion.

   PROTOCOLO DE TRAMA
   Recibe lineas de texto con el formato:
       ID;ESTADO;frecuencia;linea_base;variacion;amplitud
   El salto de linea marca el fin de cada trama.
   Estados posibles: NORMAL, VIBRACION, ALERTA, REPOSO, CALIBRADO, SIN_BASE
   ============================================================ */

#include <BluetoothSerial.h>

// ---------------- IDENTIFICACION BLUETOOTH ----------------
// Debe coincidir con NOMBRE_DESTINO del nodo sensor.
const char* NOMBRE_PROPIO = "NodoBase";

// ---------------- CONFIGURACION DE PINES ----------------
#define PIN_LED_VERDE  18   // Estado normal
#define PIN_LED_ROJO   19   // Estado de alerta
#define PIN_BUZZER     23   // Alerta sonora
#define PIN_LED_EST     2   // LED interno: indica estado del enlace

// ---------------- PARAMETROS ----------------
// Si no llega ninguna trama en este tiempo se considera que el
// enlace se perdio. Un sistema de seguridad no puede quedarse en
// estado "normal" solo porque dejo de recibir datos: la ausencia
// de informacion es en si misma una condicion a senalizar.
const unsigned long TIMEOUT_ENLACE = 8000;   // milisegundos

// ---------------- OBJETOS GLOBALES ----------------
BluetoothSerial SerialBT;

unsigned long tUltimaTrama = 0;
bool enlaceVivo = false;

// ============================================================
//  SETUP
// ============================================================
void setup() {
  Serial.begin(115200);
  delay(500);

  pinMode(PIN_LED_VERDE, OUTPUT);
  pinMode(PIN_LED_ROJO, OUTPUT);
  pinMode(PIN_BUZZER, OUTPUT);
  pinMode(PIN_LED_EST, OUTPUT);

  Serial.println("=== NODO BASE ===");

  // Inicio del Bluetooth en modo esclavo.
  // Sin el segundo parametro, begin() configura el dispositivo
  // como esclavo: publica su nombre y espera la conexion.
  SerialBT.begin(NOMBRE_PROPIO);

  Serial.print("Bluetooth iniciado como esclavo, nombre: ");
  Serial.println(NOMBRE_PROPIO);
  Serial.println("Esperando conexion del nodo sensor...");

  // Secuencia de arranque: confirma que los actuadores funcionan
  autotest();
}

// ============================================================
//  LOOP
// ============================================================
void loop() {

  // --- Recepcion de tramas ---
  // readStringUntil('\n') acumula caracteres hasta el salto de
  // linea, reconstruyendo la trama completa enviada por el emisor.
  if (SerialBT.available()) {
    String trama = SerialBT.readStringUntil('\n');
    trama.trim();   // Elimina espacios y el retorno de carro

    if (trama.length() > 0) {
      tUltimaTrama = millis();
      enlaceVivo = true;
      digitalWrite(PIN_LED_EST, HIGH);

      Serial.print("[RX] ");
      Serial.println(trama);

      procesarTrama(trama);
    }
  }

  // --- Vigilancia del enlace ---
  // Si pasa demasiado tiempo sin recibir datos, se senaliza la
  // perdida de comunicacion en lugar de mantener el ultimo estado.
  if (enlaceVivo && (millis() - tUltimaTrama > TIMEOUT_ENLACE)) {
    enlaceVivo = false;
    digitalWrite(PIN_LED_EST, LOW);
    Serial.println("Enlace perdido: sin datos del nodo sensor.");
    estadoEnlacePerdido();
  }
}

// ============================================================
//  INTERPRETACION DE LA TRAMA RECIBIDA
// ============================================================
void procesarTrama(String trama) {

  // La trama viene delimitada por punto y coma. Se separa en sus
  // seis campos localizando la posicion de cada delimitador.
  int p[5];
  int desde = 0;
  for (int i = 0; i < 5; i++) {
    p[i] = trama.indexOf(';', desde);
    // Si falta algun delimitador, la trama llego incompleta o
    // corrupta y se descarta sin actuar sobre los actuadores.
    if (p[i] < 0) {
      Serial.println("Trama malformada: descartada.");
      return;
    }
    desde = p[i] + 1;
  }

  String id         = trama.substring(0, p[0]);
  String estado     = trama.substring(p[0] + 1, p[1]);
  double frecuencia = trama.substring(p[1] + 1, p[2]).toDouble();
  double base       = trama.substring(p[2] + 1, p[3]).toDouble();
  double variacion  = trama.substring(p[3] + 1, p[4]).toDouble();
  double amplitud   = trama.substring(p[4] + 1).toDouble();

  // --- Reporte por consola ---
  Serial.print("  Nodo: ");       Serial.println(id);
  Serial.print("  Estado: ");     Serial.println(estado);
  Serial.print("  Frecuencia: "); Serial.print(frecuencia, 2); Serial.println(" Hz");
  Serial.print("  Linea base: "); Serial.print(base, 2);       Serial.println(" Hz");
  Serial.print("  Variacion: ");  Serial.print(variacion, 1);  Serial.println(" %");
  Serial.print("  Amplitud: ");   Serial.println(amplitud, 0);

  // --- Accionamiento de los actuadores ---
  if (estado == "ALERTA") {
    estadoAlerta();
  }
  else if (estado == "VIBRACION") {
    estadoVibracion();
  }
  else if (estado == "NORMAL") {
    estadoNormal();
  }
  else if (estado == "CALIBRADO") {
    estadoCalibrado();
  }
  else if (estado == "REPOSO" || estado == "SIN_BASE") {
    estadoEspera();
  }
}

// ============================================================
//  ESTADOS DE LOS ACTUADORES
// ============================================================

// Estructura dentro de parametros
void estadoNormal() {
  digitalWrite(PIN_LED_VERDE, HIGH);
  digitalWrite(PIN_LED_ROJO, LOW);
  noTone(PIN_BUZZER);
}

// Caida de frecuencia sobre el umbral: posible perdida de rigidez
void estadoAlerta() {
  digitalWrite(PIN_LED_VERDE, LOW);
  digitalWrite(PIN_LED_ROJO, HIGH);
  tone(PIN_BUZZER, 2000, 300);
}

// Vibracion fuerte sin perdida de rigidez (sismo en curso).
// Amarillo con dos LEDs: verde y rojo juntos, mas un pitido corto.
void estadoVibracion() {
  digitalWrite(PIN_LED_VERDE, HIGH);
  digitalWrite(PIN_LED_ROJO, HIGH);
  tone(PIN_BUZZER, 1200, 80);
}

// Linea base registrada: confirmacion audible y visual
void estadoCalibrado() {
  for (int i = 0; i < 3; i++) {
    digitalWrite(PIN_LED_VERDE, HIGH);
    tone(PIN_BUZZER, 1500, 100);
    delay(150);
    digitalWrite(PIN_LED_VERDE, LOW);
    delay(150);
  }
  estadoNormal();
}

// Sin excitacion suficiente o sin linea base registrada
void estadoEspera() {
  digitalWrite(PIN_LED_VERDE, HIGH);
  digitalWrite(PIN_LED_ROJO, LOW);
  noTone(PIN_BUZZER);
}

// Sin comunicacion con el nodo sensor.
// Se senaliza con ambos LED parpadeando: no es una alerta
// estructural, pero tampoco puede presentarse como normalidad.
void estadoEnlacePerdido() {
  for (int i = 0; i < 4; i++) {
    digitalWrite(PIN_LED_VERDE, HIGH);
    digitalWrite(PIN_LED_ROJO, HIGH);
    delay(200);
    digitalWrite(PIN_LED_VERDE, LOW);
    digitalWrite(PIN_LED_ROJO, LOW);
    delay(200);
  }
}

// ============================================================
//  AUTOTEST DE ARRANQUE
// ============================================================
void autotest() {
  Serial.println("Autotest de actuadores...");

  digitalWrite(PIN_LED_VERDE, HIGH);
  delay(400);
  digitalWrite(PIN_LED_VERDE, LOW);

  digitalWrite(PIN_LED_ROJO, HIGH);
  delay(400);
  digitalWrite(PIN_LED_ROJO, LOW);

  tone(PIN_BUZZER, 1000, 200);
  delay(400);

  Serial.println("Autotest completo.");
}
