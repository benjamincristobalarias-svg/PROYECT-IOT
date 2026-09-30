# Armado electrónico — Nodo sensor y Nodo base

Son **dos circuitos separados**, uno en cada protoboard, cada uno con su ESP32.

**Regla de oro: cablear SIEMPRE con el USB desconectado.**

---

## Antes de empezar: cómo va el ESP32 en la protoboard

El ESP32 se clava **cruzando el canal central** de la protoboard, con una fila de pines a cada lado. Así cada pin queda en su propia columna y hay agujeros libres para conectar cables.

Si el ESP32 es muy ancho y tapa los agujeros de un lado, está bien: usa cables Dupont desde el lado que quede libre, o conecta directo al pin con cable macho-hembra.

Los pines vienen rotulados en la placa: 3V3, GND, D4, D18, D19, D21, D22, D23, D34.

---

## NODO SENSOR (protoboard 1)

Lleva: ESP32 + MPU6050 + potenciómetro + botón

### MPU6050 (4 cables)

| Pin del MPU6050 | Va a ESP32 | Color sugerido |
|---|---|---|
| VCC | 3V3 | Rojo |
| GND | GND | Negro |
| SCL | D22 | Amarillo |
| SDA | D21 | Verde |

Los otros pines del MPU6050 (XDA, XCL, AD0, INT) **se dejan sin conectar**.

### Potenciómetro (3 cables)

Tiene 3 patas en fila:

| Pata | Va a ESP32 |
|---|---|
| Izquierda | GND |
| **Centro** | **D34** |
| Derecha | 3V3 |

La pata del centro es la que entrega la señal. Si al girarlo funciona al revés (baja cuando debería subir), se intercambian izquierda y derecha. No se daña nada.

### Botón (2 cables)

El botón tiene 4 patas. **Las del mismo lado están unidas por dentro.** Hay que usar dos patas en **diagonal**.

| Pata | Va a ESP32 |
|---|---|
| Una esquina | D4 |
| Esquina opuesta (diagonal) | GND |

No lleva resistencia.

> Si al probar el sistema registra la línea base solo, sin apretar el botón, es porque se usaron dos patas del mismo lado.

---

## NODO BASE (protoboard 2)

Lleva: ESP32 + 2 LEDs + 2 resistencias + buzzer

### Cómo reconocer las patas del LED

- **Pata larga = positivo (+)** → va hacia el pin del ESP32 (pasando por la resistencia)
- **Pata corta = negativo (−)** → va a GND

Si un LED no enciende, casi siempre está al revés. Se da vuelta y listo.

### LED verde

```
D18 ──── resistencia 220Ω ──── pata larga LED verde
                               pata corta LED verde ──── GND
```

1. Un cable de **D18** a una fila de la protoboard
2. Una pata de la **resistencia** en esa misma fila, la otra pata en una fila nueva
3. La **pata larga** del LED en esa fila nueva
4. La **pata corta** del LED en otra fila, y de ahí un cable a **GND**

### LED rojo

Exactamente igual, pero desde **D19**.

```
D19 ──── resistencia 220Ω ──── pata larga LED rojo
                               pata corta LED rojo ──── GND
```

### Buzzer (2 cables)

| Pata | Va a ESP32 |
|---|---|
| Positivo (pata larga o marcada con +) | D23 |
| Negativo | GND |

---

## Truco para GND

Los dos LEDs y el buzzer necesitan GND. En vez de 3 cables al ESP32, se hace así:

1. Un cable de **GND del ESP32** a la **línea azul (−)** del borde de la protoboard
2. Todas las patas negativas se conectan a esa línea azul

Todo lo que está en esa línea queda conectado entre sí.

Lo mismo se puede hacer en el nodo sensor con GND y 3V3 (línea roja +).

---

## Revisión antes de conectar el USB

**Nodo sensor:**
- [ ] MPU6050: VCC→3V3, GND→GND, SCL→D22, SDA→D21
- [ ] Potenciómetro: centro→D34, extremos a 3V3 y GND
- [ ] Botón: patas en diagonal, a D4 y GND

**Nodo base:**
- [ ] LED verde: pata larga por resistencia a D18, pata corta a GND
- [ ] LED rojo: pata larga por resistencia a D19, pata corta a GND
- [ ] Buzzer: + a D23, − a GND

**Ambos:**
- [ ] Ningún cable une 3V3 con GND directamente (eso es cortocircuito)
- [ ] Cada cable está en la fila correcta y no cruza el canal central por error
- [ ] Todo entra firme, nada a medio clavar

---

## Prueba rápida después de armar

1. Cargar `nodo_base.ino` en el ESP32 del nodo base y encenderlo
   → Al arrancar debe prender el LED verde, luego el rojo, y sonar un pitido (autotest)
   → **Si eso pasa, el nodo base está bien cableado**

2. Cargar `nodo_sensor.ino` en el otro ESP32 y encenderlo
   → Monitor serie a 115200: debe decir "Enlace Bluetooth establecido"
   → Al girar el potenciómetro debe cambiar la "Frecuencia inyectada"

3. Demo completa:
   → Potenciómetro alto → apretar botón → parpadeo verde en el nodo base
   → Bajar potenciómetro → **LED rojo + pitido** en el nodo base

**Recién cuando todo esto funcione, pegar las protoboards en la maqueta.**

---

## Si algo falla

| Problema | Revisar |
|---|---|
| No hay autotest al encender el nodo base | LEDs al revés, o pines D18/D19 mal |
| El sensor dice "MPU6050 no detectado" | SDA/SCL invertidos o VCC sin conectar |
| La frecuencia no cambia al girar el potenciómetro | Pata central no está en D34 |
| Registra la base solo | Botón con patas del mismo lado |
| El sensor no conecta con el base | Encender primero el base; reiniciar el sensor |
| No suena el buzzer | Polaridad invertida |
