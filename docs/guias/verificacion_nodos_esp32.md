# Instrucciones de verificación — Nodos Bluetooth

**Qué hay que hacer:** verificar que los dos sketches compilan y, si alcanzas, probar el enlace Bluetooth entre los dos ESP32.

**Archivos:** `nodo_sensor.ino` y `nodo_base.ino`

---

## Preparar el Arduino IDE

### 1. Instalar soporte para ESP32

Si el IDE ya tiene ESP32 instalado, salta este paso.

1. `Archivo → Preferencias`
2. En **"Gestor de URLs Adicionales de Tarjetas"** pegar:
   ```
   https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json
   ```
3. `Herramientas → Placa → Gestor de tarjetas`
4. Buscar **esp32** e instalar el paquete de Espressif

### 2. Instalar librerías

`Herramientas → Administrar bibliotecas` e instalar:

- **Adafruit MPU6050** → aceptar cuando ofrezca instalar las dependencias (Adafruit Unified Sensor y Adafruit BusIO)
- **arduinoFFT**

> `BluetoothSerial.h` y `Preferences.h` ya vienen incluidas con el ESP32, no hay que instalarlas.

### 3. Seleccionar la placa

`Herramientas → Placa → ESP32 Arduino → ESP32 Dev Module`

---

## PARTE 1 — Verificación de compilación (obligatorio)

Esto **no requiere tener el ESP32 conectado**.

1. Abrir `nodo_sensor.ino`
2. Presionar el botón **✓ (Verificar)** — el de la izquierda, no la flecha
3. Esperar a que termine
4. Repetir con `nodo_base.ino`

### Qué registrar

**Si compila bien:**
- Captura de pantalla de la consola mostrando el mensaje de compilación exitosa
- Anotar el porcentaje de memoria de programa y de memoria dinámica que aparece al final

**Si da error:**
- Captura de pantalla **completa** del error (con todo el texto rojo visible)
- Copiar el texto del error a un archivo aparte
- Anotar en qué archivo de los dos ocurrió

> El error más común es que falte una librería. El mensaje dice algo como `No such file or directory` seguido del nombre del archivo que falta.

---

## PARTE 2 — Prueba del enlace Bluetooth (si hay tiempo y dos ESP32)

### Carga de los sketches

1. Conectar el **primer ESP32** → `Herramientas → Puerto` → seleccionar el COM que aparezca
2. Cargar `nodo_base.ino` con el botón **→ (Subir)**
3. Desconectar
4. Conectar el **segundo ESP32** → volver a seleccionar el puerto (cambia de COM)
5. Cargar `nodo_sensor.ino`

> Si al subir se queda en "Connecting..." hay que mantener presionado el botón **BOOT** de la placa hasta que empiece a cargar.

### Orden de encendido

**Importante: primero el nodo base, después el nodo sensor.** El base tiene que estar esperando cuando el sensor intente conectarse.

### Cómo observar ambos a la vez

Hace falta ver los dos monitores serie al mismo tiempo. Dos opciones:

- Abrir **dos ventanas del Arduino IDE**, una por cada placa, cada una con su puerto
- O usar el IDE para una y un programa aparte (PuTTY, Termite) para la otra

Ambos monitores van a **115200 baudios**, no 9600.

### Qué debería verse

**Nodo base:**
```
=== NODO BASE ===
Bluetooth iniciado como esclavo, nombre: NodoBase
Esperando conexion del nodo sensor...
Autotest de actuadores...
```

**Nodo sensor:**
```
=== NODO SENSOR ===
Bluetooth iniciado como maestro.
Conectando con NodoBase...
Enlace Bluetooth establecido.
```

Y después, de forma repetida:
- En el sensor: líneas con el prefijo **`[TX]`**
- En el base: líneas con el prefijo **`[RX]`** con los mismos datos

**Esa correspondencia entre TX y RX es la prueba de que la comunicación funciona.**

### Qué registrar

- Captura de ambos monitores serie mostrando `[TX]` y `[RX]` con datos coincidentes
- Foto o video del nodo base con los LEDs respondiendo
- Si algo falla: captura del monitor donde ocurre y anotar en qué momento se detiene

---

## PARTE 3 — Demostración completa (opcional)

Si el enlace funciona, esta es la demo del proyecto:

1. Girar el potenciómetro del nodo sensor hasta una frecuencia **alta** (ej: 35 Hz)
2. Presionar el **botón** → debe aparecer `LINEA BASE REGISTRADA` y el nodo base hace tres parpadeos con pitido
3. Bajar el potenciómetro (ej: 28 Hz)
4. El sensor debe reportar una variación sobre 5% y enviar estado `ALERTA`
5. El nodo base debe encender el **LED rojo** y sonar el **buzzer**

### Qué registrar

- **Video** de la secuencia completa (es lo más valioso para el informe)
- Captura del monitor serie mostrando la transición de `NORMAL` a `ALERTA`
- Anotar la frecuencia de línea base registrada y la frecuencia tras la caída

---

## Resumen de lo mínimo a devolver

| Prioridad | Qué |
|---|---|
| **Obligatorio** | Resultado de compilación de ambos sketches (captura) |
| **Obligatorio** | Si hay errores: texto completo del error |
| Deseable | Capturas de TX/RX si se prueba el enlace |
| Deseable | Video de la demo completa |

---

## Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---|---|---|
| `BluetoothSerial.h: No such file` | Falta el soporte ESP32, o la placa seleccionada no es ESP32 | Revisar `Herramientas → Placa` |
| `arduinoFFT.h: No such file` | Falta la librería | Instalarla desde el gestor |
| Errores con `FFTWindow::Hamming` | Versión antigua de arduinoFFT (v1.x) | Actualizar a v2.x desde el gestor |
| No aparece ningún puerto COM | Falta driver USB-Serial, o el cable es solo de carga | Instalar driver CP2102 o CH340; probar otro cable |
| `Failed to connect to ESP32` | La placa no entra en modo carga | Mantener BOOT presionado durante "Connecting..." |
| El sensor no encuentra al base | El base no estaba encendido primero | Reiniciar en el orden correcto |
| El base nunca recibe nada | Los nombres no coinciden | Verificar que `NOMBRE_DESTINO` del sensor sea igual a `NOMBRE_PROPIO` del base |
