# AppChat — Documentación de la API

> **Swagger UI interactivo:** `http://localhost:8080/appchat/swagger/`  
> Desde ahí podés probar cada endpoint directamente en el navegador.

---

## Autenticación

Todos los endpoints (excepto `/auth/*`) requieren un **Bearer Token JWT** en la cabecera:

```
Authorization: Bearer <token>
```

El token se obtiene en `POST /auth/login` y tiene una duración configurada en `JwtUtil`.

---

## Base URL

| Entorno | URL base |
|---------|----------|
| Local   | `http://localhost:8080/appchat/api` |
| ngrok   | `https://<id>.ngrok-free.app/appchat/api` |

---

## Módulos

- [Auth](#auth)
- [Usuarios](#usuarios)
- [Comunidades](#comunidades)
- [Chats (directos)](#chats-directos)
- [Chat Grupos](#chat-grupos)
- [WebSocket](#websocket)

---

## Auth

### `POST /auth/registro`

Registra un nuevo usuario.

**Body:**
```json
{
  "email": "usuario@empresa.com",
  "password": "secret123",
  "username": "carlos",
  "nombre": "Carlos"
}
```

**Respuesta `201`:**
```json
{ "id": 42 }
```

---

### `POST /auth/login`

Autentica un usuario y devuelve el JWT.

**Body:**
```json
{
  "email": "usuario@empresa.com",
  "password": "secret123"
}
```

**Respuesta `200`:**
```json
{ "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." }
```

---

## Usuarios

> Todos requieren `Authorization: Bearer <token>`

### `GET /usuarios`

Lista todos los usuarios registrados.

**Respuesta `200`:** array de `UsuarioResponseDTO`

---

### `GET /usuarios/buscar?q={término}`

Busca usuarios por nombre o username.

| Param | Tipo | Descripción |
|-------|------|-------------|
| `q`   | string (query) | Término de búsqueda (obligatorio) |

**Respuesta `200`:** array de `UsuarioResponseDTO`

---

### `GET /usuarios/{id}`

Obtiene un usuario por su ID.

**Respuesta `200`:** `UsuarioResponseDTO`  
**Respuesta `404`:** usuario no encontrado

---

### `PUT /usuarios/{id}`

Actualiza los datos del perfil.

**Body:**
```json
{
  "nombre": "Carlos López",
  "username": "carlosl",
  "bio": "...",
  "foto": "https://..."
}
```

**Respuesta `200`:** `UsuarioResponseDTO`

---

### `PUT /usuarios/{id}/estado`

Cambia el estado de presencia. Solo el propio usuario puede modificarlo.

**Body:**
```json
{ "estado": "EN_LINEA" }
```

Valores posibles: `EN_LINEA`, `OCUPADO`, `INVISIBLE`, `DESCONECTADO`

**Respuesta `200`:** `UsuarioResponseDTO`  
**Respuesta `403`:** intento de modificar otro usuario

---

### `PUT /usuarios/{id}/publicKey`

Guarda la clave pública E2E del usuario (ECDH P-256, formato Base64). Solo el propio usuario puede actualizarla.

**Content-Type:** `text/plain`  
**Body:** string Base64 con la clave pública

**Respuesta `204`:** sin contenido  
**Respuesta `403`:** intento de modificar otro usuario

---

### `POST /usuarios/{id}/foto`

Sube una foto de perfil.

**Content-Type:** `multipart/form-data`  
**Body:** archivo de imagen (máx. 5 MB)

**Respuesta `200`:**
```json
{ "url": "data:image/jpeg;base64,..." }
```

---

## Comunidades

> Todos requieren `Authorization: Bearer <token>`

### `POST /comunidades`

Crea una nueva comunidad. El usuario autenticado queda como owner.

**Body:**
```json
{
  "nombre": "Equipo Backend",
  "descripcion": "Canal del equipo de backend"
}
```

**Respuesta `201`:** sin cuerpo

---

### `GET /comunidades`

Lista las comunidades a las que pertenece el usuario autenticado.

**Respuesta `200`:** array de `ComunidadResumenDTO`

---

### `GET /comunidades/{id}`

Obtiene el detalle de una comunidad (incluye miembros y chats).

**Respuesta `200`:** `ComunidadDetalleDTO`

---

### `PUT /comunidades/{id}`

Edita nombre/descripción de la comunidad. Solo el owner puede hacerlo.

**Body:**
```json
{
  "nombre": "Equipo Backend v2",
  "descripcion": "..."
}
```

**Respuesta `200`:** `Comunidad`

---

### `DELETE /comunidades/{id}`

Elimina la comunidad. Solo el owner puede hacerlo.

**Respuesta `204`:** sin contenido

---

### `GET /comunidades/{id}/miembros`

Lista los miembros de una comunidad.

**Respuesta `200`:** array de `UsuarioResponseDTO`

---

### `DELETE /comunidades/{id}/miembros/{userId}`

Expulsa a un miembro de la comunidad. Solo el owner puede hacerlo.

**Respuesta `204`:** sin contenido

---

### `DELETE /comunidades/{id}/salir`

El usuario autenticado abandona la comunidad.

**Respuesta `204`:** sin contenido

---

### `POST /comunidades/{id}/invitar`

Invita a un usuario a la comunidad por su username.

**Body:**
```json
{ "username": "carlosl" }
```

**Respuesta `200`:** `"Invitacion Enviada"`

---

### `GET /comunidades/invitaciones/pendientes`

Lista las invitaciones pendientes del usuario autenticado.

**Respuesta `200`:** array de objetos con detalle de invitación

---

### `PUT /comunidades/invitaciones/{id}/aceptar`

Acepta una invitación pendiente.

**Respuesta `204`:** sin contenido

---

### `PUT /comunidades/invitaciones/{id}/rechazar`

Rechaza una invitación pendiente.

**Respuesta `204`:** sin contenido

---

## Chats (directos)

> Todos requieren `Authorization: Bearer <token>`

### `GET /chats/comunidad/{comunidadId}`

Lista los chats visibles para el usuario dentro de una comunidad.

**Respuesta `200`:** array de `ChatResumenDTO`

---

### `POST /chats`

Crea o abre un chat directo entre dos usuarios dentro de una comunidad.

**Body:**
```json
{
  "usuarioDestinoId": 7,
  "comunidadId": 3
}
```

**Respuesta `201`:** `ChatResumenDTO`

---

### `GET /chats/{id}/mensajes`

Historial de mensajes paginado.

| Param  | Tipo | Default | Descripción |
|--------|------|---------|-------------|
| `page` | int  | `0`     | Número de página |
| `size` | int  | `20`    | Mensajes por página |

**Respuesta `200`:** `HistorialMensajesDTO`

---

### `POST /chats/{id}/mensajes/{mensajeId}/pin`

Fija un mensaje en el chat.

**Respuesta `204`:** sin contenido

---

### `DELETE /chats/{id}/mensajes/{mensajeId}/pin`

Desfija un mensaje del chat.

**Respuesta `204`:** sin contenido

---

### `GET /chats/{id}/mensajes/pin`

Lista los mensajes fijados de un chat.

**Respuesta `200`:** array de `MensajeDTO`

---

### `POST /chats/{id}/adjuntos`

Envía un mensaje con archivo adjunto (almacenado en Supabase Storage).

**Body:**
```json
{
  "fileName": "documento.pdf",
  "mimeType": "application/pdf",
  "base64": "<base64 del archivo>"
}
```

**Respuesta `201`:** `MensajeDTO`

---

### `GET /chats/adjuntos/{mensajeId}`

Descarga el archivo adjunto de un mensaje.

**Respuesta `200`:** stream binario (`application/octet-stream`)  
Headers de respuesta:
- `Content-Disposition`: `inline` o `attachment` según el tipo
- `Cache-Control`: `private, max-age=120`

---

## Chat Grupos

> Todos requieren `Authorization: Bearer <token>`  
> Base path: `/chat`

### `POST /chat`

Crea un chat grupal dentro de una comunidad.

**Body:**
```json
{
  "nombre": "general",
  "comunidadId": 3,
  "miembrosIds": [1, 2, 5]
}
```

**Respuesta `201`:** `ChatResumenDTO`

---

### `POST /chat/{id}/miembros`

Agrega miembros a un grupo existente.

**Body:**
```json
{ "miembrosIds": [8, 9] }
```

**Respuesta `200`:** `ChatResumenDTO`

---

### `PUT /chat/{id}`

Edita el nombre u otros datos del grupo.

**Body:**
```json
{ "nombre": "general-v2" }
```

**Respuesta `200`:** `ChatResumenDTO`

---

### `PUT /chat/{id}/roles`

Cambia el rol de un miembro del grupo.

**Body:**
```json
{
  "usuarioId": 5,
  "rol": "ADMIN"
}
```

**Respuesta `200`:** `ChatResumenDTO`

---

### `DELETE /chat/{id}/miembros/{userId}`

Elimina a un miembro del grupo.

**Respuesta `204`:** sin contenido

---

### `DELETE /chat/{id}`

Elimina el grupo completo.

**Respuesta `204`:** sin contenido

---

### `POST /chat/{id}/mensajes/{mensajeId}/pin`

Fija un mensaje en el grupo.

**Respuesta `204`:** sin contenido

---

### `DELETE /chat/{id}/mensajes/{mensajeId}/pin`

Desfija un mensaje del grupo.

**Respuesta `204`:** sin contenido

---

### `GET /chat/{id}/mensajes/pin`

Lista los mensajes fijados del grupo.

**Respuesta `200`:** array de `MensajeFijadoDTO`

---

## WebSocket

### `wss://<host>/appchat/chat/{chatId}`

Conexión en tiempo real para mensajería.

**Query param requerido:** `token=<JWT>`

**Ejemplo de conexión:**
```
wss://localhost:8080/appchat/chat/5?token=eyJhbGc...
```

**Mensaje enviado (cliente → servidor):**
```json
{
  "contenido": "Hola!",
  "tipo": "TEXTO"
}
```

Para mensajes E2E cifrados, `contenido` es un JSON serializado con el formato:
```json
{
  "e2e": true,
  "iv": "<base64>",
  "ct": "<base64>",
  "spk": "<senderPublicKeyBase64>",
  "keys": {
    "<userId>": "<wrapIv>.<wrappedKeyBase64>"
  }
}
```

**Mensaje recibido (servidor → cliente):**
```json
{
  "id": 123,
  "chatId": 5,
  "emisorId": 2,
  "contenido": "...",
  "tipo": "TEXTO",
  "fechaEnvio": "2026-07-01T10:30:00",
  "reacciones": []
}
```
