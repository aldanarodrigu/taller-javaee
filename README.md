> **💡 Tip:** Estás viendo el archivo en modo texto plano.  
> Para ver este README con formato (títulos, tablas, código), presioná **`Ctrl + Shift + V`** en VS Code.  
> O bien **`Ctrl + K` → `V`** para abrirlo en panel lateral junto al código.

# AppChat — Guía de Ejecución

Aplicación de comunicación corporativa con mensajería en tiempo real, cifrado E2E y gestión de comunidades.

---

## Requisitos previos

| Herramienta | Versión mínima |
|---|---|
| Java JDK | 17 LTS |
| Maven | 3.x |
| Payara Server | 6.x (Jakarta EE 10) |
| Node.js | 18+ |
| ngrok | v3.x (cuenta con authtoken configurado) |

---

## 1. Levantar el backend

### 1.1 Compilar y desplegar

```bash
# Desde la raíz del proyecto backend (donde está pom.xml)
cd C:\Users\Carlos\Documents\NetBeansProjects\JAVAEE
mvn clean package

# El WAR se genera en: target/appchat.war
# Copiarlo al directorio de despliegue de Payara:
copy target\appchat.war C:\Users\Carlos\Payara_Server\glassfish\domains\domain1\autodeploy\
```

O desplegarlo desde **NetBeans** con Run → Deploy.

### 1.2 Verificar que levantó

```
http://localhost:8080/appchat/api/auth/login  → debe responder (aunque sea 405)
http://localhost:8080/appchat/swagger/        → Swagger UI
```

---

## 2. Levantar los túneles ngrok

Se necesitan **dos túneles** simultáneos (backend en puerto 8080, frontend en puerto 3000).

Abrir **dos terminales separadas**:

```bash
# Terminal 1 — túnel backend
ngrok http 8080

# Terminal 2 — túnel frontend
ngrok http 3000
```

Anotar las URLs HTTPS que genera ngrok, por ejemplo:
- Backend: `https://XXXX-XXX-XXX-XXX-XXX.ngrok-free.app`
- Frontend: `https://YYYY-YYY-YYY-YYY-YYY.ngrok-free.app`

---

## 3. ⚠️ Cambiar las URLs de ngrok (obligatorio en cada reinicio)

Las URLs de ngrok **cambian en cada reinicio** si no tenés un plan pago. Hay **un solo archivo** que actualizar en el frontend:

### Archivo: `appchat-frontend/src/services/config.js`

```javascript
const CONFIG = {
    BASE_URL: 'https://<URL-BACKEND-NGROK>/appchat/api',   // ← cambiar por URL del túnel del backend
    WS_URL:   'wss://<URL-BACKEND-NGROK>/appchat/chat'     // ← mismo host, protocolo wss://
};

export default CONFIG;
```

**Ejemplo con URLs reales:**
```javascript
const CONFIG = {
    BASE_URL: 'https://2e92-148-227-105-176.ngrok-free.app/appchat/api',
    WS_URL:   'wss://2e92-148-227-105-176.ngrok-free.app/appchat/chat'
};
```

> `BASE_URL` y `WS_URL` apuntan siempre al **mismo túnel** (el del backend, puerto 8080).  
> Solo cambia el protocolo: `https://` para REST, `wss://` para WebSocket.

---

## 4. Levantar el frontend

```bash
cd C:\Users\Carlos\Documents\NetBeansProjects\appchat-frontend
npm start
```

El frontend arranca en `http://localhost:3000`.  
Desde ngrok se accede en la URL del túnel del frontend (puerto 3000).

---

## 5. Variables de entorno del frontend

Archivo: `appchat-frontend/.env.local` (no subir a git)

```env
REACT_APP_SUPABASE_URL=https://hnqtqnzvlwiggawffmuu.supabase.co
REACT_APP_SUPABASE_ANON_KEY=<anon key de Supabase>
DANGEROUSLY_DISABLE_HOST_CHECK=true
```

- `DANGEROUSLY_DISABLE_HOST_CHECK=true` es necesario para que ngrok no bloquee el dev server de React.
- Sin `REACT_APP_SUPABASE_URL` y `REACT_APP_SUPABASE_ANON_KEY` no funcionan las subidas de archivos ni fotos de perfil.

---

## 6. Configuración del backend (sin .env)

El backend no usa archivo `.env`. La configuración está en:

| Qué | Dónde |
|---|---|
| URL de base de datos (Supabase PostgreSQL) | `src/main/webapp/WEB-INF/glassfish-resources.xml` |
| Clave secreta JWT | `src/main/java/com/appchat/security/JwtUtil.java` |
| CORS permitidos | `src/main/java/com/appchat/security/CorsServletFilter.java` |

---

## 7. Deploy local (sin ngrok)

Para correr todo en la misma máquina sin exponer a internet.

### 7.1 Backend

Igual que la sección 1 — desplegar el WAR en Payara local, queda en `http://localhost:8080/appchat/api`.

### 7.2 Frontend — apuntar a localhost

Editar `appchat-frontend/src/services/config.js`:

```javascript
const CONFIG = {
    BASE_URL: 'http://localhost:8080/appchat/api',
    WS_URL:   'ws://localhost:8080/appchat/chat'
};

export default CONFIG;
```

> Notar que en local se usa `http://` y `ws://` (sin la `s` de seguro), ya que no hay TLS.

### 7.3 Frontend — quitar la restricción de host check

En `appchat-frontend/.env.local` asegurarse de tener (o crear el archivo si no existe):

```env
REACT_APP_SUPABASE_URL=https://hnqtqnzvlwiggawffmuu.supabase.co
REACT_APP_SUPABASE_ANON_KEY=<anon key de Supabase>
```

`DANGEROUSLY_DISABLE_HOST_CHECK` no es necesario en local, se puede omitir.

### 7.4 CORS en el backend

El backend acepta `*` en los headers CORS (`CorsServletFilter.java`), por lo que no hay cambios necesarios para correr en local.

### 7.5 Levantar el frontend

```bash
cd C:\Users\Carlos\Documents\NetBeansProjects\appchat-frontend
npm start
```

Acceder en: `http://localhost:3000`

### 7.6 Resumen config.js según entorno

| Entorno | BASE_URL | WS_URL |
|---|---|---|
| **Local** | `http://localhost:8080/appchat/api` | `ws://localhost:8080/appchat/chat` |
| **ngrok** | `https://<host-ngrok>/appchat/api` | `wss://<host-ngrok>/appchat/chat` |

---

## 8. Resumen de archivos a tocar por IP de ngrok

| Cuándo | Archivo | Qué cambiar |
|---|---|---|
| **Cambia IP ngrok** | `appchat-frontend/src/services/config.js` | `BASE_URL` y `WS_URL` con la nueva URL del túnel backend |
| Cambia Supabase project | `appchat-frontend/.env.local` | `REACT_APP_SUPABASE_URL` y `REACT_APP_SUPABASE_ANON_KEY` |
| Cambia base de datos | `WEB-INF/glassfish-resources.xml` | `serverName`, `user`, `password`, `databaseName` |

---

## 8. Orden de inicio recomendado

```
1. Iniciar Payara Server
2. Desplegar appchat.war
3. Ejecutar: ngrok http 8080    (anotar URL backend)
4. Actualizar config.js con la nueva URL
5. Ejecutar: ngrok http 3000
6. Ejecutar: npm start  (en appchat-frontend/)
7. Abrir la URL del túnel frontend en el browser
```

