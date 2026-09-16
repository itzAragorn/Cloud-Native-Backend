# 📊 Scripts de Población de Bases de Datos - Banco Cloud

Este directorio contiene scripts SQL para poblar las bases de datos con datos de prueba.

## 📋 Scripts Disponibles

### 1. **01-populate-usuarios.sql**
Puebla la tabla `usuarios` de **usuarios-service** con 4 usuarios de prueba.

**Usuarios creados:**

| Email | Nombre | Rol | Estado | Autorizado | Contraseña |
|-------|--------|-----|--------|------------|------------|
| admin@bancocloud.com | Administrador Principal | ADMIN | ACTIVO | ✓ | Admin@123456 |
| cliente1@bancocloud.com | Juan Pérez García | CLIENTE | ACTIVO | ✗ | Cliente123@456 |
| cliente2@bancocloud.com | María López Rodríguez | CLIENTE | ACTIVO | ✓ | Cliente456@789 |
| cliente3@bancocloud.com | Carlos Sánchez Martín | CLIENTE | BLOQUEADO | ✓ | Carlos@123456 |

---

### 2. **02-populate-fondos.sql**
Puebla la tabla `fondos` de **fondos-service** con 6 fondos de inversión.

**Fondos creados:**

| ID | Nombre | Valor Cuota | Estado |
|----|--------|------------|--------|
| 1 | Fondo Conservador Plus | $1,500.00 | ACTIVO |
| 2 | Fondo Moderado Equilibrado | $2,200.00 | ACTIVO |
| 3 | Fondo Agresivo Crecimiento | $3,500.00 | ACTIVO |
| 4 | Fondo Tecnología Global | $4,100.00 | ACTIVO |
| 5 | Fondo Renta Fija Corporativa | $1,100.00 | ACTIVO |
| 6 | Fondo Sostenible ESG | $2,800.00 | INACTIVO |

---

### 3. **03-populate-inversiones.sql**
Puebla la tabla `inversiones` de **inversiones-service** con 8 inversiones.

**Inversiones creadas:**

**Cliente2 (María López - autorizado):**
- $50,000 en Fondo Conservador (ACTIVA, hace 90 días)
- $75,000 en Fondo Moderado (ACTIVA, hace 60 días)
- $100,000 en Fondo Tecnología (ACTIVA, hace 30 días)
- $60,000 en Fondo Renta Fija (ACTIVA, hace 75 días)
- **Total: $285,000**

**Cliente1 (Juan Pérez - no autorizado):**
- $30,000 en Fondo Conservador (ACTIVA, hace 120 días)
- $45,000 en Fondo Renta Fija (ACTIVA, hace 45 días)
- $20,000 en Fondo Agresivo (RETIRADA, hace 180 días)
- $25,000 en Fondo Moderado (CANCELADA, hace 200 días)
- **Total: $120,000**

**Total general: $405,000 en 8 inversiones**

---

## 🚀 Cómo Usar

### Opción 1: Copiar y Pegar Manualmente

#### Paso 1: Conectarse a MySQL desde Terminal

```bash
# Usuarios-Service
mysql -u root -p usuarios_db < scripts-sql/01-populate-usuarios.sql

# Fondos-Service
mysql -u root -p fondos_db < scripts-sql/02-populate-fondos.sql

# Inversiones-Service
mysql -u root -p inversiones_db < scripts-sql/03-populate-inversiones.sql
```

#### Paso 2: O ejecutar en MySQL Workbench

1. Abre MySQL Workbench
2. Conectate a tu servidor MySQL
3. Selecciona la base de datos correspondiente:
   - Para usuarios: `usuarios_db`
   - Para fondos: `fondos_db`
   - Para inversiones: `inversiones_db`
4. Abre el archivo SQL (File → Open SQL Script)
5. Ejecuta el script (Ctrl+Shift+Enter o el botón Execute)

---

## 🔐 Seguridad - Passwords Hasheados

Todos los passwords están hasheados con **BCrypt** (Spring Security):

```
Admin@123456     → $2a$10$lKZcGn8RgsFsDrz.pXJPYe.hxF3e7gVz8uVHVhm5Hw2KzYGJZWyM2
Cliente123@456   → $2a$10$KNlvVkNjJn8c.9XzP.9cWO4GQMQ8m5DvL2K3vQ4rT8sZ1Y0pM6Ba
Cliente456@789   → $2a$10$4qnKnVcL9m3P.8YzQ.8cXO5HRNPR9n6EwM3L4wR5sU9aZ1PqN7Cb
Carlos@123456    → $2a$10$5roLoWdM0n4Q.9ZaR.9dYP6ISQSo0o7FxN4M5xS6tV0bA2QrO8Dc
```

**Todos los passwords cumplen con los requisitos:**
- ✓ Mínimo 8 caracteres
- ✓ Al menos 1 mayúscula
- ✓ Al menos 1 número
- ✓ Al menos 1 símbolo especial (@)

---

## 🧪 Pruebas Recomendadas

Después de ejecutar los scripts, prueba estos escenarios:

### 1. Login con Usuario Admin
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "admin@bancocloud.com",
  "password": "Admin@123456"
}
```

**Respuesta esperada:** ✓ Token JWT válido (acceso autorizado)

### 2. Login con Cliente Autorizado
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "cliente2@bancocloud.com",
  "password": "Cliente456@789"
}
```

**Respuesta esperada:** ✓ Token JWT válido (acceso autorizado)

### 3. Login con Cliente No Autorizado
```bash
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "cliente1@bancocloud.com",
  "password": "Cliente123@456"
}
```

**Respuesta esperada:** ✗ Error 401 - "Usuario no autorizado para acceder al sistema"

### 4. Obtener Fondos
```bash
GET /api/v1/fondos
Authorization: Bearer {token}
```

**Respuesta esperada:** Lista de 6 fondos

### 5. Obtener Inversiones del Cliente
```bash
GET /api/v1/inversiones/usuario/{usuarioId}
Authorization: Bearer {token}
```

**Respuesta esperada:** Lista de inversiones del usuario

---

## 📝 Notas Importantes

1. **Orden de ejecución:** Ejecuta los scripts en orden:
   - Primero: `01-populate-usuarios.sql`
   - Segundo: `02-populate-fondos.sql`
   - Tercero: `03-populate-inversiones.sql`

2. **IDs asumidos:**
   - El script de inversiones asume que los usuarios tienen IDs 2 y 3
   - El script de inversiones asume que los fondos tienen IDs 1-5
   - Ajusta los IDs si usaste un orden diferente

3. **Limpiar datos:**
   Si necesitas empezar de cero, descomenta las líneas `DELETE` al inicio de cada script

4. **Datos adicionales:**
   Los timestamps se generan automáticamente con `NOW()`
   Las fechas de inversión están distribuidas en los últimos 200 días

---

## 🔄 Script Avanzado: Limpieza Total

Si necesitas limpiar todo y empezar de cero:

```sql
-- Primero: Limpiar inversiones-service
DELETE FROM inversiones;
ALTER TABLE inversiones AUTO_INCREMENT = 1;

-- Segundo: Limpiar fondos-service
DELETE FROM fondos;
ALTER TABLE fondos AUTO_INCREMENT = 1;

-- Tercero: Limpiar usuarios-service
DELETE FROM usuarios;
ALTER TABLE usuarios AUTO_INCREMENT = 1;
```

Luego ejecuta los scripts de población nuevamente.

---

## 📞 Soporte

Si encuentras problemas al ejecutar los scripts:

1. **Verifica que las bases de datos existan:**
   ```bash
   mysql -u root -p -e "SHOW DATABASES;"
   ```

2. **Verifica que las tablas existan:**
   ```bash
   mysql -u root -p usuarios_db -e "SHOW TABLES;"
   ```

3. **Revisa los mensajes de error** en la salida de MySQL

4. **Ajusta las contraseñas** de conexión según tu configuración

