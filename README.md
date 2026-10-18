# Sistema de gestión de tickets de soporte técnico

API REST inicial para registrar y gestionar solicitudes de soporte técnico de empleados o clientes.

Este proyecto comenzó como el primer avance APF1. En APF2 se implementó la evolución de la API hacia una solución persistente y segura.

## Resumen de APF2

- **Migración a base de datos relacional:** el almacenamiento en memoria fue reemplazado por persistencia con JPA/Hibernate y MySQL.
- **CRUD persistente:** los recursos principales guardan, consultan, actualizan y eliminan información en la base de datos.
- **Consulta JPQL útil:** se incorporaron consultas JPQL para obtener información operativa de los tickets.
- **Operación transaccional:** las modificaciones de tickets y el registro de historial se coordinan mediante transacciones.
- **Spring Security:** se configuraron permisos diferenciados para `USUARIO`, `TECNICO` y `ADMIN`.
- **Autenticación JWT:** se implementó login, generación de tokens, validación de tokens y rutas protegidas.
- **Tests actualizados:** se adaptaron y ampliaron las pruebas de persistencia, servicio, transacciones, seguridad y MockMvc.
- **Postman actualizado:** la colección incluye login, uso del token Bearer, rutas protegidas y respuestas `401`/`403`.

## Requisitos

- Java 21.
- Maven Wrapper incluido en el proyecto (`mvnw.cmd`). No es necesario instalar Maven globalmente.
- MySQL 8 o compatible instalado y ejecutándose localmente en el puerto `3306`.
- Postman, opcional, para ejecutar la colección incluida.

Verifica Java con:

```powershell
java -version
```

## Ejecutar la aplicación

Desde la raíz del proyecto:

```powershell
.\mvnw.cmd spring-boot:run
```

La API queda disponible en:

```text
http://localhost:8080
```

La documentación OpenAPI, si se desea consultar, está disponible en:

```text
http://localhost:8080/swagger-ui.html
```

## Preparar MySQL para APF2

La aplicación utiliza la siguiente configuración de conexión predeterminada:

```text
jdbc:mysql://localhost:3306/sistema_tickets
```

Para ejecutar el proyecto, MySQL debe estar disponible en localhost:3306 y la base de datos sistema_tickets debe existir o poder crearse automáticamente.

### Valores configurables

Los valores predeterminados de conexión son:

| Variable            | Valor para desarrollo        | Propósito                                      |
| ------------------- | ---------------------------- | ---------------------------------------------- |
| `MYSQL_HOST`        | `localhost`                  | Host de MySQL                                  |
| `MYSQL_DATABASE`    | `sistema_tickets`            | Base de datos                                  |
| `MYSQL_USERNAME`    | `root`                       | Usuario de conexión                            |
| `MYSQL_PASSWORD`    | `root_password_dev`          | Contraseña de conexión                         |
| `JWT_SECRET`        | clave de desarrollo incluida | Firma de tokens; usar una propia en producción |
| `JWT_EXPIRATION_MS` | `3600000`                    | Expiración del JWT en milisegundos             |

La clave `JWT_SECRET` debe tener al menos 32 bytes. En PowerShell se pueden sobrescribir temporalmente antes de iniciar la aplicación:

```powershell
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="root_password_dev"
$env:JWT_SECRET="una-clave-local-de-al-menos-32-caracteres"
$env:JWT_EXPIRATION_MS="3600000"
```

### MySQL local en Windows

Crear una base llamada `sistema_tickets`, iniciar el servicio MySQL local en el puerto `3306` y ejecutar:

```powershell
$env:MYSQL_USERNAME="root"
$env:MYSQL_PASSWORD="tu_clave"
.\mvnw.cmd spring-boot:run
```

La aplicación conserva `localhost:3306`; solo se cambian las credenciales mediante `MYSQL_USERNAME` y `MYSQL_PASSWORD` si son diferentes.

## Ejecutar las pruebas

```powershell
.\mvnw.cmd test
```

La suite incluye pruebas del modelo, repositorios JPA, servicio, contrato HTTP con MockMvc, transacciones y seguridad. El perfil de prueba usa H2 aislado para evitar que las pruebas dependan de un MySQL externo.

La verificación de APF2 cubre:

- Persistencia de tickets, relaciones con usuario/categoría, consultas JPQL e IDs inexistentes.
- Creación, actualización, validación de tickets cerrados, roles de técnicos y rollback transaccional.
- Login JWT, credenciales inválidas, token ausente, roles insuficientes y roles permitidos.
- Contratos HTTP para login, listado protegido, aislamiento de tickets y rutas administrativas.

## Caso TDD: RED → GREEN → REFACTOR

Caso elegido: crear un ticket nuevo debe asignarle un ID y dejarlo en estado `ABIERTO`.

1. **RED**: se escribió la prueba `crearAsignaIdYValoresIniciales` en `TicketServiceTest`, esperando que un ticket nuevo tuviera un ID y estado `ABIERTO`.
2. **GREEN**: se implementó el servicio y el repositorio persistente para satisfacer el comportamiento esperado.
3. **REFACTOR**: se separaron las responsabilidades entre servicio, repositorios JPA, validaciones, excepciones e historial transaccional. El comportamiento se mantuvo y la suite continúa pasando.

La prueba verifica el resultado observable, no la implementación interna, por lo que sigue siendo válida con el repositorio JPA actual.

## Usuarios de desarrollo y seguridad

La Fase 5 persiste los usuarios en la tabla `usuarios`. La columna `password_hash` contiene hashes BCrypt; nunca se almacena la contraseña original. `JpaUserDetailsService` busca por email y convierte el rol en una autoridad Spring Security (`ROLE_USUARIO`, `ROLE_TECNICO` o `ROLE_ADMIN`).

Al iniciar la aplicación se crean, si no existen, estos usuarios de desarrollo:

| Email                      | Contraseña de desarrollo | Rol       |
| -------------------------- | ------------------------ | --------- |
| `ana.usuario@example.com`  | `ana1234`                | `USUARIO` |
| `luis.tecnico@example.com` | `tecnico1234`            | `TECNICO` |
| `maria.admin@example.com`  | `admin1234`              | `ADMIN`   |

Estas credenciales son únicamente para desarrollo local.

### Registro, login y uso del JWT

El registro público crea únicamente usuarios con rol `USUARIO`. El cliente no puede elegir ni elevar su rol:

```http
POST http://localhost:8080/api/auth/registro
Content-Type: application/json
```

```json
{
  "nombre": "Carlos Soporte",
  "email": "carlos@empresa.test",
  "password": "carlos1234"
}
```

La respuesta es `201 Created` y devuelve el usuario sin `passwordHash`. Para crear usuarios con cualquier rol, un administrador debe usar `POST /api/usuarios` con un JWT de `ADMIN`:

```json
{
  "nombre": "Nuevo técnico",
  "email": "tecnico2@empresa.test",
  "password": "tecnico1234",
  "rol": "TECNICO"
}
```

Esta operación responde `201 Created`; sin token responde `401` y con un rol distinto de `ADMIN` responde `403`. Los correos duplicados responden `400`.

Solicita un token con:

```http
POST http://localhost:8080/api/auth/login
Content-Type: application/json
```

```json
{
  "email": "ana.usuario@example.com",
  "password": "ana1234"
}
```

La respuesta `200 OK` contiene `token`, `tokenType`, `subject`, `role` y `expiresAt`. Para acceder a una ruta protegida se envía:

```http
Authorization: Bearer <token>
```

Respuestas de seguridad:

- `401 Unauthorized`: credenciales inválidas, token ausente, expirado o con firma inválida.
- `403 Forbidden`: token válido, pero el rol no tiene permiso para la operación.

Rutas públicas: `/api/auth/login`, `/api/auth/registro`, `/swagger-ui/**`, `/swagger-ui.html` y `/v3/api-docs/**`. Las operaciones de tickets requieren JWT; eliminar tickets requiere `ADMIN` y las operaciones de asignación, cambio de estado y cierre requieren `TECNICO` o `ADMIN`.

### Reglas de autorización por rol

- `USUARIO`: puede crear tickets y consultar únicamente los tickets donde figura como solicitante.
- `TECNICO`: puede consultar tickets, utilizar las búsquedas operativas, asignar técnicos, cambiar estados y cerrar tickets.
- `ADMIN`: tiene las capacidades del técnico, puede eliminar tickets y es el único rol autorizado para administrar `/api/usuarios/**` y `/api/categorias/**`.
- Un usuario que intenta consultar el ticket de otro recibe `404`, evitando revelar si el recurso existe.
- Las respuestas de seguridad conservan el formato común de error con `timestamp`, `status`, `error`, `message` y `path`.

## Colección de Postman

La colección se encuentra en:

```text
postman/Sistema-Tickets-APF2.postman_collection.json
```

Para usarla:

1. Inicia la aplicación.
2. Abre Postman y selecciona **Import**.
3. Importa el archivo JSON de la colección.
4. Ejecuta los tres requests de **Autenticación** para guardar los tokens de usuario, técnico y administrador.
5. Ejecuta **Registro público - USUARIO** para comprobar que el rol siempre es `USUARIO`.
6. Ejecuta **Crear usuario - ADMIN** para comprobar la creación administrativa de un técnico.
7. Ejecuta **Crear ticket - USUARIO**; la respuesta `201` guarda automáticamente el ID en `ticketId`.
8. Ejecuta el resto de peticiones respetando el orden de las carpetas.

Variables de colección:

| Variable       | Valor inicial           | Uso                                    |
| -------------- | ----------------------- | -------------------------------------- |
| `baseUrl`      | `http://localhost:8080` | URL base de la API                     |
| `token`        | vacío                   | Token activo de trabajo                |
| `usuarioToken` | vacío                   | JWT del usuario Ana                    |
| `tecnicoToken` | vacío                   | JWT del técnico Luis                   |
| `adminToken`   | vacío                   | JWT de la administradora María         |
| `ticketId`     | vacío                   | ID guardado después de crear un ticket |

La colección incluye:

- Login de usuario, técnico y administrador (`POST`, `200`) con almacenamiento automático del JWT.
- Registro público (`POST`, `201`) que siempre crea un usuario con rol `USUARIO`.
- Alta administrativa (`POST`, `201`) de usuarios con cualquier rol usando `adminToken`.
- Rechazo de alta administrativa por un usuario normal (`POST`, `403`).
- Login inválido (`POST`, `401`).
- Crear ticket con usuario (`POST`, `201`).
- Listar y consultar tickets con token (`GET`, `200`).
- Modificar y asignar técnico (`PUT/PATCH`, `200`).
- Eliminar ticket con administrador (`DELETE`, `204`).
- Ruta protegida sin token (`GET`, `401`).
- Usuario sin permisos administrativos (`GET`, `403`).
- Validación con título vacío (`POST`, `400`).
- Ticket inexistente (`GET`, `404`).

## Ejemplo de creación

```http
POST http://localhost:8080/api/tickets
Content-Type: application/json
```

```json
{
  "titulo": "No puedo acceder al correo",
  "descripcion": "El sistema rechaza mis credenciales desde esta mañana.",
  "prioridad": "MEDIA",
  "usuarioId": 1,
  "categoriaId": 1
}
```

Respuesta esperada: `201 Created`.

```json
{
  "id": 1,
  "titulo": "No puedo acceder al correo",
  "descripcion": "El sistema rechaza mis credenciales desde esta mañana.",
  "prioridad": "MEDIA",
  "estado": "ABIERTO",
  "usuarioId": 1,
  "categoriaId": 1,
  "tecnicoId": null
}
```

## Endpoints

| Método   | URI                                                | Éxito | Descripción                                       |
| -------- | -------------------------------------------------- | ----: | ------------------------------------------------- |
| `POST`   | `/api/tickets`                                     | `201` | Crear ticket                                      |
| `GET`    | `/api/tickets`                                     | `200` | Listar tickets                                    |
| `GET`    | `/api/tickets/{id}`                                | `200` | Obtener ticket                                    |
| `PUT`    | `/api/tickets/{id}`                                | `200` | Modificar ticket                                  |
| `PATCH`  | `/api/tickets/{id}/asignacion`                     | `200` | Asignar técnico y registrar historial             |
| `PATCH`  | `/api/tickets/{id}/estado`                         | `200` | Cambiar estado y registrar transición             |
| `POST`   | `/api/tickets/{id}/cerrar`                         | `200` | Cerrar ticket y registrar cierre                  |
| `DELETE` | `/api/tickets/{id}`                                | `204` | Eliminar ticket                                   |
| `GET`    | `/api/tickets/search?estado=ABIERTO&categoriaId=1` | `200` | Buscar por estado y categoría mediante JPQL       |
| `GET`    | `/api/tickets/mis-asignados?tecnicoId=2`           | `200` | Tickets asignados no cerrados mediante JPQL       |
| `GET`    | `/api/tickets/filtrar`                             | `200` | Filtrar por estado, prioridad, categoría y fechas |
| `GET`    | `/api/tickets/resumen-por-estado`                  | `200` | Resumen agrupado por estado mediante JPQL         |
| `POST`   | `/api/auth/login`                                  | `200` | Autenticar y obtener JWT                          |
| `POST`   | `/api/auth/registro`                               | `201` | Registro público; siempre crea rol `USUARIO`      |
| `POST`   | `/api/usuarios`                                    | `201` | Crear usuario con cualquier rol; solo `ADMIN`     |

Errores principales:

- `400 Bad Request`: datos inválidos, campos obligatorios ausentes o JSON mal formado.
- `404 Not Found`: el ticket solicitado no existe.

Ejemplo de error `404`:

```json
{
  "timestamp": "2026-09-07T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Ticket no encontrado: 999999",
  "path": "/api/tickets/999999"
}
```

## Consultas JPQL de APF2

El repositorio persistente contiene consultas JPQL para las bandejas y reportes principales del soporte:

- Tickets por estado y categoría.
- Tickets asignados a un técnico que todavía no están cerrados.
- Filtros combinados por estado, prioridad, categoría y rango de fechas.
- Resumen agrupado de tickets por estado.

Estas consultas se ejecutan en la base de datos mediante `TicketRepository`; no se recupera toda la colección para filtrarla en Java.

## Operaciones transaccionales de APF2

`TicketService` coordina las actualizaciones del ticket y sus eventos en `TicketHistorial` mediante `@Transactional`:

- Crear ticket y registrar el evento inicial.
- Asignar o cambiar el técnico y registrar la asignación.
- Cambiar el estado y registrar la transición.
- Cerrar el ticket y registrar el cierre.

Si falla el guardado del historial, la transacción se revierte y el cambio del ticket no queda persistido.

## Catálogos iniciales del APF1

Para crear o modificar tickets se pueden usar estas referencias:

### Usuarios

|  ID | Rol       | Nombre               |
| --: | --------- | -------------------- |
| `1` | `USUARIO` | Ana Usuario          |
| `2` | `TECNICO` | Luis Técnico         |
| `3` | `ADMIN`   | María Administradora |

### Categorías

|  ID | Nombre   |
| --: | -------- |
| `1` | Acceso   |
| `2` | Hardware |
| `3` | Software |

## Estructura del proyecto

```text
src/
├── main/java/com/utp/sistema_tickets/
│   ├── controller/
│   │   ├── ApiExceptionHandler.java
│   │   ├── AuthController.java
│   │   └── TicketController.java
│   ├── dto/
│   │   ├── ApiError.java
│   │   ├── TicketRequest.java
│   │   └── TicketResponse.java
│   ├── exception/
│   ├── model/
│   ├── repository/
│   ├── security/
│   ├── service/
│   └── config/
└── test/java/com/utp/sistema_tickets/
    ├── controller/
    ├── model/
    ├── repository/
    └── service/
postman/
├── Sistema-Tickets-APF1.postman_collection.json
└── Sistema-Tickets-APF2.postman_collection.json
```

## Arquitectura

```text
HTTP → TicketController → TicketService → TicketRepository
                                      ├→ UsuarioRepository
                                      └→ CategoriaRepository
```

- `Controller`: recibe solicitudes HTTP y devuelve respuestas HTTP.
- `Service`: aplica reglas de negocio y valida referencias.
- `Repository`: abstrae el almacenamiento persistente con Spring Data JPA.
- `DTO`: separa el contrato HTTP del modelo de dominio.
- `ApiExceptionHandler`: unifica las respuestas de error.
- `Security`: configura BCrypt y carga las identidades persistidas mediante `UserDetailsService`.
- `Config`: crea datos iniciales de desarrollo de forma idempotente.
