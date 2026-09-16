📍 # Scripts para Poblar Bases de Datos - Banco Cloud

> **Guía completa para poblar las bases de datos y probar los endpoints**

---

## 📁 Archivos en este Directorio

| Archivo | Descripción |
|---------|-------------|
| **01-populate-usuarios.sql** | Inserta 4 usuarios de prueba |
| **02-populate-fondos.sql** | Inserta 6 fondos de inversión |
| **03-populate-inversiones.sql** | Inserta 8 inversiones |
| **run-all-scripts.sh** | Script bash para ejecutar todos los SQL automáticamente |
| **test-endpoints.sh** | Script bash para probar todos los endpoints |
| **curl-commands.md** | Comandos cURL listos para copiar y ejecutar |
| **Banco-Cloud-Postman.postman_collection.json** | Colección Postman importable |
| **README.md** | Documentación detallada |
| **index.md** | Este archivo |

---

## 🚀 Inicio Rápido (3 pasos)

### Paso 1️⃣: Ejecutar Scripts SQL

#### Opción A: Usar el script bash (Recomendado)
```bash
cd scripts-sql
chmod +x run-all-scripts.sh
./run-all-scripts.sh
```

#### Opción B: Ejecutar manualmente con MySQL
```bash
# Usuarios-Service
mysql -u root -p usuarios_db < scripts-sql/01-populate-usuarios.sql

# Fondos-Service
mysql -u root -p fondos_db < scripts-sql/02-populate-fondos.sql

# Inversiones-Service
mysql -u root -p inversiones_db < scripts-sql/03-populate-inversiones.sql
```

#### Opción C: Copiar y pegar en MySQL Workbench
1. Abre MySQL Workbench
2. Conectate a tu servidor MySQL
3. Abre File → Open SQL Script
4. Selecciona cada archivo .sql y ejecútalo (Ctrl+Shift+Enter)

---

### Paso 2️⃣: Inicia los Servicios

```bash
# Terminal 1: Usuarios-Service
cd usuarios-service
mvn spring-boot:run

# Terminal 2: Fondos-Service
cd fondos-service
mvn spring-boot:run

# Terminal 3: Inversiones-Service
cd inversiones-service
mvn spring-boot:run

# Terminal 4: BFF-Service (API Gateway)
cd bff-service
mvn spring-boot:run
```

---

### Paso 3️⃣: Prueba los Endpoints

Elige una opción:

#### Opción A: Usar Postman (Más fácil)
1. Abre Postman
2. File → Import
3. Selecciona `Banco-Cloud-Postman.postman_collection.json`
4. Ejecuta los requests (comienza con "Login Admin")

#### Opción B: Usar script bash
```bash
cd scripts-sql
chmod +x test-endpoints.sh
./test-endpoints.sh
```

#### Opción C: Copiar comandos cURL
1. Abre `curl-commands.md`
2. Copia los comandos y pégalos en tu terminal
3. Ejecuta cada uno

---

## 📊 Datos de Prueba Incluidos

### 👥 4 Usuarios
```
Admin (autorizado)           → admin@bancocloud.com | Admin@123456
Cliente (no autorizado)      → cliente1@bancocloud.com | Cliente123@456
Cliente (autorizado)         → cliente2@bancocloud.com | Cliente456@789
Cliente (bloqueado)          → cliente3@bancocloud.com | Carlos@123456
```

### 💰 6 Fondos de Inversión
```
Fondo Conservador Plus       → $1,500.00/cuota
Fondo Moderado Equilibrado   → $2,200.00/cuota
Fondo Agresivo Crecimiento   → $3,500.00/cuota
Fondo Tecnología Global      → $4,100.00/cuota
Fondo Renta Fija Corporativa → $1,100.00/cuota
Fondo Sostenible ESG         → $2,800.00/cuota (INACTIVO)
```

### 📈 8 Inversiones
```
Cliente2: 4 inversiones activas    → $285,000 total
Cliente1: 2 activas + 2 retiradas → $120,000 total
```

---

## 🧪 Test de Flujo Completo

### 1. Login como Admin
```bash
curl -X POST "http://localhost:8090/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bancocloud.com","password":"Admin@123456"}'
```
✅ **Debe devolver:** Token JWT válido

---

### 2. Login como Cliente (Autorizado)
```bash
curl -X POST "http://localhost:8090/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente2@bancocloud.com","password":"Cliente456@789"}'
```
✅ **Debe devolver:** Token JWT válido

---

### 3. Login como Cliente (No Autorizado) - Debe fallar
```bash
curl -X POST "http://localhost:8090/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"cliente1@bancocloud.com","password":"Cliente123@456"}'
```
❌ **Debe devolver:** Error 401 "Usuario no autorizado para acceder al sistema"

---

### 4. Obtener Fondos
```bash
TOKEN="<tu_token_aqui>"
curl -X GET "http://localhost:8090/api/v1/fondos" \
  -H "Authorization: Bearer $TOKEN"
```
✅ **Debe devolver:** Lista de 6 fondos

---

### 5. Obtener Inversiones del Usuario
```bash
TOKEN="<tu_token_aqui>"
curl -X GET "http://localhost:8090/api/v1/inversiones/usuario/3" \
  -H "Authorization: Bearer $TOKEN"
```
✅ **Debe devolver:** 4 inversiones para cliente2

---

## 🔐 Contraseñas Hasheadas

Todos los passwords están protegidos con BCrypt:

| Usuario | Password | Hash |
|---------|----------|------|
| admin | Admin@123456 | `$2a$10$lKZcGn8RgsFsDrz...` |
| cliente1 | Cliente123@456 | `$2a$10$KNlvVkNjJn8c.9Xz...` |
| cliente2 | Cliente456@789 | `$2a$10$4qnKnVcL9m3P.8Yz...` |
| cliente3 | Carlos@123456 | `$2a$10$5roLoWdM0n4Q.9Za...` |

**Requisitos cumplidos:**
- ✓ Mínimo 8 caracteres
- ✓ Al menos 1 mayúscula
- ✓ Al menos 1 número
- ✓ Al menos 1 símbolo especial

---

## 📚 Documentación Completa

Para más detalles, consulta:

- **[README.md](README.md)** - Documentación extendida
- **[curl-commands.md](curl-commands.md)** - Todos los comandos cURL
- **Scripts SQL comentados:**
  - [01-populate-usuarios.sql](01-populate-usuarios.sql)
  - [02-populate-fondos.sql](02-populate-fondos.sql)
  - [03-populate-inversiones.sql](03-populate-inversiones.sql)

---

## 🛠️ Solución de Problemas

### Error: "Connection refused"
```
✓ Solución: Verifica que MySQL está corriendo
mysql -u root -p -e "SELECT 1"
```

### Error: "Access denied"
```
✓ Solución: Verifica usuario/contraseña
export MYSQL_PASSWORD="tu_contraseña"
```

### Error: "Database doesn't exist"
```
✓ Solución: Las BD deben estar creadas por Spring Boot
Ejecuta: mvn spring-boot:run (en cada servicio)
```

### Error: "Token inválido"
```
✓ Solución: El token expiró, haz login de nuevo
```

---

## 📋 Checklist de Instalación

```
□ MySQL corriendo en localhost:3306
□ Bases de datos creadas (usuarios_db, fondos_db, inversiones_db)
□ Scripts SQL ejecutados (en orden)
□ Servicios compilados (mvn clean install)
□ Todos los servicios iniciados
□ Token JWT obtenido de /api/v1/auth/login
□ Endpoints probados con Postman o cURL
```

---

## 🚀 Próximos Pasos

1. **Crear nuevas inversiones**
   ```bash
   POST /api/v1/inversiones
   ```

2. **Autorizar clientes pendientes**
   ```bash
   PUT /api/v1/usuarios/{id}/autorizar
   ```

3. **Actualizar valor de cuota de fondos**
   ```bash
   PUT /api/v1/fondos/{id}
   ```

4. **Consultar reportes**
   ```bash
   GET /api/v1/inversiones/reporte
   ```

---

## 💡 Tips Profesionales

### Guardar tokens en variables
```bash
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8090/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@bancocloud.com","password":"Admin@123456"}' | jq -r '.token')

echo "Token: $ADMIN_TOKEN"
```

### Usar jq para parsear JSON
```bash
# Extraer solo emails
curl -s "http://localhost:8090/api/v1/usuarios" \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | .email'

# Extraer solo montos invertidos
curl -s "http://localhost:8090/api/v1/inversiones/usuario/3" \
  -H "Authorization: Bearer $TOKEN" | jq '.[] | .montoInvertido'
```

### Automatizar con loops
```bash
# Probar login de todos los usuarios
for email in admin@bancocloud.com cliente1@bancocloud.com cliente2@bancocloud.com; do
  echo "Probando: $email"
  curl -s -X POST "http://localhost:8090/api/v1/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$email\",\"password\":\"test\"}" | jq '.token'
done
```

---

## 📞 Contacto & Soporte

Si encuentras problemas:

1. Revisa los logs del servicio correspondiente
2. Verifica la conexión a MySQL
3. Asegúrate que Spring Boot creó las tablas
4. Prueba con un comando cURL simple
5. Consulta la documentación en README.md

---

## 📄 Licencia

Este proyecto es parte de Banco Cloud EV1.

**Última actualización:** 2024-09-10

