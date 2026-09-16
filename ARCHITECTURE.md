# 🏗️ Banco Cloud - Arquitectura del Sistema

## Diagrama de Componentes

```
┌─────────────────────────────────────────────────────────────────┐
│                         USUARIOS FINALES                         │
├─────────────────────────────────────────────────────────────────┤
│
│  ┌──────────────────────────────────────────────────────────┐
│  │               React Frontend                             │
│  │  (Con autenticación Azure AD + JWT)                     │
│  │  ⏳ Por implementar                                      │
│  └──────────────────────────────┬───────────────────────────┘
│                                 │
│                        HTTP/REST │
│                                 ▼
│  ┌──────────────────────────────────────────────────────────┐
│  │            BFF SERVICE - Puerto 8080                     │
│  │  (API Gateway, JWT Validation, Routing)                 │
│  │  ⏳ Por implementar (Spring Security OAuth2)            │
│  │                                                          │
│  │  ├─ GET    /api/v1/fondos/*                            │
│  │  ├─ POST   /api/v1/inversiones                         │
│  │  ├─ GET    /api/v1/usuarios/*                          │
│  │  └─ PUT    /api/v1/usuarios/{id}/autorizar             │
│  └───┬──────────────┬───────────────┬──────────────────────┘
│      │              │               │
│  ┌───▼───┐      ┌───▼───┐      ┌───▼───┐
│  │ 8081  │      │ 8082  │      │ 8083  │
│  └───┬───┘      └───┬───┘      └───┬───┘
│      ▼              ▼              ▼
└─────┬──────────────┬───────────────┬────────────────────────┐
      │              │               │
      │  HTTP        │  HTTP         │  HTTP
      │  (REST)      │  (REST)       │  (REST)
      ▼              ▼               ▼
┌─────────────────────────────────────────────────────────────┐
│                    MICROSERVICIOS BACKEND                    │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌────────────────────┐  ┌────────────────────┐            │
│  │  FONDOS-SERVICE    │  │ INVERSIONES-SERVICE│  Comunica  │
│  │  Puerto: 8081      │  │  Puerto: 8082      │◄──────────┐│
│  │  ✅ COMPLETADO     │  │  ✅ COMPLETADO     │           ││
│  │                    │  │                    │           ││
│  │ Endpoints (6):     │  │ Endpoints (6):     │           ││
│  │ ├ POST   /fondos   │  │ ├ POST   /inversiones       ││
│  │ ├ GET    /fondos   │  │ ├ GET    /mis-inversiones  ││
│  │ ├ GET    /fondos/activos   │ ├ GET    /mi-portafolio    ││
│  │ ├ GET    /fondos/{id}      │ ├ GET    /rendimiento  ││
│  │ ├ GET    /fondos/{id}/valor-cuota  ││
│  │ └ PUT    /fondos/{id}/valor-cuota  ││
│  │                    │  │                    │           ││
│  │ Entidades (1):     │  │ Entidades (1):     │           ││
│  │ ├ Fondo            │  │ ├ Inversion        │           ││
│  │ └ EstadoFondo      │  │ └ EstadoInversion  │           ││
│  │                    │  │                    │           ││
│  │ Base de Datos:     │  │ Base de Datos:     │           ││
│  │ fondos_db (MySQL)  │  │ inversiones_db     │           ││
│  └────────────────────┘  └────────────────────┘           │
│         ▲                                                   │
│         │                                                   │
│         │ Consulta valores de cuota                        │
│         │ (FondoServiceClient)                             │
│         │                                                   │
│    ┌────────────────────┐                                  │
│    │ USUARIOS-SERVICE   │                                  │
│    │ Puerto: 8083       │                                  │
│    │ ✅ COMPLETADO      │                                  │
│    │                    │                                  │
│    │ Endpoints (9):     │                                  │
│    │ ├ POST   /usuarios │                                  │
│    │ ├ GET    /usuarios │                                  │
│    │ ├ GET    /usuarios/por-rol                            │
│    │ ├ GET    /usuarios/pendientes-autorizacion           │
│    │ ├ GET    /usuarios/{id}                               │
│    │ ├ GET    /usuarios/por-email                          │
│    │ ├ PUT    /usuarios/{id}/autorizar                     │
│    │ ├ PUT    /usuarios/{id}/bloquear                      │
│    │ ├ PUT    /usuarios/{id}/desbloquear                   │
│    │ └ GET    /usuarios/{id}/verificar-acceso              │
│    │                    │                                  │
│    │ Entidades (3):     │                                  │
│    │ ├ Usuario          │                                  │
│    │ ├ Rol              │                                  │
│    │ └ EstadoUsuario    │                                  │
│    │                    │                                  │
│    │ Base de Datos:     │                                  │
│    │ usuarios_db (MySQL)│                                  │
│    └────────────────────┘                                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
      │                 │                 │
      ▼                 ▼                 ▼
┌───────────────────────────────────────────────────────────┐
│              BASES DE DATOS - MySQL 8.0+                  │
├───────────────────────────────────────────────────────────┤
│                                                            │
│  ┌─────────────────┐  ┌──────────────────┐               │
│  │   fondos_db     │  │  inversiones_db  │               │
│  ├─────────────────┤  ├──────────────────┤               │
│  │ Tabla: fondos   │  │ Tabla: inversiones               │
│  │                 │  │                  │               │
│  │ Columnas:       │  │ Columnas:        │               │
│  │ ├ id            │  │ ├ id             │               │
│  │ ├ nombre        │  │ ├ usuario_id     │               │
│  │ ├ descripcion   │  │ ├ fondo_id       │               │
│  │ ├ valor_cuota   │  │ ├ monto_invertido               │
│  │ ├ estado        │  │ ├ cuotas         │               │
│  │ ├ fecha_creacion│  │ ├ valor_cuota_compra             │
│  │ └ fecha_actualizacion  │ ├ estado         │               │
│  │                 │  │ ├ fecha_inversion│               │
│  │ Index: pk(id)   │  │ └ fecha_actualizacion             │
│  │ Unique: nombre  │  │                  │               │
│  │                 │  │ Index: pk(id)    │               │
│  │                 │  │ Index: usuario_id               │
│  └─────────────────┘  │ Index: fondo_id  │               │
│                       └──────────────────┘               │
│                                                            │
│  ┌──────────────────────────────────────┐               │
│  │        usuarios_db                   │               │
│  ├──────────────────────────────────────┤               │
│  │ Tabla: usuarios                      │               │
│  │                                      │               │
│  │ Columnas:                            │               │
│  │ ├ id                                 │               │
│  │ ├ nombre                             │               │
│  │ ├ email (UNIQUE)                     │               │
│  │ ├ rol (ENUM: ADMIN, CLIENTE)         │               │
│  │ ├ acceso_autorizado (Boolean)        │               │
│  │ ├ estado (ENUM: ACTIVO, BLOQUEADO)   │               │
│  │ ├ fecha_creacion                     │               │
│  │ └ fecha_actualizacion                │               │
│  │                                      │               │
│  │ Index: pk(id)                        │               │
│  │ Index: email (UNIQUE)                │               │
│  │ Index: rol                           │               │
│  │ Index: acceso_autorizado             │               │
│  └──────────────────────────────────────┘               │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

## Flujo de Datos - Ejemplo: Crear Inversión

```
                    React Frontend
                          │
                          │ POST /inversiones
                          │ {usuarioId: 1, fondoId: 1, montoInvertido: 1000}
                          ▼
                    ┌──────────────┐
                    │ BFF Service  │ ⏳
                    │ (Port 8080)  │
                    └──────┬───────┘
                           │
                           │ POST /api/v1/inversiones
                           │ (Forward request)
                           ▼
                   ┌─────────────────────┐
                   │ Inversiones Service │
                   │   (Port 8082)       │
                   └────────┬────────────┘
                            │
                    ┌───────┴────────┐
                    ▼                ▼
            ┌──────────────────┐    ┌─────────────────────┐
            │ Repository.save()│    │ FondoServiceClient  │
            │ Inversion entity │    │ GET /fondos/{id}    │
            │ to inversiones_db│    │ GET /fondos/{id}/   │
            │                  │    │     valor-cuota     │
            └──────────────────┘    └────────┬────────────┘
                                             │
                                    ┌────────▼──────────┐
                                    │ Fondos Service    │
                                    │  (Port 8081)      │
                                    │ Repository.findById│
                                    │ from fondos_db    │
                                    └────────┬──────────┘
                                             │
                                    ┌────────▼──────────┐
                                    │ Return:           │
                                    │ {id, nombre,      │
                                    │  valorCuota: 50}  │
                                    └───────────────────┘

Calculó en servicio de inversiones:
cuotas = 1000 / 50 = 20 cuotas ✅

Response:
{
  "id": 1,
  "usuarioId": 1,
  "fondoId": 1,
  "montoInvertido": 1000.00,
  "cuotas": 20.0000,
  "valorCuotaCompra": 50.00,
  "estado": "ACTIVA",
  "fechaInversion": "2024-01-15T10:30:00",
  "fechaActualizacion": "2024-01-15T10:30:00"
}
```

## Flujo de Datos - Consultar Portafolio

```
                   React Frontend
                         │
                         │ GET /inversiones/mi-portafolio?usuarioId=1
                         ▼
                    ┌──────────────┐
                    │ BFF Service  │ ⏳
                    │ (Port 8080)  │
                    └──────┬───────┘
                           │
                    ┌──────▼───────────────────┐
                    │ InversionesService       │
                    │ obtenerMiPortafolio(1)   │
                    └──────┬───────────────────┘
                           │
                    ┌──────▼──────────────────┐
                    │ 1. Repository.find      │
                    │    ByUsuarioIdAndEstado │
                    │    (1, ACTIVA)          │
                    └──────┬──────────────────┘
                           │
                    ┌──────▼────────────────────┐
                    │ 2. Por cada inversión:    │
                    │    FondoServiceClient     │
                    │    .obtenerValorCuota()   │
                    │    (actuales en mercado)  │
                    └──────┬────────────────────┘
                           │
                    ┌──────▼──────────────────────┐
                    │ 3. Calcular:                 │
                    │    - valorTotalActual       │
                    │      = cuotas × nuevaValor  │
                    │    - rendimiento            │
                    │      = actual - invertido   │
                    │    - rendimiento %          │
                    │      = (rdto/invertido)*100 │
                    └──────┬──────────────────────┘
                           │
                    ┌──────▼─────────────────────┐
                    │ Response: PortafolioDTO    │
                    │ {                          │
                    │   usuarioId: 1,            │
                    │   montoTotalInvertido:     │
                    │   1000.00,                 │
                    │   valorTotalActual: 1100,  │
                    │   rendimientoTotal: 100,   │
                    │   rendimientoPorcentaje:10,│
                    │   inversiones: [...]       │
                    │ }                          │
                    └────────────────────────────┘
```

## Estructura de Capas (Por Servicio)

```
┌─────────────────────────────────────────────────────┐
│                   REST Layer                        │
│  Controller (FondoController.java)                  │
│  - @RestController                                  │
│  - @RequestMapping                                  │
│  - @PostMapping, @GetMapping, @PutMapping           │
│  - Recibe requests, valida con @Valid               │
│  - Devuelve status code HTTP apropiado              │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│                   Business Logic Layer               │
│  Service (FondoService.java)                        │
│  - @Service                                         │
│  - @Transactional                                   │
│  - Lógica de validación                             │
│  - Cálculos complejos                               │
│  - Llamadas a otros servicios                       │
│  - Manejo de transacciones                          │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│                   Data Access Layer                 │
│  Repository (FondoRepository.java)                  │
│  - Extends JpaRepository                            │
│  - Custom queries                                   │
│  - @Query, query derivation                         │
│  - Comunicación con BD                              │
└────────┬────────────────────────────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────┐
│                   Database Layer                    │
│  MySQL / H2                                         │
│  - Persistencia de datos                            │
│  - Constraints e indexes                            │
│  - Transacciones ACID                               │
└─────────────────────────────────────────────────────┘

Cross-Cutting Concerns (Todos los servicios):
├─ Exception Handling (GlobalExceptionHandler)
├─ DTO Mapping (Entity ↔ DTO)
├─ Logging (Slf4j con @Slf4j)
└─ Validation (Jakarta @Valid, @NotNull, etc)
```

## Ciclo de Vida de una Transacción

```
Cliente HTTP
    │
    ▼
┌─────────────────────────────────────────┐
│ 1. Controller recibe request            │
│    @Valid valida input                  │
│    POST /api/v1/fondos                  │
│    Body: {nombre, descripcion, valor}   │
└──────────────┬────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│ 2. Service.crearFondo() comienza        │
│    @Transactional abre transacción      │
└──────────────┬──────────────────────────┘
               │
               ▼
┌──────────────────────────────────────────┐
│ 3. Validar lógica de negocio            │
│    - ¿Nombre duplicado?                 │
│    - ¿Montos válidos?                   │
│    - Lanzar excepción si hay error      │
└──────────────┬──────────────────────────┘
               │
    ┌──────────┴──────────┐
    ▼ (Error)             ▼ (Ok)
┌────────────────┐  ┌──────────────────────────┐
│ Catch Exception│  │ 4. Crear entidad Fondo   │
│ Rollback trans │  │    @PrePersist invocado  │
│ Return 400/409│  │    Timestamps automáticos │
└────────────────┘  └──────────────┬────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────┐
                    │ 5. Repository.save()     │
                    │    SQL INSERT generado   │
                    │    Persist a MySQL       │
                    └──────────────┬────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────┐
                    │ 6. @Transactional commit │
                    │    Cambios guardados     │
                    │    Transacción cierra    │
                    └──────────────┬────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────┐
                    │ 7. Mapear Entity → DTO   │
                    │    Preparar respuesta    │
                    └──────────────┬────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────┐
                    │ 8. Response HTTP 201     │
                    │    Body: FondoDTO JSON   │
                    └──────────────┬────────────┘
                                   │
                                   ▼
                              Cliente HTTP
```

## Patrones de Comunicación Inter-Servicio

```
┌──────────────────────────────────────────────────────────┐
│  INVERSIONES-SERVICE quiere dato de FONDOS-SERVICE      │
└──────────────────────────────────────────────────────────┘

┌───────────────────┐
│ InversionService  │
│ .crearInversion() │
└────────┬──────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────┐
│ FondoServiceClient.obtenerFondo(fondoId)                │
│ - @Component bean injected en Service                   │
│ - RestTemplate.getForObject() encapsulado               │
│ - URL: http://localhost:8081/api/v1/fondos/{id}        │
│ - Timeout: 5s connect, 10s read                         │
└────────┬──────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────┐
│ HTTP GET /api/v1/fondos/1                               │
└────────┬──────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────┐
│ FondoService.obtenerFondoPorId(1)                       │
│ - Repository.findById(1)                                │
│ - Mapear Entity → DTO                                    │
│ - Return FondoDTO con todos los datos                   │
└────────┬──────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────────────────────┐
│ HTTP 200 OK + JSON Body                                 │
│ {id: 1, nombre: "...", valorCuota: 50.00, ...}          │
└────────┬──────────────────────────────────────────────────┘
         │
         ▼
┌────────────────────────────┐
│ FondoServiceClient         │
│ - Catch any HttpClientError│
│ - Throw FondoServiceException│
│ - Service rollback transaction│
│ - Return 502 Bad Gateway   │
└────────────────────────────┘

Error handling:
├─ Si Fondos está DOWN → FondoServiceException → 502
├─ Si timeout → FondoServiceException → 502
├─ Si 404 → FondoServiceException → 502
└─ Success → Continuar con lógica
```

---

## Resumen de Arquitectura

| Aspecto | Valor |
|---------|-------|
| **Tipo** | Microservicios REST |
| **Comunicación** | HTTP + JSON |
| **Persistencia** | MySQL 8.0+ (Database per Service) |
| **Framework** | Spring Boot 4.1.1 |
| **Java** | 17 (LTS) |
| **Testing Local** | MySQL local + H2 in-memory |
| **Escalabilidad** | Horizontal (servicios independientes) |
| **Disponibilidad** | Alta (servicios descoplados) |
| **Seguridad** | JWT (⏳ en BFF - por implementar) |

**Implementado:** ✅ Arquitectura base completada
**Pendiente:** ⏳ Autenticación y autorización centralizada (BFF)
