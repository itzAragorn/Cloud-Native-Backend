# 🚀 Banco Cloud - Guía de Inicio Rápido

## En 5 minutos: Setup y Primer Test

### Paso 1: Verificar Requisitos ✅
```bash
# Verificar Java 17
java -version
# Expected: openjdk version "17.x.x"

# Verificar Maven
mvn -version
# Expected: Apache Maven 3.8+

# Verificar MySQL
mysql --version
# Expected: mysql  Ver X.X
```

### Paso 2: Crear Bases de Datos (Copia-Pega)
```bash
# Conectarse a MySQL
mysql -u root -p

# Pegar esto en el prompt de MySQL:
```

```sql
CREATE DATABASE fondos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE inversiones_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE usuarios_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
SHOW DATABASES;
EXIT;
```

### Paso 3: Compilar Proyectos
```bash
# Navegar al workspace
cd /Users/nicoosses/Proyectos/Cloud\ Native/EV1

# Compilar todos (Takes ~2 min cada uno)
cd fondos-service && mvn clean install && cd ..
cd inversiones-service && mvn clean install && cd ..
cd usuarios-service && mvn clean install && cd ..
```

### Paso 4: Iniciar Servicios (Abre 3 terminales)

**Terminal 1:**
```bash
cd fondos-service
mvn spring-boot:run
# Espera: "Started FondosServiceApplication"
```

**Terminal 2:**
```bash
cd inversiones-service
mvn spring-boot:run
# Espera: "Started InversionesServiceApplication"
```

**Terminal 3:**
```bash
cd usuarios-service
mvn spring-boot:run
# Espera: "Started UsuariosServiceApplication"
```

### Paso 5: Probar (Terminal 4 - Copia-Pega)

```bash
# Test 1: Crear Fondo
curl -X POST http://localhost:8081/api/v1/fondos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Fondo Test",
    "descripcion": "Descripción de prueba del fondo",
    "valorCuota": 50.00
  }'

# Respuesta esperada: {id: 1, nombre: "Fondo Test", ...}
```

```bash
# Test 2: Crear Usuario
curl -X POST http://localhost:8083/api/v1/usuarios \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Test User",
    "email": "test@bancoclod.com",
    "rol": "CLIENTE"
  }'

# Respuesta esperada: {id: 1, nombre: "Test User", accesoAutorizado: false, ...}
```

```bash
# Test 3: Autorizar Usuario
curl -X PUT http://localhost:8083/api/v1/usuarios/1/autorizar \
  -H "Content-Type: application/json" \
  -d '{"autorizado": true}'

# Respuesta esperada: {id: 1, accesoAutorizado: true, ...}
```

```bash
# Test 4: Crear Inversión
curl -X POST http://localhost:8082/api/v1/inversiones \
  -H "Content-Type: application/json" \
  -d '{
    "usuarioId": 1,
    "fondoId": 1,
    "montoInvertido": 1000.00
  }'

# Respuesta esperada: {id: 1, montoInvertido: 1000, cuotas: 20, ...}
```

```bash
# Test 5: Ver Portafolio
curl http://localhost:8082/api/v1/inversiones/mi-portafolio?usuarioId=1

# Respuesta esperada: {usuarioId: 1, montoTotalInvertido: 1000, valorTotalActual: 1000, ...}
```

✅ **¡Listo! Los 3 microservicios funcionan correctamente**

---

## Importar a Postman (Recomendado para desarrollo)

1. Abrir **Postman**
2. Click en **Import**
3. **Upload Files** → Seleccionar `BancoCloud-Postman-Collection.json`
4. Verás 3 carpetas: FONDOS SERVICE, INVERSIONES SERVICE, USUARIOS SERVICE
5. Ejecutar requests desde la interfaz

---

## Estructura Rápida

```
Workspace:
├── fondos-service        (Puerto 8081) - Gestión de fondos
├── inversiones-service   (Puerto 8082) - Inversiones y portafolio
├── usuarios-service      (Puerto 8083) - Usuarios y autorización
└── bff-service          (Puerto 8080) - ⏳ Por implementar (JWT + routing)

Bases de Datos:
├── fondos_db       (tablas: fondos)
├── inversiones_db  (tablas: inversiones)
└── usuarios_db     (tablas: usuarios)
```

---

## Troubleshooting Común

### Error: "Connection refused" en puerto 8081/8082/8083
**Solución**: Verificar que los servicios están corriendo en las terminales correctas
```bash
# Verificar puertos activos
lsof -i :8081
lsof -i :8082
lsof -i :8083
# Debería mostrar java processes
```

### Error: "Cannot connect to MySQL"
**Solución**: Verificar que MySQL está corriendo
```bash
# macOS con Homebrew
brew services list | grep mysql
# Si está stopped:
brew services start mysql-server
```

### Error: "Table doesn't exist"
**Solución**: Confirmar que las bases de datos fueron creadas
```bash
mysql -u root -p
SHOW DATABASES;
USE fondos_db;
SHOW TABLES;
# Debería crear tablas automáticamente en primer request
```

### Error: "Cannot resolve com.bancocloud"
**Solución**: Ejecutar `mvn clean install` nuevamente en el servicio

---

## Comandos Útiles

### Ver logs en tiempo real
```bash
# En cada terminal donde corre el servicio:
# Los logs aparecen automáticamente
# Para ver solo errores:
# [Grep no es necesario, revisar consola roja]
```

### Detener servicios
```bash
# En cada terminal: Ctrl+C
```

### Limpiar datos (borrar tablas)
```bash
mysql -u root -p
USE fondos_db;
DELETE FROM fondos;
# Repeat para inversiones_db e usuarios_db
```

### Compilar un solo servicio
```bash
cd fondos-service
mvn clean compile
```

---

## Próximos Pasos Después del Setup

1. **Leer DOCUMENTACION_API.md** para detalles de cada endpoint
2. **Implementar bff-service** con autenticación JWT
3. **Agregar tests** unitarios e integración
4. **Documentación OpenAPI** con Swagger
5. **Containerizar** con Docker
6. **Desplegar en AWS**

---

## Preguntas Frecuentes

**P: ¿Puedo usar H2 en lugar de MySQL?**
A: Sí, descomenta las líneas de H2 en `application.properties` y comenta las de MySQL

**P: ¿Debo detener los servicios para cambiar el código?**
A: Sí, cambia el código, guarda, detén (Ctrl+C), y ejecuta `mvn spring-boot:run` nuevamente

**P: ¿Cómo conectarme directamente a la BD?**
```bash
mysql -u root -p
USE fondos_db;
SELECT * FROM fondos;
```

**P: ¿Puedo cambiar los puertos?**
A: Sí, edita `server.port` en cada `application.properties`

---

**Última actualización**: 2024-01-15  
**Duración estimada del setup**: 5-10 min (primea vez)  
**Soporte**: Revisar logs en consola roja
