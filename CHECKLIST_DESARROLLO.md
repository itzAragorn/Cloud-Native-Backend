# 📊 Banco Cloud - Checklist de Desarrollo

## Fases de Implementación

### ✅ FASE 1: Fondos-Service (COMPLETADA)
- [x] Entity `Fondo` con validaciones
- [x] Enum `EstadoFondo` (ACTIVO, INACTIVO, SUSPENDIDO)
- [x] DTOs: `FondoDTO`, `FondoRequestDTO`, `ActualizarValorCuotaRequestDTO`
- [x] Repository `FondoRepository` con custom queries
- [x] Service `FondoService` (6 métodos)
  - [x] crearFondo() - POST
  - [x] obtenerTodosFondos() - GET
  - [x] obtenerFondosActivos() - GET
  - [x] obtenerFondoPorId() - GET
  - [x] actualizarValorCuota() - PUT
  - [x] cambiarEstado() - PATCH
- [x] Controller `FondoController` (6 endpoints)
- [x] Exception handling
  - [x] FondoNotFoundException
  - [x] FondoDuplicadoException
  - [x] ErrorResponse
  - [x] GlobalExceptionHandler
- [x] application.properties (port 8081, MySQL config)
- [x] pom.xml (dependencies configured)
- [x] @PrePersist/@PreUpdate timestamp management

### ✅ FASE 2: Inversiones-Service (COMPLETADA)
- [x] Entity `Inversion` con validaciones
- [x] Enum `EstadoInversion` (ACTIVA, RETIRADA, CANCELADA)
- [x] DTOs: `CrearInversionRequestDTO`, `InversionDTO`, `PortafolioDTO`, `RendimientoDTO`, `FondoDTO`
- [x] Repository `InversionRepository` (6 custom queries)
- [x] Service `InversionService` (7 métodos)
  - [x] crearInversion() - Consulta fondos-service, calcula cuotas
  - [x] obtenerMisInversiones()
  - [x] obtenerInversionesActivas()
  - [x] obtenerInversion()
  - [x] obtenerMiPortafolio() - Cálculos de valor y rendimiento
  - [x] obtenerRendimientoInversion()
  - [x] obtenerRendimientoUsuario()
  - [x] cambiarEstado()
- [x] Controller `InversionController` (6 endpoints)
- [x] FondoServiceClient (RestTemplate)
  - [x] obtenerFondo()
  - [x] obtenerValorCuota()
- [x] RestClientConfig (RestTemplate bean con timeout)
- [x] Exception handling
  - [x] InversionNotFoundException
  - [x] FondoServiceException
  - [x] ErrorResponse
  - [x] GlobalExceptionHandler
- [x] application.properties (port 8082, fondos-service.url config)
- [x] pom.xml
- [x] Cálculos de:
  - [x] cuotas = monto / valorCuota (RoundingMode.HALF_UP, 4 decimales)
  - [x] valorTotalActual = cuotas × valorActualCuota
  - [x] rendimientoAbsoluto = valorTotalActual - montoInvertido
  - [x] rendimientoPorcentaje (con división segura por cero)

### ✅ FASE 3: Usuarios-Service (COMPLETADA)
- [x] Entity `Usuario` con validaciones
- [x] Enum `Rol` (ADMIN, CLIENTE)
- [x] Enum `EstadoUsuario` (ACTIVO, BLOQUEADO, INACTIVO)
- [x] DTOs: `CrearUsuarioRequestDTO`, `UsuarioDTO`, `AutorizarUsuarioRequestDTO`
- [x] Repository `UsuarioRepository` (6 custom queries)
- [x] Service `UsuarioService` (8 métodos)
  - [x] crearUsuario() - ADMIN nace autorizado, CLIENTE no
  - [x] obtenerTodosUsuarios()
  - [x] obtenerUsuariosPorRol()
  - [x] obtenerClientesPendientesAutorizacion()
  - [x] obtenerUsuarioPorId()
  - [x] obtenerUsuarioPorEmail()
  - [x] autorizarUsuario()
  - [x] bloquearUsuario()
  - [x] desbloquearUsuario()
  - [x] verificarAcceso() - Valida autorización + estado ACTIVO
- [x] Controller `UsuarioController` (9 endpoints)
- [x] Exception handling
  - [x] UsuarioNotFoundException
  - [x] EmailDuplicadoException
  - [x] ErrorResponse
  - [x] GlobalExceptionHandler
- [x] application.properties (port 8083)
- [x] pom.xml
- [x] Unique constraint en email
- [x] Business logic: solo ADMIN nace autorizado

### ✅ FASE 4: BFF-Service (COMPLETADA)
- [x] Spring Security dependency
- [x] Spring Security OAuth2 Resource Server dependency
- [x] JWT Provider configuration
- [x] SecurityConfig
  - [x] Configurar Authentication Manager
  - [x] Configurar JWT validation
  - [x] Configurar issuer y audience (Azure AD ready)
- [x] RestTemplateConfig (con timeouts)
- [x] Role-based authorization
  - [x] @PreAuthorize annotations en todos los controllers
  - [x] Custom authorities from JWT claims
- [x] CORS configuration
- [x] Request routing to microservices
  - [x] FondosServiceClient (6 métodos)
  - [x] InversionesServiceClient (6 métodos)
  - [x] UsuariosServiceClient (9 métodos)
- [x] FondosProxyController (6 endpoints)
- [x] InversionesProxyController (6 endpoints)
- [x] UsuariosProxyController (9 endpoints)
- [x] HealthCheckController (health + info)
- [x] GlobalExceptionHandler
  - [x] ServiceException handling
  - [x] AuthenticationException handling
  - [x] AccessDeniedException handling
  - [x] General exception handling
- [x] ErrorResponse DTO
- [x] application.properties (configurado)
- [x] pom.xml (dependencias completas)
- [x] Compilación exitosa
  - [ ] InversionServiceProxy
  - [ ] UsuarioServiceProxy
- [ ] CORS configuration
- [ ] Error handling y response translation
- [ ] application.properties (port 8080, Azure AD config)
- [ ] pom.xml

### ⏳ FASE 5: Testing & Integration (POR IMPLEMENTAR)
- [ ] Unit tests para Services (Mockito)
- [ ] Integration tests para Controllers (MockMvc)
- [ ] Test fixtures y data builders
- [ ] Coverage > 80%
- [ ] End-to-end flow tests
- [ ] Error scenarios testing
- [ ] Load testing

### ⏳ FASE 6: React Frontend (POR IMPLEMENTAR)
- [ ] Autenticación con MSAL (Azure AD)
- [ ] Protected routes
- [ ] JWT interceptor (agregar header Authorization)
- [ ] API Gateway proxy
- [ ] Components principales:
  - [ ] Login page
  - [ ] Dashboard
  - [ ] Fund listing
  - [ ] Investment creation form
  - [ ] Portfolio view
  - [ ] Performance charts
- [ ] Error handling UI
- [ ] Loading states

### ⏳ FASE 7: Deployment (POR IMPLEMENTAR)
- [ ] Docker images para cada servicio
- [ ] docker-compose.yml para local testing
- [ ] AWS infrastructure setup
  - [ ] VPC configuration
  - [ ] RDS for MySQL
  - [ ] EC2 o ECS for microservices
  - [ ] API Gateway
- [ ] CI/CD pipeline (GitHub Actions o AWS CodePipeline)
- [ ] Environment variables management
- [ ] Secrets management (AWS Secrets Manager)
- [ ] Logging y monitoring (CloudWatch)
- [ ] Health checks y auto-recovery

---

## Documentación Completada ✅

- [x] DOCUMENTACION_API.md
  - [x] Requisitos previos
  - [x] Setup local con comandos
  - [x] Estructura de proyectos
  - [x] Todos los endpoints documentados con ejemplos JSON
  - [x] Flujos de negocio
  - [x] Códigos de error HTTP
  - [x] Postman examples

- [x] QUICKSTART.md
  - [x] Setup de 5 minutos
  - [x] Comandos copy-paste
  - [x] Verificación de requisitos
  - [x] Troubleshooting
  - [x] Comandos útiles

- [x] BancoCloud-Postman-Collection.json
  - [x] Todos los endpoints
  - [x] Ejemplos de request/response
  - [x] Flujo completo tutorial

- [x] Este checklist

---

## Estándares de Código Implementados ✅

### Validación
- [x] @Valid en controllers
- [x] @NotNull, @NotBlank, @Size, @Email, @DecimalMin en DTOs y entities
- [x] Mensajes de error personalizados

### Logging
- [x] @Slf4j en todos los Services
- [x] Log en entrada de métodos principales
- [x] Log en errores con detalles
- [x] Niveles apropiados (INFO, DEBUG, ERROR)

### Transacciones
- [x] @Transactional en services
- [x] readOnly = true para queries
- [x] Manejo de excepciones transaccionales

### Architecture
- [x] Separación de capas (controller → service → repository)
- [x] No lógica en controllers
- [x] DTOs para input/output
- [x] Entities solo en repository
- [x] Mapping explícito (Entity ↔ DTO)

### Manejo de Errores
- [x] Custom exceptions por dominio
- [x] GlobalExceptionHandler centralizado
- [x] ErrorResponse DTO consistente
- [x] HTTP status codes correctos
- [x] Timestamps en errores

### Database
- [x] spring.jpa.hibernate.ddl-auto = update
- [x] @PrePersist para fechaCreacion
- [x] @PreUpdate para fechaActualizacion
- [x] Enums con @Enumerated(EnumType.STRING)
- [x] Unique constraints donde aplica

### Configuration
- [x] application.properties por servicio
- [x] Port configuration centralizado
- [x] Database credentials configurables
- [x] Logging levels configurables
- [x] Spring config beans para dependencies inyectadas

---

## Pruebas Manuales Completadas ✅

- [x] Crear fondo (validar campos requeridos)
- [x] Listar fondos (incluyendo filtro activos)
- [x] Actualizar valor de cuota
- [x] Crear usuario (ADMIN y CLIENTE)
- [x] Autorizar usuario
- [x] Bloquear/desbloquear usuario
- [x] Crear inversión (valida que fondo exista)
- [x] Cálculo de cuotas
- [x] Ver portafolio con cambios de valor
- [x] Cálculo de rendimiento
- [x] Error handling (404, 409, 400)

---

## Próximas Tareas Recomendadas (Orden de Prioridad)

### 🔴 CRÍTICO (Semana 1)
1. [ ] Implementar bff-service completo
2. [ ] Testing de los 3 servicios
3. [ ] Documentación de datos en DB (schema SQL)
4. [ ] Validar todos los flujos end-to-end

### 🟠 ALTO (Semana 2)
5. [ ] React frontend básico
6. [ ] Integración con Azure AD
7. [ ] Docker setup
8. [ ] Postman tests automatizados (Newman)

### 🟡 MEDIO (Semana 3)
9. [ ] CI/CD pipeline
10. [ ] Monitoring y logging
11. [ ] Performance testing
12. [ ] Security audit

### 🟢 BAJO (Cuando sea)
13. [ ] Swagger/OpenAPI
14. [ ] Caching
15. [ ] Rate limiting
16. [ ] GraphQL alternative

---

## Métricas de Implementación

| Métrica | Fondos | Inversiones | Usuarios | Total |
|---------|--------|-------------|----------|-------|
| Entities | 2 | 2 | 3 | 7 |
| DTOs | 3 | 5 | 3 | 11 |
| Repositories | 1 | 1 | 1 | 3 |
| Services | 1 (6 mét) | 1 (8 mét) | 1 (8 mét) | 3 (22 mét) |
| Controllers | 1 (6 end) | 1 (6 end) | 1 (9 end) | 3 (21 end) |
| Exceptions | 3 | 3 | 3 | 9 |
| **Total Clases** | **11** | **14** | **14** | **39** |
| Lines of Code | ~800 | ~1000 | ~900 | ~2700 |

---

## Comandos de Referencia Rápida

```bash
# Compilar un servicio
cd fondos-service && mvn clean install

# Ejecutar un servicio
mvn spring-boot:run

# Limpiar y compilar todos
for dir in fondos-service inversiones-service usuarios-service; do
  cd $dir && mvn clean install && cd ..
done

# Test con curl
curl -X GET http://localhost:8081/api/v1/fondos

# Conectar a BD
mysql -u root -p fondos_db
SELECT * FROM fondos;
```

---

**Última actualización**: 2024-01-15  
**Estado Global**: ✅ Fase 1-3 COMPLETADAS | ⏳ Fase 4-7 PENDIENTES  
**Estimado para finalización**: 
- BFF: 1-2 días
- Frontend React: 3-4 días  
- Testing completo: 2-3 días
- AWS Deployment: 2-3 días
- **Total**: ~2 semanas

**Próximo enfoque**: Implementar bff-service con Spring Security OAuth2
