# Seguridad y pruebas de la app (Unidad 2)

> Hecho con IA (Claude).

## 1. Seguridad

Por ahora la app no usa internet ni pide permisos: los datos son simulados y no salen del celular.

### Lineamientos de la industria OT aplicados desde el diseño

- **Conexión y condición separadas:** una estructura puede estar conectada y con daño; son dos estados distintos.
- **Lecturas antiguas marcadas:** si un sensor se desconecta, su última lectura se muestra como "no actual" y no cuenta como alerta.
- **Sin datos inventados:** si no hay lectura se muestra "Sin datos" (`null`), nunca un 0.
- **Línea base solo en terreno:** la frecuencia de referencia se registra junto al sensor, nunca a distancia.

### Medidas ISO/IEC 27400 que se agregan con Firebase (siguiente guía)

| Medida | Cómo se aplicará |
|---|---|
| Autenticación | Inicio de sesión con correo y contraseña |
| Control de acceso | Reglas de Firebase: solo usuarios con sesión leen y escriben |
| Cifrado en tránsito | Conexión HTTPS/TLS con Firebase |
| Protección de credenciales | La contraseña no se guarda en el celular |

## 2. Pruebas

### Pruebas automáticas (`SimulacionTest.kt`)

| Prueba | Resultado |
|---|---|
| Al inicio, Bodega Norte no tiene lectura | Pasó |
| A los 6 s, Edificio B tiene daño | Pasó |
| A los 15 s, Edificio A tiene sismo pero no daño | Pasó |
| Al desconectarse, Bodega guarda su última lectura | Pasó |
| Una caída de justo 5 % no cuenta como daño | Pasó |

### Lista de aceptación en el celular

| N° | Comprobar | ✓ / ✗ |
|---|---|---|
| 1 | Se ven Edificio A, Edificio B y Bodega Norte con IDs distintos | |
| 2 | Cada lectura muestra frecuencia, línea base, caída y hora | |
| 3 | Los valores cambian cada 3 segundos | |
| 4 | Al inicio, Bodega Norte muestra "Sin lecturas disponibles" | |
| 5 | A los 6 s, Edificio B aparece en rojo con "Daño" | |
| 6 | A los 12 s, Bodega Norte conserva su lectura y la marca como no actual | |
| 7 | A los 15 s, Edificio A aparece en amarillo con "Sismo" | |
| 8 | El contador de daños solo cuenta estructuras conectadas | |
| 9 | Al girar el celular la lista sigue funcionando | |
| 10 | La app funciona en modo avión | |
