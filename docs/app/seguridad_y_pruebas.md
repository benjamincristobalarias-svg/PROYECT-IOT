# Seguridad y pruebas de la app (Unidad 2)

> Hecho con IA (Claude).

## 1. Seguridad

| Medida | Estado | Cómo |
|---|---|---|
| Permisos mínimos | Aplicada | La app solo pide **INTERNET** |
| Cifrado en tránsito | Aplicada | Firebase se comunica por **HTTPS/TLS** |
| Acceso solo a lo necesario | Aplicada | Las reglas solo permiten leer y escribir en `estructuras` e `historial`; el resto de la base queda cerrado |
| Registro de datos | Aplicada | `historial` guarda cada lectura enviada, para revisar qué pasó |
| Autenticación (ISO/IEC 27400) | Pendiente | Inicio de sesión con Firebase Authentication |
| Control de acceso por usuario | Pendiente | Cambiar las reglas a `auth != null` cuando exista el login |

> Las reglas actuales son de **modo de prueba**: cualquiera con la dirección de la base podría escribir. Se cierran al agregar el inicio de sesión.

### Lineamientos de la industria OT

- **Conexión y condición separadas:** una estructura puede estar conectada y con daño; son dos estados distintos (`conectada` en Firebase).
- **Lecturas antiguas marcadas:** si un sensor se desconecta, su última lectura se muestra como "no actual" y no cuenta como alerta.
- **Sin datos inventados:** si no hay lectura, Firebase no guarda el campo `lectura` y la app muestra "Sin datos", nunca un 0.
- **Línea base solo en terreno:** la frecuencia de referencia se registra junto al sensor, nunca a distancia.

## 2. Pruebas

### Pruebas automáticas (`SimulacionTest.kt`)

| Prueba | Resultado |
|---|---|
| Al inicio, Bodega Norte no tiene lectura | Pasó |
| A los 6 s, Edificio B tiene daño | Pasó |
| A los 15 s, Edificio A tiene sismo pero no daño | Pasó |
| Al desconectarse, Bodega guarda su última lectura | Pasó |
| Una caída de justo 5 % no cuenta como daño | Pasó |

### Pruebas con Firebase

| N° | Comprobar | ✓ / ✗ |
|---|---|---|
| 1 | Al abrir la app aparecen `estructuras/NODO-01`, `NODO-02` y `NODO-03` en la consola | |
| 2 | Los valores en la consola cambian cada 3 segundos | |
| 3 | `historial` agrega una lectura nueva por cada envío | |
| 4 | A los 6 s, Edificio B aparece en rojo en la app y con frecuencia 30 en Firebase | |
| 5 | A los 12 s, `NODO-03/conectada` queda en false y la app marca la lectura como no actual | |
| 6 | A los 15 s, Edificio A aparece en amarillo (sismo) | |
| 7 | Al cambiar un valor a mano en la consola, la app lo muestra (hasta el siguiente envío, 3 s después) | |
| 8 | Sin internet, la app muestra los últimos datos y se actualiza sola al volver la red | |
