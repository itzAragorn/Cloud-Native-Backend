# 🔐 Guía de Autenticación JWT para AWS

## Resumen de Cambios

Se ha reemplazado la autenticación de **Azure AD** con **JWT genérico** para ser compatible con la arquitectura de **AWS**. El sistema ahora usa tokens JWT firmados con una clave secreta que se pueden generar y validar localmente y en cualquier entorno en AWS.

---

## 🏗️ Arquitectura JWT

### Flujo de Autenticación

```
┌─────────────┐
│   Frontend  │ (React/Angular)
│  (Puerto    │
│   3000)     │
└──────┬──────┘
       │
       │ POST /api/v1/auth/login
       │ {email, password}
       │
       ▼
┌──────────────────────────────────┐
│   BFF-Service (Puerto 8080)      │
│  ┌────────────────────────────┐  │
│  │  AuthController            │  │
│  │  - /api/v1/auth/login      │  │
│  │  - /api/v1/auth/verify     │  │
│  │  - /api/v1/auth/logout     │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │  JwtTokenProvider          │  │
│  │  - Genera JWT token        │  │
│  │  - Valida token            │  │
│  │  - Extrae claims           │  │
│  └────────────────────────────┘  │
│  ┌────────────────────────────┐  │
│  │  JwtAuthenticationFilter   │  │
│  │  - Valida en cada request  │  │
│  │  - Extrae usuario del JWT  │  │
│  └────────────────────────────┘  │
└──────────┬───────────────────────┘
           │
           │ Respuesta:
           │ {token, usuarioId, rol, expiresIn}
           │
           ▼
┌─────────────────────────────────────┐
│   Frontend                          │
│   Guarda token en localStorage      │
│   Usa en header: Authorization:     │
│   Bearer <token>                    │
└─────────────────────────────────────┘
       │
       │ GET/POST /api/v1/...
       │ Header: Authorization: Bearer <token>
       │
       ▼
┌──────────────────────────────────┐
│   BFF-Service                    │
│   JwtAuthenticationFilter        │
│   ✓ Extrae token del header      │
│   ✓ Valida firma JWT             │
│   ✓ Extrae usuario y rol         │
│   ✓ Valida expiración            │
│   ✓ Crea SecurityContext         │
└──────────┬───────────────────────┘
           │
           │ Si es válido → Procesa request
           │ Si es inválido → 401 Unauthorized
           ▼
┌─────────────────────────────────────────┐
│   Rutas a otros microservicios:         │
│   - /api/v1/fondos → fondos-service     │
│   - /api/v1/inversiones → inversiones   │
│   - /api/v1/usuarios → usuarios         │
└─────────────────────────────────────────┘
```

---

## 📋 Configuración JWT (application.properties)

```properties
# ===========================
# JWT Configuration for AWS
# ===========================
jwt.secret=BancoCloud-Super-Secret-Key-Change-In-Production
jwt.expiration=86400000                  # 24 horas en ms
jwt.issuer=bancoclod                     # Emisor del token
jwt.audience=bancoclod-app               # Audiencia del token
```

### ⚠️ IMPORTANTE PARA PRODUCCIÓN

En **AWS**, la clave secreta debe:
1. **Cambiar en cada ambiente** (dev, staging, prod)
2. **Ser almacenada en AWS Secrets Manager**
3. **Tener al menos 32 caracteres**
4. **Ser diferente en cada región de AWS**

Ejemplo para AWS Secrets Manager:
```bash
aws secretsmanager create-secret \
  --name bancoclod/jwt-secret \
  --secret-string "Tu-Clave-Secreta-Super-Segura-De-MinimoBancoCloudProd123456789"
```

---

## 🔑 Componentes JWT

### 1. **JwtTokenProvider** 
Clase responsable de generar y validar JWT tokens

**Métodos principales:**
```java
// Generar token para usuario autenticado
String generateToken(Long usuarioId, String email, String rol)

// Validar que el token es correcto
boolean validateToken(String token)

// Extraer información del token
Long getUserIdFromToken(String token)
String getEmailFromToken(String token)
String getRoleFromToken(String token)
Claims getClaimsFromToken(String token)
```

**Ejemplo de token generado:**
```
eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJqdWFuQGV4YW1wbGUuY29tIiwidXN1YXJpb0lkIjoxLCJlbWFpbCI6Imp1YW5AZXhhbXBsZS5jb20iLCJyb2wiOiJDTElFTlRFIiwiaXNzIjoiYmFuY29jbG9kIiwiYXVkIjoiYmFuY29jbG9kLWFwcCIsImlhdCI6MTcwNTMxNDIwMCwiZXhwIjoxNzA1NDAwNjAwfQ.rJxY...
```

**Contenido del token (JWT Payload):**
```json
{
  "sub": "juan@example.com",           // Usuario (email)
  "usuarioId": 1,                      // ID del usuario
  "email": "juan@example.com",         // Email del usuario
  "rol": "CLIENTE",                    // Rol (ADMIN o CLIENTE)
  "iss": "bancoclod",                  // Emisor
  "aud": "bancoclod-app",              // Audiencia
  "iat": 1705314200,                   // Emitido en (timestamp)
  "exp": 1705400600                    // Expira en (timestamp)
}
```

### 2. **JwtAuthenticationFilter**
Filtro que valida JWT en cada request HTTP

**Proceso:**
1. Extrae el header `Authorization`
2. Verifica que comience con `Bearer `
3. Extrae el token
4. Valida la firma JWT
5. Extrae usuario, rol, id
6. Crea `SecurityContext` para Spring Security
7. Permite que el request continúe

**Manejo de errores:**
- Token expirado → 401 Unauthorized
- Token inválido → 401 Unauthorized
- Token ausente → 401 Unauthorized (para endpoints protegidos)

### 3. **AuthenticationService**
Servicio que maneja la lógica de login

**Proceso:**
1. Recibe email y password
2. Consulta usuarios-service por email
3. Valida que usuario esté ACTIVO
4. Valida que tenga acceso autorizado
5. Genera JWT token
6. Devuelve token al cliente

**Validaciones:**
- ✓ Email debe existir
- ✓ Usuario debe estar en estado ACTIVO
- ✓ Usuario debe tener acceso autorizado
- ✓ (En producción: validar password con bcrypt)

### 4. **AuthController**
Controlador REST para autenticación

**Endpoints:**

#### POST `/api/v1/auth/login`
Autentica usuario y genera JWT

**Request:**
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "password123"
  }'
```

**Response 200 OK:**
```json
{
  "usuarioId": 1,
  "email": "juan@example.com",
  "rol": "CLIENTE",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "mensaje": "Login exitoso"
}
```

**Response 401 Unauthorized:**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 401,
  "error": "UNAUTHORIZED",
  "message": "Email o contraseña incorrectos",
  "path": "/api/v1/auth/login",
  "errorCode": "AUTH_FAILED"
}
```

#### GET `/api/v1/auth/verify`
Verifica que el token es válido

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/auth/verify \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response 200 OK:**
```json
{
  "válido": true,
  "mensaje": "Token válido"
}
```

#### GET `/api/v1/auth/logout`
Realiza logout (cliente debe eliminar token local)

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer eyJhbGciOiJIUzUxMiJ9..."
```

**Response 200 OK:**
```json
{
  "mensaje": "Logout exitoso"
}
```

---

## 🔒 Cómo Funciona la Validación

### 1. Cliente hace login
```
POST /api/v1/auth/login
{
  "email": "juan@example.com",
  "password": "password123"
}
```

### 2. BFF-Service genera JWT
```
JwtTokenProvider.generateToken(1, "juan@example.com", "CLIENTE")
↓
token = Jwts.builder()
  .setSubject("juan@example.com")
  .claim("usuarioId", 1)
  .claim("rol", "CLIENTE")
  .setExpiration(new Date(24 horas))
  .signWith(secret, HS512)
  .compact()
```

### 3. Cliente guarda token
```javascript
// Frontend
localStorage.setItem('token', response.token);
```

### 4. Cliente hace requests con token
```javascript
// Cada request incluye:
headers: {
  'Authorization': 'Bearer eyJhbGciOiJIUzUxMiJ9...'
}
```

### 5. BFF valida token en cada request
```
JwtAuthenticationFilter.doFilter()
  ↓
  Extrae header Authorization
  ↓
  Valida firma JWT con clave secreta
  ↓
  Si válido: Extrae usuario, rol, ID
  ↓
  Crea SecurityContext
  ↓
  Spring Security permite acceso
```

### 6. Si token es inválido
```
401 Unauthorized
{
  "error": "UNAUTHORIZED",
  "message": "Token JWT inválido, expirado o ausente"
}

// Frontend debe:
// 1. Eliminar token
// 2. Redirigir a login
```

---

## 🧪 Pruebas Locales

### Paso 1: Iniciar BFF-Service
```bash
cd bff-service
mvn spring-boot:run
# Espera: Started BffServiceApplication on port 8080
```

### Paso 2: Crear usuario (con otro terminal)
```bash
# Primero iniciar usuarios-service en otro terminal
cd usuarios-service
mvn spring-boot:run
```

```bash
# Crear usuario
curl -X POST http://localhost:8083/api/v1/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Juan Pérez",
    "email": "juan@example.com",
    "rol": "CLIENTE"
  }'

# Respuesta:
# {"id": 1, "nombre": "Juan Pérez", "email": "juan@example.com", "accesoAutorizado": false, ...}
```

### Paso 3: Autorizar usuario (necesitas crear ADMIN primero)
```bash
# Crear ADMIN
curl -X POST http://localhost:8083/api/v1/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Admin User",
    "email": "admin@example.com",
    "rol": "ADMIN"
  }'

# El ADMIN nace autorizado automáticamente

# Autorizar CLIENTE
curl -X PUT http://localhost:8083/api/v1/usuarios/1/autorizar \
  -H "Content-Type: application/json" \
  -d '{"autorizado": true}'
```

### Paso 4: Login y obtener JWT
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "juan@example.com",
    "password": "cualquierpassword"
  }'

# Respuesta:
# {
#   "usuarioId": 1,
#   "email": "juan@example.com",
#   "rol": "CLIENTE",
#   "token": "eyJhbGciOiJIUzUxMiJ9...",
#   "tokenType": "Bearer",
#   "expiresIn": 86400
# }
```

### Paso 5: Usar token para acceder a endpoints protegidos
```bash
# Guardar token
TOKEN="eyJhbGciOiJIUzUxMiJ9..."

# Acceder a fondos-service a través de BFF
curl -X GET http://localhost:8080/api/v1/fondos \
  -H "Authorization: Bearer $TOKEN"
```

### Paso 6: Verificar token válido
```bash
curl -X GET http://localhost:8080/api/v1/auth/verify \
  -H "Authorization: Bearer $TOKEN"

# Respuesta:
# {"válido": true, "mensaje": "Token válido"}
```

### Paso 7: Test con token expirado/inválido
```bash
# Token inválido
curl -X GET http://localhost:8080/api/v1/fondos \
  -H "Authorization: Bearer invalid-token"

# Respuesta 401:
# {
#   "timestamp": "2024-01-15T10:30:00",
#   "status": 401,
#   "error": "UNAUTHORIZED",
#   "message": "Token JWT inválido, expirado o ausente"
# }
```

---

## 🚀 Deployment en AWS

### 1. Crear Secret en AWS Secrets Manager
```bash
aws secretsmanager create-secret \
  --name /bancoclod/prod/jwt-secret \
  --secret-string "Tu-Clave-Secreta-De-Al-Menos-32-Caracteres-Para-Prod"
```

### 2. Configurar en application.properties (AWS)
```properties
# Usar AWS Secrets Manager para la clave
spring.cloud.aws.secrets-manager.enabled=true
spring.cloud.aws.secrets-manager.prefix=/bancoclod/prod

jwt.secret=${JWT_SECRET:default-secret-for-dev}
jwt.expiration=86400000
jwt.issuer=bancoclod
jwt.audience=bancoclod-app
```

### 3. O usar variables de entorno en ECS/Lambda
```bash
export JWT_SECRET="Tu-Clave-Secreta-Para-AWS"
export JWT_EXPIRATION=86400000
export JWT_ISSUER=bancoclod
export JWT_AUDIENCE=bancoclod-app
```

### 4. Usar en Docker/Kubernetes
```dockerfile
FROM openjdk:17-slim
ARG JWT_SECRET
ARG JWT_EXPIRATION

ENV JWT_SECRET=$JWT_SECRET
ENV JWT_EXPIRATION=$JWT_EXPIRATION

COPY app.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
```

---

## 🔐 Seguridad

### ✅ Lo que hace bien JWT:
- Token es autosuficiente (stateless)
- No requiere sesión en servidor
- Funciona bien en microservicios
- Compatible con AWS (Lambda, ECS, etc.)
- Puede reutilizarse en múltiples servidores

### ⚠️ Consideraciones de Seguridad:
1. **Clave secreta fuerte**: Mínimo 32 caracteres aleatorios
2. **HTTPS obligatorio**: Los tokens nunca sobre HTTP
3. **Expiración**: 24 horas (ajustable según necesidad)
4. **Refresh tokens**: Implementar en fase siguiente si es necesario
5. **CORS configurado**: Solo orígenes permitidos
6. **Rate limiting**: Proteger endpoint de login contra fuerza bruta
7. **Revocación**: Lista negra de tokens en caso necesario (Redis)

### 🛡️ Protecciones Implementadas:
- ✓ Validación de firma JWT
- ✓ Validación de expiración
- ✓ Extracción segura de claims
- ✓ CORS configurado
- ✓ CSRF deshabilitado (stateless)
- ✓ Sesiones stateless (no cookies de sesión)
- ✓ Roles basados en rol JWT

---

## 📝 Cambios vs Azure AD

### Antes (Azure AD - ❌ Eliminado)
```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=https://login.microsoftonline.com/...
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=https://login.microsoftonline.com/...
```
- Dependencia de Azure AD
- Requiere registrar aplicación en Azure
- No funciona en AWS

### Ahora (JWT Genérico - ✅ Nuevo)
```properties
jwt.secret=BancoCloud-Super-Secret-Key-Change-In-Production
jwt.expiration=86400000
jwt.issuer=bancoclod
jwt.audience=bancoclod-app
```
- Completamente independiente
- Funciona en cualquier cloud (AWS, Azure, GCP, on-premise)
- Clave secreta centralizada
- Mejor para microservicios

---

## 🔄 Flujo Completo en Postman

### Colección para pruebas:

1. **Login**
   ```
   POST http://localhost:8080/api/v1/auth/login
   Body:
   {
     "email": "juan@example.com",
     "password": "password123"
   }
   ```
   Guardar token en variable: `{{token}}`

2. **Verificar token**
   ```
   GET http://localhost:8080/api/v1/auth/verify
   Header: Authorization: Bearer {{token}}
   ```

3. **Acceder a fondos**
   ```
   GET http://localhost:8080/api/v1/fondos
   Header: Authorization: Bearer {{token}}
   ```

4. **Logout**
   ```
   GET http://localhost:8080/api/v1/auth/logout
   Header: Authorization: Bearer {{token}}
   ```

---

## ❓ Preguntas Frecuentes

### ¿Qué pasa si el token expira?
El cliente recibe un 401. Debe hacer login nuevamente para obtener un nuevo token. (En fase siguiente podemos implementar refresh tokens)

### ¿Dónde se valida el token?
En el `JwtAuthenticationFilter` - en CADA request que llegue al BFF-Service antes de cualquier controlador.

### ¿Se puede cambiar la expiración?
Sí, en `jwt.expiration`. El valor está en milisegundos. Ej: 3600000 = 1 hora

### ¿Se puede revocar un token antes de expirar?
Con JWT puro, no. Para eso necesitarías:
- Redis con lista negra de tokens
- Base de datos de tokens revocados
- (Implementable en fase siguiente)

### ¿Funciona con Swagger?
Sí, pero necesita configuración adicional en Springdoc-OpenAPI (pendiente para fase siguiente)

---

## 📚 Referencias

- [JWT.io](https://jwt.io) - Decodificador online de JWT
- [JJWT](https://github.com/jwtk/jjwt) - Librería JWT para Java
- [Spring Security Docs](https://spring.io/projects/spring-security)
- [AWS Secrets Manager](https://docs.aws.amazon.com/secretsmanager/)

