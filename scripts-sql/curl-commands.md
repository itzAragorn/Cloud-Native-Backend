# 🧪 Comandos cURL para Probar Endpoints - Banco Cloud

Este archivo contiene comandos cURL listos para copiar y ejecutar en la terminal para probar los endpoints.

## 🔧 Configuración Previa

```bash
# Establecer la URL base
export API_URL="http://localhost:8090"

# Usuarios de prueba
export ADMIN_EMAIL="admin@bancocloud.com"
export ADMIN_PASSWORD="Admin@123456"

export CLIENT_EMAIL="cliente2@bancocloud.com"
export CLIENT_PASSWORD="Cliente456@789"

export UNAUTHORIZED_EMAIL="cliente1@bancocloud.com"
export UNAUTHORIZED_PASSWORD="Cliente123@456"
```

---

## 🔐 Autenticación

### 1. Login Admin
```bash
curl -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$ADMIN_EMAIL\",
    \"password\": \"$ADMIN_PASSWORD\"
  }" | jq '.'
```

**Respuesta esperada:**
```json
{
  "usuarioId": 1,
  "email": "admin@bancocloud.com",
  "rol": "ADMIN",
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "mensaje": "Login exitoso"
}
```

### 2. Login Cliente (Autorizado)
```bash
curl -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$CLIENT_EMAIL\",
    \"password\": \"$CLIENT_PASSWORD\"
  }" | jq '.'
```

### 3. Login Cliente (No Autorizado) - Debe fallar
```bash
curl -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$UNAUTHORIZED_EMAIL\",
    \"password\": \"$UNAUTHORIZED_PASSWORD\"
  }" | jq '.'
```

**Respuesta esperada:** Error 401 "Usuario no autorizado para acceder al sistema"

### 4. Login con Password Incorrecto
```bash
curl -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"$CLIENT_EMAIL\",
    \"password\": \"wrongpassword\"
  }" | jq '.'
```

**Respuesta esperada:** Error 401 "Email o contraseña incorrectos"

### 5. Verificar Token
```bash
# Primero obtén un token válido
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | jq -r '.token')

# Luego verifica el token
curl -X GET "$API_URL/api/v1/auth/verify" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

---

## 👥 Usuarios (Admin Only)

### 6. Listar todos los usuarios
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/usuarios" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 7. Obtener usuario por ID
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/usuarios/2" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 8. Obtener usuario por Email
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/usuarios/email/$CLIENT_EMAIL" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 9. Obtener clientes pendientes de autorización
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/usuarios/clientes/pendientes" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 10. Autorizar usuario
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | jq -r '.token')

curl -X PUT "$API_URL/api/v1/usuarios/2/autorizar" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"autorizado\": true}" | jq '.'
```

### 11. Bloquear usuario
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | jq -r '.token')

curl -X PUT "$API_URL/api/v1/usuarios/4/bloquear" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 12. Desbloquear usuario
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$ADMIN_EMAIL\", \"password\": \"$ADMIN_PASSWORD\"}" | jq -r '.token')

curl -X PUT "$API_URL/api/v1/usuarios/4/desbloquear" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

---

## 💰 Fondos

### 13. Listar todos los fondos
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$CLIENT_EMAIL\", \"password\": \"$CLIENT_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/fondos" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 14. Obtener fondo por ID
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$CLIENT_EMAIL\", \"password\": \"$CLIENT_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/fondos/1" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 15. Listar solo fondos activos
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$CLIENT_EMAIL\", \"password\": \"$CLIENT_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/fondos?estado=ACTIVO" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

---

## 📈 Inversiones

### 16. Obtener inversiones del usuario
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$CLIENT_EMAIL\", \"password\": \"$CLIENT_PASSWORD\"}" | jq -r '.token')

# usuario_id = 3 (cliente2)
curl -X GET "$API_URL/api/v1/inversiones/usuario/3" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 17. Obtener inversión por ID
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$CLIENT_EMAIL\", \"password\": \"$CLIENT_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/inversiones/1" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### 18. Listar inversiones activas
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"$CLIENT_EMAIL\", \"password\": \"$CLIENT_PASSWORD\"}" | jq -r '.token')

curl -X GET "$API_URL/api/v1/inversiones?estado=ACTIVA" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

---

## 🏥 Health & Info

### 19. Health Check
```bash
curl -X GET "$API_URL/api/v1/health" | jq '.'
```

### 20. Información del Sistema
```bash
curl -X GET "$API_URL/api/v1/info" | jq '.'
```

---

## 📊 Resumen de Datos de Prueba

### Usuarios
| Email | Password | Rol | Autorizado | Estado |
|-------|----------|-----|-----------|--------|
| admin@bancocloud.com | Admin@123456 | ADMIN | ✓ | ACTIVO |
| cliente1@bancocloud.com | Cliente123@456 | CLIENTE | ✗ | ACTIVO |
| cliente2@bancocloud.com | Cliente456@789 | CLIENTE | ✓ | ACTIVO |
| cliente3@bancocloud.com | Carlos@123456 | CLIENTE | ✓ | BLOQUEADO |

### Fondos
| ID | Nombre | Valor Cuota | Estado |
|----|--------|------------|--------|
| 1 | Fondo Conservador Plus | $1,500.00 | ACTIVO |
| 2 | Fondo Moderado Equilibrado | $2,200.00 | ACTIVO |
| 3 | Fondo Agresivo Crecimiento | $3,500.00 | ACTIVO |
| 4 | Fondo Tecnología Global | $4,100.00 | ACTIVO |
| 5 | Fondo Renta Fija Corporativa | $1,100.00 | ACTIVO |
| 6 | Fondo Sostenible ESG | $2,800.00 | INACTIVO |

### Inversiones
**Cliente2 (usuario_id=3):** 4 inversiones activas por $285,000
**Cliente1 (usuario_id=2):** 4 inversiones (2 activas, 2 retiradas/canceladas) por $120,000

---

## 💡 Tips Útiles

### Guardar token en variable
```bash
TOKEN=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"admin@bancocloud.com\", \"password\": \"Admin@123456\"}" | jq -r '.token')

echo "Token: $TOKEN"
```

### Usar jq para extraer datos
```bash
# Extraer solo los emails
curl -s "$API_URL/api/v1/usuarios" \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | .email'

# Extraer solo los IDs
curl -s "$API_URL/api/v1/fondos" \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | .id'
```

### Formatear JSON para guardar
```bash
curl -s "$API_URL/api/v1/usuarios" \
  -H "Authorization: Bearer $TOKEN" | jq '.' > usuarios.json
```

### Contar registros
```bash
curl -s "$API_URL/api/v1/fondos" \
  -H "Authorization: Bearer $TOKEN" | jq 'length'
```

---

## ⚠️ Errores Comunes

| Error | Causa | Solución |
|-------|-------|----------|
| `Connection refused` | Servidor no está corriendo | Inicia el servidor |
| `401 Unauthorized` | Token inválido/expirado | Obtén un token nuevo |
| `403 Forbidden` | Sin permisos | Usa usuario admin |
| `404 Not Found` | Endpoint no existe | Verifica la URL |
| `400 Bad Request` | Datos inválidos | Revisa el formato JSON |

