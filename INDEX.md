# 📑 Banco Cloud - Guía de Navegación de Documentación

## 🎯 Elige tu Ruta Según Tu Rol

### 👨‍💼 **Gerente / Product Owner**
*Objetivo: Entender estado y progreso*

Leer en este orden:
1. [README.md](README.md) - Resumen ejecutivo (5 min) ⭐
2. [CHECKLIST_DESARROLLO.md](CHECKLIST_DESARROLLO.md) - Estado del proyecto y métricas (5 min)
3. [RESUMEN_VISUAL.md](RESUMEN_VISUAL.md) - Diagramas y gráficos (3 min)

**Resultado**: Sabes qué se completó, qué falta y métricas del proyecto

---

### 👨‍💻 **Desarrollador - Primeras Horas**
*Objetivo: Tener el ambiente corriendo en 15 minutos*

Leer en este orden:
1. **[QUICKSTART.md](QUICKSTART.md)** ⭐ COMENZAR AQUÍ (5 min)
   - Paso 1: Crear bases de datos
   - Paso 2: Compilar proyectos
   - Paso 3: Ejecutar servicios
   - Paso 4: Probar con curl

2. [README.md](README.md) - Arquitectura del sistema (10 min)

3. [BancoCloud-Postman-Collection.json](BancoCloud-Postman-Collection.json) - Importar en Postman
   - 17 requests preconfigurados
   - Ejecutar flujos de negocio

**Resultado**: Ambiente local corriendo, servicios respondiendo

---

### 🔧 **Desarrollador - Desarrollo Activo**
*Objetivo: Entender endpoints y lógica de negocio*

Tener abiertos mientras desarrollas:
1. [DOCUMENTACION_API.md](DOCUMENTACION_API.md) - **Referencia técnica completa**
   - Todos los endpoints documentados
   - Ejemplos de request/response
   - Códigos de error
   - Flujos de negocio

2. [CHECKLIST_DESARROLLO.md](CHECKLIST_DESARROLLO.md) - Status de tareas
   - Qué está completado ✅
   - Qué falta ⏳
   - Estructura de código

3. Código fuente:
   - `fondos-service/src/main/java/com/bancocloud/`
   - `inversiones-service/src/main/java/com/bancocloud/`
   - `usuarios-service/src/main/java/com/bancocloud/`

**Resultado**: Puedes implementar nuevas features o corregir bugs

---

### 🏗️ **Arquitecto / Tech Lead**
*Objetivo: Entender diseño, patrones y escalabilidad*

Leer en profundidad:
1. [ARCHITECTURE.md](ARCHITECTURE.md) - **Diagramas de componentes y flujos** (20 min)
   - Estructura de microservicios
   - Comunicación inter-servicio
   - Database per microservice pattern
   - Manejo de errores distribuido

2. [README.md](README.md) - Sección "Comunicación Inter-Servicios" (5 min)

3. [DOCUMENTACION_API.md](DOCUMENTACION_API.md) - Sección "Arquitectura de Comunicación" (10 min)

4. Revisar código ejemplo:
   - `inversiones-service/src/main/java/com/bancocloud/client/FondoServiceClient.java`
   - `usuarios-service/src/main/java/com/bancocloud/service/UsuarioService.java`

5. [JWT_AUTH_GUIDE.md](JWT_AUTH_GUIDE.md) - Seguridad (10 min)

**Resultado**: Comprendes arquitectura, decisiones de diseño, roadmap de escalabilidad

---

### 🧪 **QA / Tester**
*Objetivo: Probar sistema completo, documentar bugs*

Flujo de trabajo:
1. [QUICKSTART.md](QUICKSTART.md) - Setup (5 min)

2. [BancoCloud-Postman-Collection.json](BancoCloud-Postman-Collection.json) - Importar en Postman
   - 17 requests ordenados por flujo
   - Ejecutar uno tras otro

3. [DOCUMENTACION_API.md](DOCUMENTACION_API.md) - Referencia mientras testeas
   - Códigos de error esperados
   - Validaciones
   - Límites y edge cases

4. [RESUMEN_VISUAL.md](RESUMEN_VISUAL.md) - Flujos de negocio (para planear test cases)

**Checklist de pruebas:**
- [ ] Setup local completado
- [ ] 3 servicios corriendo en puertos 8081, 8082, 8083
- [ ] Importar collection en Postman
- [ ] Ejecutar 17 requests sin errores
- [ ] Intentar casos de error (fondos duplicados, email duplicado, etc.)
- [ ] Verificar logs en consola

**Resultado**: Documentación completa de bugs encontrados o confirmación de que todo funciona

---

## 📚 Referencia Rápida de Documentos

| Archivo | Propósito | Lectura | Audiencia |
|---------|-----------|---------|-----------|
| **README.md** | 🎯 Punto de entrada: estado, features, arquitectura | 10 min | **TODOS** |
| **INDEX.md** (este) | 🧭 Navegación según rol | 5 min | **TODOS** |
| **QUICKSTART.md** | ⚡ Setup en 5 minutos | 5 min | Devs, QA |
| **DOCUMENTACION_API.md** | 📖 Referencia técnica completa de endpoints | 30 min (referencia) | Devs, QA, Arquitecto |
| **ARCHITECTURE.md** | 🏗️ Diagramas y decisiones de diseño | 20 min | Arquitecto, Tech Lead |
| **CHECKLIST_DESARROLLO.md** | ✅ Status de tareas, métricas, roadmap | 5 min | PM, Tech Lead, Devs |
| **RESUMEN_VISUAL.md** | 📊 Gráficos de entrega, flujos de negocio | 5 min | Gerentes, PM |
| **DOCKER.md** | 🐳 Instrucciones de Docker Compose | 5 min | DevOps, Devs |
| **JWT_AUTH_GUIDE.md** | 🔐 Autenticación JWT, seguridad | 10 min | Arquitecto, Devs Security |
| **BancoCloud-Postman-Collection.json** | 🧪 17 requests preconfigurados | Usar mientras testas | Devs, QA |

---

## 🚀 Caminos Rápidos (Quick Paths)

### "Quiero compilar y correr YA"
1. QUICKSTART.md (sigue pasos 1-4)
2. Espera 5 minutos
3. `curl http://localhost:8081/api/v1/fondos`

### "Necesito saber si esto está listo para producción"
1. README.md (Fases Pendientes)
2. CHECKLIST_DESARROLLO.md (Status)
3. Respuesta: 75% completado, falta BFF + frontend + tests

### "¿Cómo comunican los servicios?"
1. ARCHITECTURE.md (diagrama de componentes)
2. README.md (sección "Comunicación Inter-Servicios")
3. Código: `FondoServiceClient.java`

### "¿Cuáles son todos los endpoints?"
1. DOCUMENTACION_API.md (tabla de endpoints)
2. O: BancoCloud-Postman-Collection.json (ver en Postman)

### "¿Qué hace cada servicio?"
1. README.md (sección "Lo Que Se Implementó")
2. README.md (sección "Flujos de Negocio")

---

## ✅ Checklist: Estoy Listo Para...

### ...Empezar a Desarrollar
- [ ] Leí QUICKSTART.md completamente
- [ ] Ejecuté `mvn clean install` en 3 servicios
- [ ] Vi ✅ BUILD SUCCESS en consola
- [ ] Servicios corriendo sin errores (3 terminales)
- [ ] Probé al menos 1 curl exitoso

### ...Hacer Code Review
- [ ] Leí ARCHITECTURE.md
- [ ] Entiendo patrón Database per Microservice
- [ ] Entiendo RestTemplate + timeouts para inter-service
- [ ] Revisé al menos 2 archivos de código
- [ ] Conozco estructura de exceptions + global handler

### ...Hacer QA Testing
- [ ] Leí QUICKSTART.md (setup)
- [ ] Importé Postman collection
- [ ] Ejecuté todos los 17 requests sin errores
- [ ] Intenté varios casos de error
- [ ] Documenté bugs en GIT issues (si los hay)

### ...Presentar a Stakeholders
- [ ] Leí README.md completo
- [ ] Vi RESUMEN_VISUAL.md
- [ ] Tengo respuestas a: qué se hizo, cuánto falta, timeline estimado
- [ ] Conozco "Fases Pendientes" (BFF, Frontend, Tests, AWS)

---

## 🎬 Quick Demo (2 minutos)

Si solo tienes 2 minutos:

1. **El proyecto tiene 3 servicios funcionando:**
   - fondos-service (8081): Gestión de fondos
   - inversiones-service (8082): Portafolio e inversiones
   - usuarios-service (8083): Usuarios y autorización

2. **Flujo principal:**
   ```
   ADMIN crea Fondo → Cliente se registra → ADMIN autoriza 
   → Cliente invierte → Ve su portafolio con rendimientos
   ```

3. **Estado:** ✅ 3 de 4 servicios completados, ~2700 líneas de código Java

4. **Próximo:** Implementar BFF (Spring Security + JWT)

---

## 🚀 Setup Rápido (Copy-Paste)

```bash
# 1. Crear bases de datos
mysql -u root -p
# Copiar y pegar esto en MySQL:
CREATE DATABASE fondos_db;
CREATE DATABASE inversiones_db;
CREATE DATABASE usuarios_db;
EXIT;

# 2. Compilar servicios (en el workspace)
cd fondos-service && mvn clean install && cd ..
cd inversiones-service && mvn clean install && cd ..
cd usuarios-service && mvn clean install && cd ..

# 3. Ejecutar (en 3 terminales separadas)
# Terminal 1:
cd fondos-service && mvn spring-boot:run

# Terminal 2:
cd inversiones-service && mvn spring-boot:run

# Terminal 3:
cd usuarios-service && mvn spring-boot:run

# 4. Probar (Terminal 4)
curl http://localhost:8081/api/v1/fondos
curl http://localhost:8083/api/v1/usuarios
curl http://localhost:8082/api/v1/inversiones/mi-portafolio?usuarioId=1
```

---

## 📊 Estructura de Carpetas Completadas

```
EV1/  ← Estás aquí
├── 📄 README.md                                ⭐ Comienza aquí
├── 📄 QUICKSTART.md                           ⭐ Setup rápido
├── 📄 DOCUMENTACION_API.md                    ⭐ Referencia técnica
├── 📄 CHECKLIST_DESARROLLO.md                 ⭐ Status y roadmap
├── 📄 BancoCloud-Postman-Collection.json      ⭐ Tests listos
├── 📄 INDEX.md                                ← Este archivo
│
├── fondos-service/                            ✅ COMPLETADO
│   ├── src/main/java/com/bancocloud/fondos_service/
│   │   ├── model/          (Fondo, EstadoFondo)
│   │   ├── dto/            (FondoDTO, FondoRequestDTO)
│   │   ├── repository/     (FondoRepository)
│   │   ├── service/        (FondoService)
│   │   ├── controller/     (FondoController)
│   │   └── exception/      (GlobalExceptionHandler)
│   ├── application.properties
│   └── pom.xml
│
├── inversiones-service/                       ✅ COMPLETADO
│   ├── src/main/java/com/bancocloud/inversiones_service/
│   │   ├── model/          (Inversion, EstadoInversion)
│   │   ├── dto/            (InversionDTO, PortafolioDTO)
│   │   ├── repository/     (InversionRepository)
│   │   ├── service/        (InversionService)
│   │   ├── controller/     (InversionController)
│   │   ├── client/         (FondoServiceClient)
│   │   ├── config/         (RestClientConfig)
│   │   └── exception/      (GlobalExceptionHandler)
│   ├── application.properties
│   └── pom.xml
│
├── usuarios-service/                         ✅ COMPLETADO
│   ├── src/main/java/com/bancocloud/usuarios_service/
│   │   ├── model/          (Usuario, Rol, EstadoUsuario)
│   │   ├── dto/            (UsuarioDTO, CrearUsuarioRequestDTO)
│   │   ├── repository/     (UsuarioRepository)
│   │   ├── service/        (UsuarioService)
│   │   ├── controller/     (UsuarioController)
│   │   └── exception/      (GlobalExceptionHandler)
│   ├── application.properties
│   └── pom.xml
│
└── bff-service/                              ⏳ PENDIENTE
    ├── src/main/java/...
    ├── application.properties
    └── pom.xml
```

---

## ✅ Lo Que Está Completado

- ✅ **3 Microservicios** completamente implementados
- ✅ **21 Endpoints** REST documentados
- ✅ **39 Clases Java** siguiendo best practices
- ✅ **Validación robusta** de entrada/salida
- ✅ **Manejo centralizado** de errores
- ✅ **Comunicación inter-servicio** (inversiones ↔ fondos)
- ✅ **Bases de datos independientes** per servicio
- ✅ **Logging completo** con Slf4j
- ✅ **Transacciones ACID** con @Transactional
- ✅ **Documentación técnica** completa
- ✅ **Postman collection** con ejemplos

---

## ⏳ Lo Que Falta

| Tarea | Prioridad | Estimado | Estado |
|-------|-----------|----------|--------|
| BFF Service (JWT + Routing) | 🔴 CRÍTICO | 2 días | ⏳ |
| Testing (Unit + Integration) | 🔴 CRÍTICO | 2 días | ⏳ |
| React Frontend | 🟠 ALTO | 3-4 días | ⏳ |
| Azure AD Integration | 🟠 ALTO | 1 día | ⏳ |
| Docker setup | 🟠 ALTO | 1 día | ⏳ |
| CI/CD Pipeline | 🟡 MEDIO | 1 día | ⏳ |
| AWS Deployment | 🟡 MEDIO | 2 días | ⏳ |

---

## 🎓 Estándares Implementados

Este proyecto usa:
- ✅ **Spring Boot 4.1.1** (latest)
- ✅ **Java 17** (LTS)
- ✅ **Spring Data JPA** + Hibernate
- ✅ **MySQL 8.0+** (con H2 para testing)
- ✅ **REST conventions** (HTTP status, resource naming)
- ✅ **Clean Architecture** (layers: controller → service → repository)
- ✅ **SOLID Principles** (separation of concerns)
- ✅ **Design Patterns** (DTO, Repository, Service, Factory)
- ✅ **Lombok** (reduce boilerplate)
- ✅ **Slf4j** (structured logging)

---

## 🔗 Referencias Externas Recomendadas

Mientras trabajas en el proyecto, estos son útiles:

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Data JPA Guide](https://spring.io/projects/spring-data-jpa)
- [Maven Central Repository](https://mvnrepository.com/)
- [REST API Best Practices](https://restfulapi.net/)
- [HTTP Status Codes](https://httpwg.org/specs/rfc7231.html#status.codes)

---

## 💬 Preguntas Comunes

**P: ¿Dónde veo los logs?**
A: En la consola de cada terminal donde corre el servicio (lineas rojas = errores)

**P: ¿Cómo cambio los datos de conexión a MySQL?**
A: Edita `application.properties` en cada servicio (spring.datasource.*)

**P: ¿Puedo usar H2 en lugar de MySQL?**
A: Sí, descomenta las líneas H2 en properties y comenta las MySQL

**P: ¿Cómo pruebo si un servicio está corriendo?**
A: `curl http://localhost:8081/api/v1/fondos` (para cada puerto)

**P: ¿Los datos persisten después de reiniciar?**
A: Sí, están en la base de datos MySQL (H2 solo si lo usas)

**P: ¿Puedo ejecutar los servicios en diferentes máquinas?**
A: Sí, solo cambia la URL en `fondos-service.url` en inversiones-service properties

---

## 📞 Soporte

Si algo no funciona:
1. Consulta [QUICKSTART.md](QUICKSTART.md) - Troubleshooting section
2. Verifica que MySQL esté corriendo: `brew services list | grep mysql`
3. Verifica que los 3 servicios estén compilados: `mvn clean install`
4. Verifica que los puertos 8081, 8082, 8083 no estén en uso: `lsof -i :8081`

---

## 🚀 Próximos Pasos Recomendados

1. **HOY**: Ejecuta [QUICKSTART.md](QUICKSTART.md) para setup local
2. **HOY**: Importa [BancoCloud-Postman-Collection.json](BancoCloud-Postman-Collection.json) en Postman
3. **MAÑANA**: Comienza a revisar código de los servicios
4. **PRÓX SEMANA**: Implementa bff-service con Spring Security

---

**Versión**: 1.0  
**Última actualización**: 2024-01-15  
**Estado**: ✅ Documentación Completa | ⏳ Desarrollo Continuo  
**Siguiente hito**: Implementar BFF Service

**¡Que disfrutes desarrollando! 🎉**
