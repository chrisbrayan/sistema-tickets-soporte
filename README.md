# Sistema de gestión de tickets de soporte técnico

API REST inicial para registrar y gestionar solicitudes de soporte técnico de empleados o clientes.

Este proyecto corresponde al primer avance APF1. Está implementado con Spring Boot, arquitectura por capas y almacenamiento en memoria. No utiliza todavía base de datos, JPA, JWT, Angular ni despliegue cloud.

## Requisitos

- Java 21.
- Maven Wrapper incluido en el proyecto (`mvnw.cmd`). No es necesario instalar Maven globalmente.
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

## Ejecutar las pruebas

```powershell
.\mvnw.cmd test
```

La suite incluye pruebas del modelo, repositorio en memoria, servicio y contrato HTTP con MockMvc.

## Caso TDD: RED → GREEN → REFACTOR

Caso elegido: crear un ticket nuevo debe asignarle un ID y dejarlo en estado `ABIERTO`.

1. **RED**: se escribió la prueba `crearAsignaIdYValoresIniciales` en `TicketServiceTest`, esperando que un ticket nuevo tuviera un ID y estado `ABIERTO`. La primera implementación del servicio todavía no delegaba correctamente al repositorio, por lo que la prueba fallaba.
2. **GREEN**: se implementó `TicketService.crear`, se validaron las referencias de usuario y categoría y se delegó el guardado en `InMemoryTicketRepository`. El repositorio generó el ID y `Ticket` proporcionó el estado inicial `ABIERTO`; la prueba pasó.
3. **REFACTOR**: se separaron las responsabilidades en `TicketRepository`, repositorios de catálogo, excepciones de dominio y métodos privados de validación/normalización. El comportamiento se mantuvo y la suite completa continúa pasando.

La prueba verifica el resultado observable, no la implementación interna, por lo que seguirá siendo válida cuando el almacenamiento en memoria sea sustituido por otro repositorio.

## Colección de Postman

La colección se encuentra en:

```text
postman/Sistema-Tickets-APF1.postman_collection.json
```

Para usarla:

1. Inicia la aplicación.
2. Abre Postman y selecciona **Import**.
3. Importa el archivo JSON de la colección.
4. Ejecuta primero **Crear ticket**.
5. La respuesta `201` guarda automáticamente el ID en la variable `ticketId`.
6. Ejecuta el resto de peticiones del flujo.

Variables de colección:

| Variable   | Valor inicial           | Uso                                          |
| ---------- | ----------------------- | -------------------------------------------- |
| `baseUrl`  | `http://localhost:8080` | URL base de la API                           |
| `ticketId` | `1`                     | ID usado por las operaciones sobre un ticket |

La colección incluye:

- Crear ticket (`POST`, `201`).
- Listar tickets (`GET`, `200`).
- Obtener ticket (`GET`, `200`).
- Modificar ticket (`PUT`, `200`).
- Eliminar ticket (`DELETE`, `204`).
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

| Método   | URI                 | Éxito | Descripción      |
| -------- | ------------------- | ----: | ---------------- |
| `POST`   | `/api/tickets`      | `201` | Crear ticket     |
| `GET`    | `/api/tickets`      | `200` | Listar tickets   |
| `GET`    | `/api/tickets/{id}` | `200` | Obtener ticket   |
| `PUT`    | `/api/tickets/{id}` | `200` | Modificar ticket |
| `DELETE` | `/api/tickets/{id}` | `204` | Eliminar ticket  |

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

## Catálogos iniciales en memoria

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
│   │   └── TicketController.java
│   ├── dto/
│   │   ├── ApiError.java
│   │   ├── TicketRequest.java
│   │   └── TicketResponse.java
│   ├── exception/
│   ├── model/
│   ├── repository/
│   └── service/
└── test/java/com/utp/sistema_tickets/
    ├── controller/
    ├── model/
    ├── repository/
    └── service/
postman/
└── Sistema-Tickets-APF1.postman_collection.json
```

## Arquitectura

```text
HTTP → TicketController → TicketService → TicketRepository
                                      ├→ UsuarioRepository
                                      └→ CategoriaRepository
```

- `Controller`: recibe solicitudes HTTP y devuelve respuestas HTTP.
- `Service`: aplica reglas de negocio y valida referencias.
- `Repository`: abstrae el almacenamiento y actualmente usa colecciones en memoria.
- `DTO`: separa el contrato HTTP del modelo de dominio.
- `ApiExceptionHandler`: unifica las respuestas de error.

Al reiniciar la aplicación se pierden los tickets creados, porque APF1 no utiliza todavía una base de datos.
