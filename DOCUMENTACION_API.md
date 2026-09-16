# Banco Cloud - Microservicios API
## Documentación Completa y Guía de Setup

---

## 📋 Tabla de Contenidos
1. [Requisitos Previos](#requisitos-previos)
2. [Setup Local](#setup-local)
3. [Estructura de Proyectos](#estructura-de-proyectos)
4. [Endpoints API](#endpoints-api)
5. [Ejemplos de Uso (Postman)](#ejemplos-de-uso-postman)
6. [Flujos de Negocio](#flujos-de-negocio)
7. [Próximos Pasos](#próximos-pasos)

---

## Requisitos Previos

### Software Necesario
- **Java 17+**: verificar con `java -version`
- **Maven 3.8+**: verificar con `mvn -version`
- **MySQL 8.0+**: para persistencia
- **Postman** o **curl**: para testing de APIs
- **Git**: control de versiones
- **VS Code** + **GitHub Copilot**: desarrollo

### Variables de Entorno Recomendadas
```bash
# ~/.zshrc o ~/.bashrc
export JAVA_HOME=/path/to/java17
export MAVEN_HOME=/path/to/maven
export PATH=$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH
```

---

## Setup Local

### 1. Crear Bases de Datos MySQL

```sql
-- Conectarse a MySQL
mysql -u root -p

-- Crear bases de datos
CREATE DATABASE fondos_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE inversiones_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE usuarios_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Verificar
SHOW DATABASES;
```

### 2. Compilar Proyectos

```bash
# En el directorio raíz del workspace: /Users/nicoosses/Proyectos/Cloud Native/EV1

# Compilar fondos-service
cd fondos-service
mvn clean install
cd ..

# Compilar inversiones-service
cd inversiones-service
mvn clean install
cd ..

# Compilar usuarios-service
cd usuarios-service
mvn clean install
cd ..

# Compilar bff-service (próximamente)
cd bff-service
mvn clean install
cd ..
```

### 3. Iniciar Servicios (en orden)

**Terminal 1 - Fondos Service**
```bash
cd fondos-service
mvn spring-boot:run
# Salida esperada: Started FondosServiceApplication on port 8081
```

**Terminal 2 - Inversiones Service**
```bash
cd inversiones-service
mvn spring-boot:run
# Salida esperada: Started InversionesServiceApplication on port 8082
```

**Terminal 3 - Usuarios Service**
```bash
cd usuarios-service
mvn spring-boot:run
# Salida esperada: Started UsuariosServiceApplication on port 8083
```

### 4. Verificar Servicios en Ejecución

```bash
# Terminal 4 - Verificar health checks
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

# O sin actuator, probar endpoints básicos
curl http://localhost:8081/api/v1/fondos
curl http://localhost:8082/api/v1/inversiones/mis-inversiones?usuarioId=1
curl http://localhost:8083/api/v1/usuarios
```

---

## Estructura de Proyectos

Cada microservicio sigue esta estructura:

```
service-name/
├── src/main/java/com/bancocloud/service_name/
│   ├── model/              # Entidades JPA
│   │   └── *.java
│   ├── dto/                # Data Transfer Objects
│   │   └── *.java
│   ├── repository/         # Spring Data JPA
│   │   └── *Repository.java
│   ├── service/            # Lógica de negocio
│   │   └── *Service.java
│   ├── controller/         # REST Controllers
│   │   └── *Controller.java
│   ├── exception/          # Manejo de errores
│   │   ├── GlobalExceptionHandler.java
│   │   ├── ErrorResponse.java
│   │   └── *Exception.java
│   ├── client/             # HTTP Clients (solo inversiones)
│   │   └── FondoServiceClient.java
│   ├── config/             # Configuraciones
│   │   └── *.java
│   └── *Application.java   # Spring Boot entry point
├── src/main/resources/
│   └── application.properties
└── pom.xml
```

---

## Endpoints API

### FONDOS-SERVICE (Puerto 8081)

#### 1. Crear Fondo (ADMIN)
```http
POST /api/v1/fondos
Content-Type: application/json

{
  "nombre": "Fondo de Acciones Colombia",
  "descripcion": "Fondo de inversión en acciones colombianas con enfoque de crecimiento a largo plazo",
  "valorCuota": 50.00
}

Response 201 Created:
{
  "id": 1,
  "nombre": "Fondo de Acciones Colombia",
  "descripcion": "Fondo de inversión en acciones colombianas con enfoque de crecimiento a largo plazo",
  "valorCuota": 50.00,
  "fechaCreacion": "2024-01-15T10:30:00",
  "fechaActualizacion": "2024-01-15T10:30:00",
  "estado": "ACTIVO"
}
```

#### 2. Listar Todos los Fondos
```http
GET /api/v1/fondos

Response 200 OK: [Array of FondoDTO]
```

#### 3. Listar Fondos Activos
```http
GET /api/v1/fondos/activos

Response 200 OK: [Array of FondoDTO - solo activos]
```

#### 4. Obtener Fondo por ID
```http
GET /api/v1/fondos/{id}

Response 200 OK: FondoDTO
```

#### 5. Obtener Valor de Cuota
```http
GET /api/v1/fondos/{id}/valor-cuota

Response 200 OK:
55.75
```

#### 6. Actualizar Valor de Cuota (ADMIN)
```http
PUT /api/v1/fondos/{id}/valor-cuota
Content-Type: application/json

{
  "nuevoValor": 55.75
}

Response 200 OK: FondoDTO
```

---

### INVERSIONES-SERVICE (Puerto 8082)

#### 1. Crear Inversión (CLIENTE)
```http
POST /api/v1/inversiones
Content-Type: application/json

{
  "usuarioId": 1,
  "fondoId": 1,
  "montoInvertido": 1000.00
}

Response 201 Created:
{
  "id": 1,
  "usuarioId": 1,
  "fondoId": 1,
  "montoInvertido": 1000.00,
  "cuotas": 20.0000,        // calculado: 1000 / 50
  "valorCuotaCompra": 50.00,
  "fechaInversion": "2024-01-15T10:35:00",
  "fechaActualizacion": "2024-01-15T10:35:00",
  "estado": "ACTIVA"
}
```

#### 2. Obtener Mis Inversiones (CLIENTE)
```http
GET /api/v1/inversiones/mis-inversiones?usuarioId=1

Response 200 OK: [Array of InversionDTO]
```

#### 3. Obtener Mi Portafolio (CLIENTE)
```http
GET /api/v1/inversiones/mi-portafolio?usuarioId=1

Response 200 OK:
{
  "usuarioId": 1,
  "montoTotalInvertido": 1000.00,
  "valorTotalActual": 1115.00,    // Si cuota subió a 55.75
  "rendimientoTotal": 115.00,
  "rendimientoPorcentaje": 11.5000,
  "inversiones": [Array of InversionDTO]
}
```

#### 4. Obtener Rendimiento de Inversión
```http
GET /api/v1/inversiones/{id}/rendimiento

Response 200 OK:
{
  "inversionId": 1,
  "fondoId": 1,
  "nombreFondo": "Fondo de Acciones Colombia",
  "montoInvertido": 1000.00,
  "valorActualCuota": 55.75,
  "valorCuotaCompra": 50.00,
  "cuotas": 20.0000,
  "valorTotalActual": 1115.00,
  "rendimientoAbsoluto": 115.00,
  "rendimientoPorcentaje": 11.5000
}
```

#### 5. Obtener Rendimiento del Usuario
```http
GET /api/v1/inversiones/rendimiento?usuarioId=1

Response 200 OK: [Array of RendimientoDTO]
```

#### 6. Obtener Inversión por ID
```http
GET /api/v1/inversiones/{id}

Response 200 OK: InversionDTO
```

---

### USUARIOS-SERVICE (Puerto 8083)

#### 1. Crear Usuario
```http
POST /api/v1/usuarios
Content-Type: application/json

{
  "nombre": "Juan Pérez",
  "email": "juan.perez@bancoclod.com",
  "rol": "CLIENTE"
}

Response 201 Created:
{
  "id": 1,
  "nombre": "Juan Pérez",
  "email": "juan.perez@bancoclod.com",
  "rol": "CLIENTE",
  "accesoAutorizado": false,      // Requiere autorización del ADMIN
  "estado": "ACTIVO",
  "fechaCreacion": "2024-01-15T10:40:00",
  "fechaActualizacion": "2024-01-15T10:40:00"
}
```

#### 2. Listar Todos los Usuarios
```http
GET /api/v1/usuarios

Response 200 OK: [Array of UsuarioDTO]
```

#### 3. Listar Usuarios por Rol
```http
GET /api/v1/usuarios/por-rol?rol=CLIENTE

Response 200 OK: [Array of UsuarioDTO con rol CLIENTE]
```

#### 4. Listar Clientes Pendientes de Autorización
```http
GET /api/v1/usuarios/pendientes-autorizacion

Response 200 OK: [Array of CLIENTE UsuarioDTO sin acceso autorizado]
```

#### 5. Obtener Usuario por ID
```http
GET /api/v1/usuarios/{id}

Response 200 OK: UsuarioDTO
```

#### 6. Obtener Usuario por Email
```http
GET /api/v1/usuarios/por-email?email=juan.perez@bancoclod.com

Response 200 OK: UsuarioDTO
```

#### 7. Autorizar Usuario (ADMIN)
```http
PUT /api/v1/usuarios/{id}/autorizar
Content-Type: application/json

{
  "autorizado": true
}

Response 200 OK: UsuarioDTO actualizado con accesoAutorizado = true
```

#### 8. Bloquear Usuario (ADMIN)
```http
PUT /api/v1/usuarios/{id}/bloquear

Response 200 OK: UsuarioDTO con estado = BLOQUEADO
```

#### 9. Desbloquear Usuario (ADMIN)
```http
PUT /api/v1/usuarios/{id}/desbloquear

Response 200 OK: UsuarioDTO con estado = ACTIVO
```

#### 10. Verificar Acceso Usuario
```http
GET /api/v1/usuarios/{id}/verificar-acceso

Response 200 OK:
true | false
```

---

## Ejemplos de Uso (Postman)

### Setup Inicial en Postman

1. **Crear colección** "BancoCloud"
2. **Crear carpetas**:
   - Fondos
   - Inversiones
   - Usuarios

### Flujo de Ejemplo: Invertir en Fondo

#### Paso 1: Crear un Fondo (como ADMIN)
```http
POST http://localhost:8081/api/v1/fondos
{
  "nombre": "Fondo Renta Variable",
  "descripcion": "Fondo diversificado con enfoque en rentabilidad variable",
  "valorCuota": 100.00
}
```
✅ Guardar ID del fondo: **1**

#### Paso 2: Crear Usuario (Cliente)
```http
POST http://localhost:8083/api/v1/usuarios
{
  "nombre": "Carlos López",
  "email": "carlos@bancoclod.com",
  "rol": "CLIENTE"
}
```
✅ Guardar ID del usuario: **1**
⚠️ Estado: accesoAutorizado = false

#### Paso 3: Autorizar Usuario
```http
PUT http://localhost:8083/api/v1/usuarios/1/autorizar
{
  "autorizado": true
}
```
✅ accesoAutorizado ahora es true

#### Paso 4: Crear Inversión
```http
POST http://localhost:8082/api/v1/inversiones
{
  "usuarioId": 1,
  "fondoId": 1,
  "montoInvertido": 5000.00
}
```
✅ Inversión creada
- Cuotas calculadas: 5000 / 100 = 50 cuotas
- valorCuotaCompra: 100.00

#### Paso 5: Actualizar Valor de Cuota (Simulando cambio en mercado)
```http
PUT http://localhost:8081/api/v1/fondos/1/valor-cuota
{
  "nuevoValor": 110.50
}
```
✅ Nueva cuota: 110.50

#### Paso 6: Consultar Portafolio
```http
GET http://localhost:8082/api/v1/inversiones/mi-portafolio?usuarioId=1
```
✅ Respuesta:
- valorTotalActual = 50 × 110.50 = 5,525.00
- rendimientoTotal = 5,525.00 - 5,000.00 = 525.00
- rendimientoPorcentaje = 10.5%

---

## Flujos de Negocio

### Flujo 1: Autorización de Cliente
```
1. Cliente se registra
   ↓
2. ADMIN ve usuario pendiente
   ↓
3. ADMIN autoriza acceso
   ↓
4. Cliente puede invertir
```

### Flujo 2: Crear y Monitorear Inversión
```
1. ADMIN crea fondo con valor de cuota
   ↓
2. Cliente autorizado crea inversión
   ↓
3. Sistema consulta valor actual del fondo
   ↓
4. Calcula cuotas = monto / valorCuota
   ↓
5. Cliente puede ver:
   - Portafolio total
   - Rendimiento de inversiones
   - Cambios en valor de cuota
```

### Flujo 3: Cambio de Valor de Cuota
```
1. Mercado cambia → ADMIN actualiza valorCuota
   ↓
2. Cliente consulta portafolio
   ↓
3. Sistema recalcula:
   - valorTotalActual = cuotas × nuevoValorCuota
   - rendimiento = valorTotalActual - montoInvertido
   ↓
4. Cliente ve ganancia/pérdida
```

---

## Códigos de Error HTTP

### 2xx - Éxito
- **201 Created**: Recurso creado exitosamente
- **200 OK**: Operación exitosa

### 4xx - Cliente
- **400 Bad Request**: Validación fallida
  ```json
  {
    "error": "VALIDATION_ERROR",
    "errores": {
      "nombre": "El nombre es requerido"
    }
  }
  ```

- **404 Not Found**: Recurso no existe
  ```json
  {
    "status": 404,
    "error": "NOT_FOUND",
    "message": "Fondo no encontrado con id: 999"
  }
  ```

- **409 Conflict**: Recurso duplicado
  ```json
  {
    "status": 409,
    "error": "CONFLICT",
    "message": "Ya existe un fondo con el nombre: ..."
  }
  ```

### 5xx - Servidor
- **500 Internal Server Error**: Error interno

---

## Próximos Pasos

### Inmediatos
1. ✅ Compilar y ejecutar los 3 servicios
2. ✅ Probar endpoints en Postman
3. ✅ Verificar logs en consola
4. ⏳ Implementar bff-service con JWT

### BFF Service (Próxima Fase)
- Configurar Spring Security
- Validar JWT desde Azure AD
- Proxying de requests a microservicios
- Rate limiting
- Unified error handling

### Testing
- Crear tests unitarios para Services
- Crear tests de integración
- Postman collections compartibles
- SonarQube analysis

### Documentación Adicional
- OpenAPI/Swagger (spring-boot-starter-springdoc-openapi)
- Diagrama de componentes
- Diagrama de secuencia

---

## Notas Importantes

✅ **Separación de Datos**: Cada servicio tiene su propia base de datos
✅ **Comunicación**: inversiones-service consulta fondos-service vía HTTP
✅ **Validación**: Todos los DTOs validan con @Valid
✅ **Transacciones**: @Transactional en Services
✅ **Logging**: Slf4j en todos los servicios
✅ **Manejo de Errores**: GlobalExceptionHandler centralizado

⚠️ **Por Implementar**:
- Autenticación JWT en BFF
- CORS configuration
- Swagger/OpenAPI documentation
- Docker containers
- Deployment a AWS

---

**Última actualización**: 2024-01-15
**Versión**: 1.0
**Estado**: Fondos, Inversiones, Usuarios ✅ | BFF ⏳
