# Sistema de gestión de tickets de soporte técnico

## APF3 — Frontend Angular integrado con Spring Boot

En este avance se implementó una interfaz Angular conectada a la API REST del backend Spring Boot. El sistema permite gestionar tickets desde una interfaz web, con autenticación JWT, navegación protegida, validaciones, control por roles y persistencia en MySQL.

## Funcionalidades implementadas

- **Proyecto Angular organizado:** el frontend se separa en `core`, `features` y `shared`. `core` contiene autenticación, guards, interceptores, layout y servicios; `features` contiene las pantallas funcionales; y `shared` contiene modelos TypeScript y componentes visuales reutilizables.
- **Rutas principales:** se implementaron las pantallas de login, registro, dashboard, listado de tickets, tickets asignados, creación, detalle, edición, usuarios y categorías. Las rutas protegidas requieren una sesión válida y las rutas administrativas requieren el rol `ADMIN`.
- **Formularios con validación:** se usan formularios reactivos para login, registro, tickets, usuarios y categorías. Se validan campos obligatorios, email, longitudes mínimas y máximas, contraseña, prioridad, categoría y roles permitidos antes de enviar información al backend.
- **Mensajes de validación visibles:** los errores aparecen debajo del campo correspondiente después de intentar enviar el formulario. Además, los botones se deshabilitan mientras el formulario es inválido o una operación se encuentra en proceso.
- **Consumo real de API REST:** los servicios Angular utilizan `HttpClient` para conectarse a Spring Boot. Se consumen endpoints reales de autenticación, tickets, historial, categorías, usuarios, filtros, resumen y operaciones de asignación, estado y cierre; no se utilizan listas simuladas para mostrar los tickets.
- **CRUD visible desde frontend:** un usuario puede crear y consultar sus tickets; el administrador puede listar, filtrar, editar y eliminar tickets; y el técnico puede consultar sus tickets asignados, cambiar estados, agregar observaciones y cerrar la atención cuando corresponde.
- **Manejo de errores HTTP:** los interceptores y el servicio de notificaciones convierten las respuestas del backend en mensajes comprensibles. Se contemplan datos inválidos (`400`), sesión ausente o expirada (`401`), permisos insuficientes (`403`), recurso inexistente (`404`), conflictos (`409`) y errores de conexión.
- **Autenticación desde Angular:** el login consume `POST /api/auth/login`, guarda el JWT y la información básica del usuario en `sessionStorage`, y permite cerrar sesión limpiando esos datos. El interceptor agrega automáticamente `Authorization: Bearer <token>` a las peticiones protegidas.
- **Autorización por rol:** la interfaz muestra u oculta acciones según `USUARIO`, `TECNICO` y `ADMIN`, pero la autorización definitiva permanece en Spring Security. Por ejemplo, solo el administrador puede administrar usuarios, categorías, asignar técnicos, editar y eliminar tickets.
- **Estados de experiencia de usuario:** las pantallas muestran indicadores de carga, mensajes de operación exitosa, estados vacíos y errores. Después de una respuesta HTTP se actualizan los datos mostrados, como el listado, el detalle, las métricas y el historial.
- **Evidencia end-to-end:** el flujo completo puede comprobarse creando o modificando un ticket desde Angular, observando la petición en Network, verificando la respuesta de Spring Boot y confirmando el cambio persistido en MySQL.

## Documentación detallada del APF3

### 1. Proyecto Angular organizado

El frontend se estructuró por responsabilidades para evitar que toda la lógica se concentre en un solo componente:

- `core/`: funcionalidades transversales de la aplicación.
  - `auth/`: sesión, login, logout, usuario autenticado y JWT.
  - `guards/`: protección de rutas por autenticación y rol.
  - `http/`: interceptores, transformación y notificación de errores.
  - `layout/`: menú lateral, encabezado, navegación y diseño responsive.
  - `services/`: comunicación con los recursos del backend.
- `features/`: funcionalidades visibles del sistema.
  - `auth/`: login y registro.
  - `dashboard/`: métricas, resumen y actividad reciente.
  - `tickets/`: listado, filtros, creación, edición y detalle.
  - `admin/`: gestión de usuarios y categorías.
- `shared/`: elementos reutilizables.
  - `models/`: interfaces TypeScript para los datos de la API.
  - `ui/`: avatares, iconos, diálogos, toasts y estilos de estados.

Archivos principales para revisar:

```text
frontend/src/app/core/auth/auth.service.ts
frontend/src/app/core/services/ticket.service.ts
frontend/src/app/core/layout/app-shell.component.ts
frontend/src/app/features/tickets/ticket-list.component.ts
frontend/src/app/features/tickets/ticket-form.component.ts
frontend/src/app/features/tickets/ticket-detail.component.ts
frontend/src/app/shared/models/ticket.model.ts
```

Los componentes se ocupan de la interacción y la presentación; los servicios ejecutan las peticiones HTTP; los modelos describen el contrato de datos; y los guards/interceptores resuelven necesidades transversales.

### 2. Rutas principales y navegación

Las rutas se configuran en `frontend/src/app/app.routes.ts`.

| Ruta                     | Funcionalidad                             | Acceso                             |
| ------------------------ | ----------------------------------------- | ---------------------------------- |
| `/login`                 | Autenticación                             | Público                            |
| `/registro`              | Registro de un usuario con rol `USUARIO`  | Público                            |
| `/dashboard`             | Resumen de tickets y actividad            | Usuario autenticado                |
| `/tickets`               | Listado y filtros                         | Usuario autenticado                |
| `/tickets/mis-asignados` | Tickets asignados al técnico              | `TECNICO`                          |
| `/tickets/nuevo`         | Creación de ticket                        | `USUARIO`, `ADMIN`                 |
| `/tickets/:id`           | Detalle, historial y acciones             | Usuario autenticado según permisos |
| `/tickets/:id/editar`    | Edición de ticket                         | `ADMIN`                            |
| `/admin/usuarios`        | Alta y listado de usuarios                | `ADMIN`                            |
| `/admin/categorias`      | Alta, edición y eliminación de categorías | `ADMIN`                            |

El `authGuard` comprueba que exista una sesión válida. Si no existe, redirige a `/login` y conserva la URL solicitada. El `roleGuard` verifica que el rol del usuario esté incluido en los roles permitidos por la ruta; de lo contrario, redirige al dashboard.

Archivos relacionados:

```text
frontend/src/app/app.routes.ts
frontend/src/app/core/guards/auth.guard.ts
frontend/src/app/core/guards/role.guard.ts
frontend/src/app/core/layout/app-shell.component.ts
```

Además de proteger las rutas, el layout adapta el menú: un usuario observa sus tickets, un técnico observa sus tickets asignados y un administrador observa las opciones de administración.

### 3. Formularios con validación

Se utiliza `ReactiveFormsModule` para que cada pantalla defina explícitamente sus controles y validadores.

#### Login

`frontend/src/app/features/auth/login.component.ts` valida:

- Correo obligatorio.
- Formato válido de correo.
- Contraseña obligatoria.

#### Registro

`frontend/src/app/features/auth/register.component.ts` valida nombre, correo, contraseña y confirmación de contraseña. El registro público no permite escoger un rol elevado: el backend crea el usuario con rol `USUARIO`.

#### Tickets

`frontend/src/app/features/tickets/ticket-form.component.ts` valida:

- Título obligatorio, con longitud mínima y máxima.
- Descripción obligatoria, con longitud mínima y máxima.
- Prioridad válida: `BAJA`, `MEDIA`, `ALTA` o `URGENTE`.
- Categoría obligatoria y existente en el catálogo.
- Usuario solicitante válido.

#### Usuarios y categorías

Los componentes administrativos validan nombre, correo, contraseña, rol, nombre de categoría y descripción antes de enviar la petición.

Los mensajes se muestran cerca del campo con error después de pulsar el botón de envío. Las operaciones muestran estados como `Guardando…`, `Cargando…` o `Eliminando…`, y los botones se deshabilitan durante la petición para evitar duplicados.

### 4. Consumo real de API REST

La URL base se centraliza en `frontend/src/environments/environment.ts`:

```ts
apiUrl: "http://localhost:8080/api";
```

Los servicios Angular consumen la API mediante `HttpClient`.

#### `TicketService`

Archivo: `frontend/src/app/core/services/ticket.service.ts`

| Método Angular       | Petición HTTP                         | Uso                                               |
| -------------------- | ------------------------------------- | ------------------------------------------------- |
| `listar()`           | `GET /api/tickets`                    | Listado según rol                                 |
| `obtener(id)`        | `GET /api/tickets/{id}`               | Detalle                                           |
| `historial(id)`      | `GET /api/tickets/{id}/historial`     | Historial del ticket                              |
| `crear()`            | `POST /api/tickets`                   | Crear ticket                                      |
| `actualizar()`       | `PUT /api/tickets/{id}`               | Editar ticket                                     |
| `eliminar()`         | `DELETE /api/tickets/{id}`            | Eliminar ticket                                   |
| `asignarTecnico()`   | `PATCH /api/tickets/{id}/asignacion`  | Asignación administrativa                         |
| `cambiarEstado()`    | `PATCH /api/tickets/{id}/estado`      | Cambio de estado                                  |
| `cerrar()`           | `POST /api/tickets/{id}/cerrar`       | Cierre de atención                                |
| `filtrar()`          | `GET /api/tickets/filtrar`            | Filtros por estado, prioridad, categoría y fechas |
| `misAsignados()`     | `GET /api/tickets/mis-asignados`      | Bandeja del técnico                               |
| `resumenPorEstado()` | `GET /api/tickets/resumen-por-estado` | Métricas del dashboard                            |

También se utilizan:

- `AuthService` para login, logout y registro.
- `CategoriaService` para cargar y administrar categorías.
- `UsuarioService` para listar técnicos y administrar usuarios.

### 5. CRUD visible desde el frontend

#### Usuario

- Crea un ticket desde `/tickets/nuevo`.
- Consulta sus propios tickets en `/tickets`.
- Abre el detalle y revisa estado, categoría, técnico e historial.
- Puede cerrar su propia solicitud cuando ya no necesita atención.

#### Técnico

- Accede a `/tickets/mis-asignados`.
- Consulta el detalle de un ticket asignado.
- Cambia el estado del ticket.
- Registra observaciones de la atención.
- Consulta el historial.
- Cierra el ticket cuando termina el trabajo.

#### Administrador

- Consulta todos los tickets.
- Filtra por estado, prioridad, categoría y fechas.
- Asigna técnicos.
- Edita y elimina tickets.
- Administra usuarios y categorías.
- Consulta estadísticas y el historial completo.

La interfaz oculta o deshabilita acciones que el rol no necesita, pero la restricción final se aplica en `SecurityConfig` y en `TicketController` del backend.

### 6. Manejo de errores HTTP

El frontend registra `authInterceptor` y `errorInterceptor` en `frontend/src/app/app.config.ts`.

`error.interceptor.ts` recibe los errores HTTP y los envía a `ErrorNotificationService`. Este servicio traduce cada código a un mensaje comprensible y `error-toast.component.ts` lo presenta visualmente.

| Código | Tratamiento en la interfaz                         | Prueba sugerida                                             |
| -----: | -------------------------------------------------- | ----------------------------------------------------------- |
|  `400` | Muestra datos inválidos y el detalle de validación | Crear ticket con título vacío                               |
|  `401` | Limpia la sesión y redirige al login               | Eliminar el token y consultar una ruta protegida            |
|  `403` | Muestra acceso denegado                            | Entrar a usuarios como `USUARIO`                            |
|  `404` | Muestra recurso no encontrado                      | Abrir un ticket inexistente                                 |
|  `409` | Muestra mensaje de conflicto                       | Ejecutar una operación que el backend defina como conflicto |
|    `0` | Muestra error de conexión                          | Detener el backend y recargar la página                     |

El backend utiliza `ApiExceptionHandler` para devolver respuestas con una estructura común: `timestamp`, `status`, `error`, `message` y `path`.

### 7. Autenticación desde Angular y JWT

El flujo de autenticación es el siguiente:

1. `LoginComponent` valida el formulario.
2. `AuthService` envía `POST /api/auth/login`.
3. Spring Boot autentica el email y contraseña mediante `AuthenticationManager`.
4. El backend genera un JWT firmado con sujeto, rol y expiración.
5. Angular guarda el token y los datos de sesión en `sessionStorage`.
6. `authInterceptor` agrega `Authorization: Bearer <token>` a las peticiones protegidas.
7. `JwtAuthenticationFilter` valida la firma y expiración en el backend.
8. `SecurityConfig` autoriza la operación según el rol.

Archivos principales:

```text
frontend/src/app/features/auth/login.component.ts
frontend/src/app/core/auth/auth.service.ts
frontend/src/app/core/http/auth.interceptor.ts
backend/src/main/java/com/utp/sistema_tickets/controller/AuthController.java
backend/src/main/java/com/utp/sistema_tickets/security/JwtService.java
backend/src/main/java/com/utp/sistema_tickets/security/JwtAuthenticationFilter.java
backend/src/main/java/com/utp/sistema_tickets/security/SecurityConfig.java
```

Un `401` indica que falta autenticación o que el token no es válido. Un `403` indica que el token es válido, pero el rol no tiene permiso para ejecutar la operación.

### 8. Evidencia end-to-end

1. Iniciar MySQL y el backend Spring Boot.
2. Iniciar Angular y abrir `http://localhost:4200`.
3. Iniciar sesión como `USUARIO`.
4. Crear un ticket desde la pantalla web.
5. Mostrar en Network el `POST /api/tickets` con respuesta `201 Created`.
6. Confirmar que el ticket aparece en el listado y abrir su detalle.
7. Consultar MySQL y mostrar la fila persistida en `tickets`.
8. Iniciar sesión como `TECNICO` y cambiar el estado agregando una observación.
9. Mostrar el `PATCH /api/tickets/{id}/estado` y el nuevo evento en el historial.
10. Consultar `ticket_historial` en MySQL.
11. Iniciar sesión como `ADMIN` y demostrar asignación, edición, eliminación o gestión de usuarios/categorías.

Esta secuencia evidencia el recorrido:

```text
Angular → HttpClient → JWT → Spring Boot → Service → JPA → MySQL
      ← respuesta HTTP ← DTO ← Controller ←
```

## Arquitectura del frontend

```text
frontend/src/app/
├── core/
│   ├── auth/       # Sesión y JWT
│   ├── guards/     # Autenticación y roles
│   ├── http/       # Interceptores y errores
│   ├── layout/     # Shell y navegación
│   └── services/   # Consumo de la API
├── features/
│   ├── auth/       # Login y registro
│   ├── dashboard/  # Métricas y actividad
│   ├── tickets/    # CRUD y detalle
│   └── admin/      # Usuarios y categorías
└── shared/
    ├── models/     # Interfaces TypeScript
    └── ui/         # Componentes visuales reutilizables
```

## Rutas principales

| Ruta                     | Funcionalidad                 | Acceso             |
| ------------------------ | ----------------------------- | ------------------ |
| `/login`                 | Iniciar sesión                | Público            |
| `/registro`              | Registrar usuario             | Público            |
| `/dashboard`             | Resumen de soporte            | Autenticado        |
| `/tickets`               | Listar y filtrar tickets      | Autenticado        |
| `/tickets/nuevo`         | Crear ticket                  | `USUARIO`, `ADMIN` |
| `/tickets/:id`           | Detalle, historial y acciones | Según permisos     |
| `/tickets/:id/editar`    | Editar ticket                 | `ADMIN`            |
| `/tickets/mis-asignados` | Tickets asignados             | `TECNICO`          |
| `/admin/usuarios`        | Gestionar usuarios            | `ADMIN`            |
| `/admin/categorias`      | Gestionar categorías          | `ADMIN`            |

## Requisitos

- Java 21.
- MySQL ejecutándose en `localhost:3306`.
- Node.js y npm.
- Maven Wrapper incluido en `backend/`.

## Ejecución

### Backend

Desde una terminal:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

La API estará disponible en `http://localhost:8080`.

### Frontend

Desde otra terminal:

```powershell
cd frontend
npm install
npm start
```

La aplicación estará disponible en `http://localhost:4200` y utilizará la API configurada en:

```text
frontend/src/environments/environment.ts
```

## Pruebas

Compilar el frontend:

```powershell
cd frontend
npm run build
```

Ejecutar las pruebas del backend:

```powershell
cd backend
.\mvnw.cmd test
```
