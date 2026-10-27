# Seguridad y pruebas de la app (Unidad 2)

## 1. Medidas de seguridad (ISO/IEC 27400)

| Medida | Cómo se cumple | Dónde |
|---|---|---|
| Autenticación | Firebase Authentication con correo y contraseña. Sin sesión no se entra a la app. | `LoginActivity` |
| Control de acceso | Las reglas de la base de datos solo dejan leer y escribir a usuarios con sesión iniciada (`auth != null`). | `firebase/database.rules.json` |
| Cifrado en tránsito (HTTPS/TLS) | Firebase se comunica cifrado y la app bloquea cualquier conexión sin cifrar. | `res/xml/network_security_config.xml` |
| Protección de credenciales | La contraseña nunca se guarda en el celular. Firebase la almacena con *hash* (cifrado de una sola vía). | Firebase Authentication |
| Mensajes de error genéricos | El login no indica si el que falló fue el correo o la contraseña, así no se revela qué cuentas existen. | `LoginActivity` |
| Cierre de sesión | Botón **Cerrar sesión**: después no se puede volver atrás a las pantallas. | `InicioActivity` |
| Sin respaldos de datos | `allowBackup="false"`: los datos de la app no se copian a respaldos. | `AndroidManifest.xml` |
| Permisos mínimos | La app solo pide **INTERNET**. | `AndroidManifest.xml` |
| Cuentas individuales | Cada integrante tiene su usuario. El administrador lo crea o lo deshabilita desde Firebase. | Consola de Firebase |

## 2. Lineamientos de seguridad para la industria OT

OT (tecnología operacional) son los sistemas que miden o controlan procesos físicos, como estos sensores en estructuras. En OT lo más importante es que el sistema **siga funcionando y sea confiable**.

- **Disponibilidad primero:** si se corta la red, el monitoreo sigue mostrando el último dato y avisa "Sin conexión". El nodo físico sigue alertando en el lugar con LED y buzzer.
- **Separar monitoreo de control:** el celular de monitoreo solo lee. Los cambios críticos, como la línea base, se hacen **en terreno** (pulsador mantenido 3 s en el nodo), nunca a distancia.
- **Mínimo privilegio:** cada persona entra con su propia cuenta y solo con los permisos que necesita.
- **Comunicación cifrada y autenticada** entre los dispositivos y el servidor (TLS).
- **Registro de eventos:** el historial guarda cada medición para revisar después qué pasó.
- **Detección de fallas:** si un nodo deja de enviar por más de 15 s, se muestra en gris (*Sin datos*).
- Normas de referencia: **IEC 62443** (seguridad en automatización industrial) e **ISO/IEC 27400** (seguridad y privacidad en IoT).

## 3. Plan de pruebas

| N° | Prueba | Pasos | Resultado esperado | ✓ / ✗ |
|---|---|---|---|---|
| P1 | Login correcto | Correo y contraseña válidos | Entra al menú | |
| P2 | Login incorrecto | Contraseña mala | "Correo o contraseña incorrectos" | |
| P3 | Correo inválido | Escribir "hola" | "Escribe un correo válido" | |
| P4 | Recuperar contraseña | Correo + "¿Olvidaste tu contraseña?" | Aviso en pantalla y llega el correo | |
| P5 | Envío entre celulares | A envía una medición | B la muestra en menos de 2 s | |
| P6 | Daño | Línea base 40 Hz, bajar a 30 Hz | Rojo "Daño", primero en la lista | |
| P7 | Sismo | 40 Hz + switch sismo | Amarillo "Sismo" | |
| P8 | Sin línea base | Elegir un nodo nuevo y enviar | Azul "Sin línea base" | |
| P9 | Nodo caído | Detener el envío | Gris "Sin datos" a los 15 s | |
| P10 | Monitoreo sin internet | Apagar WiFi y datos en B | Aviso "Sin conexión" y se recupera solo | |
| P11 | Activador sin internet | Enviar sin red | Queda "Enviando…" y se envía al volver la red | |
| P12 | Historial | Tocar una estructura | Se ven las últimas 10 mediciones | |
| P13 | Acceso sin sesión | Firebase → Reglas → simulador, lectura sin autenticar | Acceso denegado | |
| P14 | Cerrar sesión | Botón Cerrar sesión y luego "atrás" | Vuelve al login y no regresa a la app | |
| P15 | Varias estructuras | Enviar NODO-01 y NODO-02 | Ambas en la lista, ordenadas por gravedad | |
