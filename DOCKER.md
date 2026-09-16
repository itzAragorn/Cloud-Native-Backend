# 🐳 BancoCloud - Docker Deployment

**Estado**: ✅ Dockerfiles creados para todos los servicios  
**Última actualización**: 2026-09-14

---

## 📋 Requisitos

- Docker >= 20.10
- Docker Compose >= 2.0
- ~4GB RAM disponible

---

## ⚡ Inicio Rápido (3 pasos)

### 1️⃣ Build de todas las imágenes

```bash
docker-compose build
```

Tarda ~2-3 minutos la primera vez.

### 2️⃣ Iniciar todos los servicios

```bash
docker-compose up -d
```

### 3️⃣ Verificar que todo esté UP

```bash
docker-compose ps
```

✅ Esperado: 5 contenedores en estado "Up" o "Up (healthy)"

```
NAME                            STATUS
bancocloud-mysql                Up (healthy)
fondos-service                  Up (running)
inversiones-service             Up (running)
usuarios-service                Up (running)
bff-service                      Up (running)
```

---

## 📊 Arquitectura Docker

```
Docker Network (bancocloud-net)
├── MySQL Container
│   └── 3306:3306 (MySQL Server)
│
├── Fondos Service
│   ├── Java 17 + Spring Boot
│   └── 8081:8081 (REST API)
│
├── Inversiones Service
│   ├── Java 17 + Spring Boot
│   └── 8082:8082 (REST API)
│
├── Usuarios Service
│   ├── Java 17 + Spring Boot
│   └── 8083:8083 (REST API)
│
└── BFF Service
    ├── Java 17 + Spring Boot
    └── 8080:8080 (API Gateway)
```

---

## 🔍 Comandos Útiles

### Ver logs en vivo
```bash
# Todos los servicios
docker-compose logs -f

# Servicio específico
docker-compose logs -f fondos-service

# Últimas 50 líneas
docker-compose logs --tail=50 inversiones-service

# Con timestamps
docker-compose logs -f --timestamps usuarios-service
```

### Parar servicios
```bash
# Parar sin eliminar (puedes hacer up -d después)
docker-compose stop

# Parar y eliminar contenedores (BUT keep volumes)
docker-compose down

# Eliminar TODO incluyendo bases de datos (⚠️ Cuidado!)
docker-compose down -v
```

### Reiniciar un servicio
```bash
docker-compose restart fondos-service
docker-compose restart inversiones-service
docker-compose restart usuarios-service
```

### Entrar a la consola de un contenedor
```bash
# Base de datos MySQL
docker-compose exec mysql mysql -u root -p

# Dentro del contenedor, password es: root

# Ver logs de aplicación Java (mientras el contenedor corre)
docker-compose logs -f bff-service
```

### Limpieza completa
```bash
docker-compose down -v  # Elimina todo
docker system prune     # Limpia imágenes no usadas
```

---

## 🧪 Testing E2E (Después de `docker-compose up -d`)

### Test 1: ¿Está MySQL activo?
```bash
curl http://localhost:3306
# Esperado: Conexión rechazada (normal, no es HTTP)
# o ver en logs: mysql | ready for connections
```

### Test 2: ¿Fondos Service está respondiendo?
```bash
curl http://localhost:8081/api/v1/fondos

# Esperado (200 OK):
# []
```

### Test 3: ¿Usuarios Service está respondiendo?
```bash
curl http://localhost:8083/api/v1/usuarios

# Esperado (200 OK):
# []
```

### Test 4: ¿Inversiones Service está respondiendo?
```bash
curl http://localhost:8082/api/v1/inversiones/mi-portafolio?usuarioId=1

# Esperado (normalmente 400 o datos vacíos, pero NO timeout)
```

### Test 5: ¿BFF Service está respondiendo?
```bash
curl http://localhost:8080/health

# Esperado: 200 OK con status del servicio
```

### Test Completo: Crear fondo + inversión
```bash
# 1. Crear un fondo
curl -X POST http://localhost:8081/api/v1/fondos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Fondo Test",
    "descripcion": "Test desde Docker",
    "valorCuota": 50.00
  }'

# Esperado: 201 Created
# Response: {"id": 1, "nombre": "Fondo Test", ...}

# 2. Crear inversión
curl -X POST http://localhost:8082/api/v1/inversiones \
  -H "Content-Type: application/json" \
  -d '{
    "usuarioId": 1,
    "fondoId": 1,
    "montoInvertido": 1000.00
  }'

# Esperado: 201 Created
```

---

## 📡 Puertos y URLs

| Servicio | Puerto | URL | Uso |
|----------|--------|-----|-----|
| **MySQL** | 3306 | `localhost:3306` | Base de datos |
| **Fondos** | 8081 | `http://localhost:8081` | Gestión de fondos |
| **Inversiones** | 8082 | `http://localhost:8082` | Gestión de inversiones |
| **Usuarios** | 8083 | `http://localhost:8083` | Gestión de usuarios |
| **BFF** | 8080 | `http://localhost:8080` | API Gateway (próximamente) |

---

## ⚙️ Configuración

### Variables de Entorno (en `docker-compose.yml`)

Cada servicio tiene configuradas:

```yaml
environment:
  - SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/[db_name]
  - SPRING_DATASOURCE_USERNAME: root
  - SPRING_DATASOURCE_PASSWORD: root
  - SPRING_JPA_HIBERNATE_DDL_AUTO: update
  - JWT_SECRET: BancoCloud-Super-Secret-Key-Change-In-Production
  - JWT_EXPIRATION: 86400000
```

Para cambiar la configuración:

1. Editar `docker-compose.yml`
2. Ejecutar `docker-compose down`
3. Ejecutar `docker-compose up -d`
4. Ver logs con `docker-compose logs -f`

### Cambiar contraseña MySQL

En `docker-compose.yml`, busca:
```yaml
mysql:
  environment:
    MYSQL_ROOT_PASSWORD: root
```

Cámbialo, luego:
```bash
docker-compose down -v  # Elimina volumen
docker-compose up -d     # Crea nuevo con nueva contraseña
```

---

## 🐛 Troubleshooting

### "ERROR: Service 'mysql' failed to build"
```bash
docker-compose down -v
docker-compose build --no-cache
docker-compose up -d
```

### "ERROR: Connection refused on port 8081"
```bash
# Verificar que el contenedor esté corriendo
docker-compose ps

# Ver logs
docker-compose logs fondos-service

# Reiniciar
docker-compose restart fondos-service
```

### "ERROR: 'Connection to localhost:3306' refused"
```bash
# MySQL tarda en iniciar (esperar 30 segundos)
docker-compose logs mysql

# O reiniciar toda la stack
docker-compose restart
```

### "ERROR: Database already exists"
```bash
# Eliminar volúmenes (cuidado: borra datos)
docker-compose down -v
docker-compose up -d
```

### "¿Por qué mi request tarda 30 segundos?"
Probable causa: RestTemplate está esperando respuesta de servicio lento.
```bash
# Aumentar timeout en docker-compose.yml
# spring.restclient.connectTimeout=15000
# spring.restclient.readTimeout=15000
```

---

## 🚀 Next Steps

### Después de `docker-compose up -d` exitoso:

1. ✅ Probar endpoints con curl (ver Testing E2E arriba)
2. ✅ O importar Postman collection: `BancoCloud-Postman-Collection.json`
3. ✅ Ver logs para debug: `docker-compose logs -f`
4. ✅ Hacer cambios en código
5. ✅ Rebuild: `docker-compose build && docker-compose up -d`

---

## 📝 Estructura del `docker-compose.yml`

```yaml
version: '3.8'

services:
  mysql:
    image: mysql:latest
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
    healthcheck:
      test: ["CMD", "mysqladmin", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  fondos-service:
    build:
      context: ./fondos-service
      dockerfile: Dockerfile
    ports:
      - "8081:8081"
    depends_on:
      mysql:
        condition: service_healthy

  # ... resto de servicios
```

---

**¿Problemas?** Revisa [QUICKSTART.md](QUICKSTART.md) sección Troubleshooting
```

## MySQL Access

```bash
# Conectar a MySQL desde fuera del contenedor
mysql -h localhost -P 3306 -u root -p

# Password: bancocloud
# Bases de datos: usuarios_db, fondos_db, inversiones_db
```

## Rebuilds Incrementales

Si modificas código:

```bash
# Rebuild solo un servicio
docker-compose build usuarios-service
docker-compose up -d usuarios-service

# O rebuild todos
docker-compose build --no-cache
docker-compose up -d
```

## Troubleshooting

### MySQL no inicia

```bash
docker-compose logs mysql
# Verificar que el puerto 3306 no esté en uso
lsof -i :3306
```

### Servicio no conecta a MySQL

```bash
# Verificar que MySQL es healthy
docker-compose ps mysql

# Si no es healthy, revisar logs
docker-compose logs mysql
```

### Borrar todo y empezar de cero

```bash
docker-compose down -v
docker system prune -a
docker-compose build --no-cache
docker-compose up -d
```

## Deployment a AWS

Ver: [AWS_DEPLOYMENT.md](../AWS_DEPLOYMENT.md)

---

**Estado:** ✅ Completamente funcional - Listo para AWS
