# Banco Cloud — Backend

Backend de Banco Cloud, una aplicación de fondos mutuos. Son seis aplicaciones Java 17 + Spring Boot 4.1: un BFF, tres microservicios de dominio con MySQL en AWS RDS y dos servicios de mensajería sobre RabbitMQ. La autenticación usa AWS Cognito.

Frontend: [Cloud-Native-Front-end](https://github.com/Nathaaam/Cloud-Native-Front-end) · Aplicación desplegada: https://3cwdl02ue4.execute-api.us-east-1.amazonaws.com (ver [Despliegue en AWS](#despliegue-en-aws))

## Arquitectura

```text
Frontend (React) ──JWT Cognito──▶ bff-service :8090  /api/v1
                                   ├─▶ usuarios-service    :8083 ── usuarios_db
                                   ├─▶ fondos-service      :8081 ── fondos_db
                                   ├─▶ inversiones-service :8082 ── inversiones_db
                                   │       └─▶ fondos-service (valor cuota)
                                   ├─▶ rabbit-admin-service :8085 ──▶ RabbitMQ
                                   └─▶ notificaciones-service :8084 (comprobantes)

usuarios / fondos / inversiones ──eventos──▶ RabbitMQ ──▶ notificaciones-service :8084
```

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| `bff-service` | 8090 | Única entrada del frontend. Valida el Access Token de Cognito (firma, `iss`, expiración, `token_use`, `client_id`), asigna el rol local y reenvía a los servicios. |
| `usuarios-service` | 8083 | Perfiles asociados a Cognito (`cognito_sub`), rol `ADMIN`/`CLIENTE`, autorización y bloqueo. |
| `fondos-service` | 8081 | Fondos mutuos y valor cuota. |
| `inversiones-service` | 8082 | Inversiones, portafolio y rendimiento. Cuotas con 4 decimales `HALF_UP`. |
| `notificaciones-service` | 8084 | Consume los eventos: comprobantes de inversión y notificaciones. Expone la consulta de comprobantes. |
| `rabbit-admin-service` | 8085 | API REST para administrar colas, exchanges y bindings. |

## Mensajería (RabbitMQ)

Los servicios de dominio publican un evento **después del commit** de cada operación. Si RabbitMQ no está disponible, la operación igual responde bien y el error queda en el log.

| Caso de uso | Exchange (topic) · routing key | Cola | Acción del consumidor |
|---|---|---|---|
| Inversión creada | `bancocloud.inversiones` · `inversion.creada` | `inversiones.comprobante.q` | Genera el comprobante `BC-<año>-<correlativo>` en JSON |
| Usuario registrado | `bancocloud.usuarios` · `usuario.registrado` | `usuarios.registro.q` | Aviso al administrador |
| Cambio de estado | `bancocloud.usuarios` · `usuario.estado.*` | `usuarios.estado.q` | Notificación al cliente (autorizado, desautorizado, bloqueado, desbloqueado) |
| Valor cuota actualizado | `bancocloud.fondos` · `fondo.valorcuota.actualizado` | `fondos.valorcuota.q` | Registro del cambio y aviso a inversionistas |

Cada cola declara `x-dead-letter-exchange = bancocloud.dlx` y tiene su DLQ `<cola>.dlq`. Los nombres están centralizados en `application.yml` (`app.rabbitmq`) y la topología se declara en `messaging/config/RabbitMQConfig` de cada servicio.

**Confirmación de mensajes en notificaciones-service** (`acknowledge-mode: manual`, política en `MensajeAckHandler`):

- Éxito → `basicAck`.
- Error no recuperable (JSON inválido, datos faltantes) → `basicNack(requeue=false)` → DLQ.
- Error recuperable → se republica con `x-retry-count` hasta 3 veces; agotados los intentos → DLQ.
- `DlqListener` registra cada mensaje muerto con su header `x-death` (cola de origen, motivo, cantidad).

La generación de comprobantes es idempotente: si un evento llega dos veces, se devuelve el comprobante ya emitido. Se consultan en `GET /api/v1/comprobantes?usuarioId=` (un CLIENTE solo ve los suyos).

## API de administración de RabbitMQ

Expuesta por el BFF en `/api/v1/admin/rabbit` (solo rol `ADMIN`). Dentro de la red Docker, rabbit-admin-service atiende las mismas rutas sin el prefijo.

| Método | Ruta | Cuerpo | Respuestas |
|---|---|---|---|
| POST | `/queues` | `name`, `durable`, `deadLetterExchange`, `deadLetterRoutingKey`, `ttlMs` | 201 · 400 · 409 |
| GET | `/queues/{name}` | — | 200 (mensajes y consumidores) · 404 |
| DELETE | `/queues/{name}` | — | 204 · 404 |
| POST | `/exchanges` | `name`, `type` (`DIRECT`, `TOPIC`, `FANOUT`, `HEADERS`), `durable` | 201 · 400 · 409 |
| DELETE | `/exchanges/{name}` | — | 204 · 404 |
| POST | `/bindings` | `queue`, `exchange`, `routingKey` | 201 · 400 · 404 |
| DELETE | `/bindings` | `queue`, `exchange`, `routingKey` | 204 · 404 |

Los nombres admiten letras, números, `.`, `_` y `-` (máximo 100) y no pueden empezar con `amq.`. Un error de validación devuelve 400 con `errores: [{campo, error}]`.

## Configuración

Copia `.env.example` a `.env` y completa los valores. `.env` está en `.gitignore` y nunca se sube al repositorio.

| Variables | Uso |
|---|---|
| `USUARIOS_DB_URL`, `FONDOS_DB_URL`, `INVERSIONES_DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a RDS |
| `COGNITO_ISSUER`, `COGNITO_CLIENT_ID` | User Pool y App Client que valida el BFF |
| `COGNITO_JWKS` | Opcional: claves públicas del User Pool (JSON de `<issuer>/.well-known/jwks.json`) para validar tokens sin acceso a internet |
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` | Broker RabbitMQ |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos por el BFF (en AWS, la URL de API Gateway) |
| `VITE_*` | Build del frontend en Docker Compose |

## Ejecución

**Docker Compose** (todo el stack, incluido RabbitMQ y el frontend):

```bash
cp .env.example .env    # completar valores
docker compose up -d --build
```

La consola de RabbitMQ queda en http://localhost:15672 (solo desde la misma máquina). El script `docker-compose.sh` ofrece atajos: `up`, `down`, `logs`, `ps`, `build`, `rebuild`, `shell` y `health`.

**Un servicio fuera de Docker** (con las variables de `.env` exportadas y `RABBITMQ_HOST=localhost`):

```bash
docker run -d --name rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
cd fondos-service && ./mvnw spring-boot:run
```

## Despliegue en AWS

La aplicación está desplegada en la cuenta del Learner Lab (región `us-east-1`):
**https://3cwdl02ue4.execute-api.us-east-1.amazonaws.com**

```text
Navegador ── HTTPS ── API Gateway (HTTP API)
                        ├─ GET /, /favicon.svg, /assets/*  ──▶ S3 (archivos del frontend)
                        └─ ANY /{proxy+}  ── VPC Link ── ALB interno :80 ── EC2 privada :8090
                                                                              ├─ bff-service
                                                                              ├─ 5 microservicios
                                                                              ├─ RabbitMQ (colas + DLQ)
                                                                              └─▶ RDS MySQL (privado)
Autenticación: Amazon Cognito (User Pool + App Client público, flujo SRP)
```

| Componente | Configuración |
|---|---|
| EC2 `bc-app-server` | `t3.medium` (4 GB), Amazon Linux 2023, subred privada **sin NAT**. Acceso por Session Manager. Docker Compose con RabbitMQ y los 6 servicios. |
| RDS `bancocloud-mysql` | MySQL 8.4 privado. Bases `usuarios_db`, `fondos_db` e `inversiones_db`; usuario de aplicación `bancocloud_app` con permisos solo sobre ellas. |
| ALB interno | Listener HTTP 80 → target group en el puerto 8090, health check `/api/v1/health`. |
| API Gateway | HTTP API, stage `$default`. `ANY /{proxy+}` va por VPC Link al ALB. Las rutas del frontend son integraciones HTTP a S3. **Sin CORS en API Gateway**: lo maneja el BFF. |
| S3 (frontend) | Lectura pública **solo** de `index.html`, `favicon.svg` y `assets/*` (política del bucket). El resto del bucket es privado. |
| S3 (despliegue) | Bucket privado para los paquetes del backend. La EC2 lo lee por el endpoint Gateway de S3. |
| Security groups | ALB: HTTP 80 desde la VPC (`10.0.0.0/16`). EC2: 8090 solo desde el ALB. RDS: 3306 solo desde la EC2. |

### Decisiones por las restricciones del Learner Lab

- **Sin NAT.** La EC2 no tiene salida a internet, así que no puede compilar ni descargar imágenes. El backend se lleva como un paquete de imágenes por S3 (ver abajo).
- **Validación de tokens sin internet.** El BFF necesita las claves públicas de Cognito (JWKS). Con `COGNITO_JWKS` en el `.env` las toma de ahí, en vez de descargarlas. PrivateLink no sirve: Cognito no lo admite en User Pools con dominio.
- **Sin CloudFront.** El Learner Lab no permite crear distribuciones ni Origin Access Control. API Gateway sirve también el frontend con HTTPS. Así frontend y API comparten la URL.

### Paquete del backend (EC2 sin internet)

```bash
# En una máquina con Docker. La t3.medium es linux/amd64.
./deploy/build-bundle.sh                       # todo: dist/bancocloud-backend-<commit>.tar (~470 MB)
SOLO="bff-service" ./deploy/build-bundle.sh    # actualización de uno o más servicios (~126 MB por servicio)
```

El script compila y prueba los servicios y construye las imágenes con `deploy/Dockerfile.runtime`, que solo copia el jar, así que no emula. Después las exporta junto con el compose y los scripts. El paquete no incluye secretos.

Instalación o actualización en la EC2 (Session Manager), después de subir el paquete al bucket de despliegue:

```bash
cd ~/bancocloud
aws s3 cp s3://<bucket-despliegue>/<paquete>.tar .
sha256sum -c <<< "<sha256 del .sha256>  <paquete>.tar"
tar -xf <paquete>.tar && rm <paquete>.tar    # se descomprime en bancocloud-backend/ y conserva el .env
cd bancocloud-backend
./deploy/ec2-deploy.sh desplegar
```

`desplegar` hace estos pasos:

1. Valida el `.env`: rechaza valores de ejemplo, la base en `localhost`, el usuario `guest` de RabbitMQ y la falta de una URL `https` en CORS.
2. Comprueba el disco.
3. Carga las imágenes nuevas.
4. Levanta los servicios con `docker-compose.aws.yml` y espera a que estén `healthy`. Solo se reinician los contenedores que cambiaron.

`docker-compose.aws.yml` acota la memoria de la JVM y usa healthchecks con bash, porque las imágenes solo traen el jar. Medido en Linux, los 7 contenedores ocupan entre 1,1 y 1,5 GB.

Otros comandos: `./deploy/ec2-deploy.sh estado` y `./deploy/ec2-deploy.sh logs <servicio>`. Si la EC2 tuviera internet, `desplegar` sin paquete compila allí mismo y `preparar` instala Docker.

### Al iniciar el Learner Lab

Al cerrar el lab se detienen la EC2 y RDS. API Gateway, ALB, S3, Cognito y los datos se conservan, y la URL no cambia.

1. **RDS** `bancocloud-mysql` en estado *Disponible*. Si está detenida: *Acciones → Iniciar*. Hazlo **antes** que la EC2, porque los servicios necesitan la base para arrancar.
2. **EC2** `bc-app-server` *En ejecución*. Docker levanta los contenedores solo (`restart: unless-stopped`; una vez, `sudo systemctl enable docker`).
3. En Session Manager:
   ```bash
   cd ~/bancocloud/bancocloud-backend && ./deploy/ec2-deploy.sh estado
   ```
   Si algún servicio no está `healthy`, normalmente porque arrancó antes que RDS, ejecuta `./deploy/ec2-deploy.sh desplegar`. No recarga imágenes y tarda 1 a 2 minutos.
4. Abre la URL de la aplicación. Si responde `Service Unavailable`, el ALB todavía no ve la EC2 sana: espera un minuto.

### Primer administrador

Todo usuario se registra desde la app como `CLIENTE`. Al primer administrador se le asigna el rol a mano, desde la EC2:

```bash
MYSQL_PWD="$(grep '^DB_PASSWORD=' .env | cut -d= -f2-)" mysql -h <endpoint-rds> -u bancocloud_app usuarios_db \
  -e "UPDATE usuarios SET rol='ADMIN', acceso_autorizado=1, estado='ACTIVO' WHERE email='<correo>';"
```

### Consola de RabbitMQ

Queda publicada solo en `127.0.0.1:15672` de la EC2. Para verla sin abrir puertos, usa el port forwarding de Session Manager:

```bash
aws ssm start-session --target <instance-id> --document-name AWS-StartPortForwardingSession \
  --parameters portNumber=15672,localPortNumber=15672
```

## Base de datos

Scripts en `scripts-sql/`:

1. `00-init-databases.sql`: crea `usuarios_db`, `fondos_db` e `inversiones_db`. Las tablas las crea Hibernate al arrancar cada servicio. En RDS conviene usar `./deploy/ec2-deploy.sh rds-init`, que además crea el usuario de la app con permisos solo sobre esas bases.
2. `01-promover-admin.sql`: convierte en `ADMIN` a un usuario ya registrado desde el frontend.
3. `02-populate-fondos.sql`: fondos de ejemplo.

## Pruebas

```bash
cd <servicio> && ./mvnw clean package
```

Los tests no requieren AWS ni RabbitMQ: los de contexto usan H2 en memoria y los de mensajería usan `RabbitTemplate` y `Channel` simulados con Mockito.

| Servicio | Tests | Qué cubren |
|---|---|---|
| rabbit-admin-service | 47 | `RabbitAdminService`, validación de los DTO y controladores |
| notificaciones-service | 26 | ACK, NACK a DLQ y reintentos con `Channel` simulado; `DlqListener`; comprobantes idempotentes |
| bff-service | 21 | Validación de tokens (firma, issuer, expiración, `token_use`, `client_id`, JWKS fijo) y permisos por rol en los proxys |
| usuarios-service | 8 | Publicación de eventos de registro y cambio de estado |
| fondos-service | 4 | Publicación del evento de valor cuota |
| inversiones-service | 4 | Publicación del evento de inversión |
| **Total** | **110** | |

`deploy/e2e.sh <URL_API>` prueba el sistema en ejecución, local o en AWS: seguridad (401/403), las 8 colas con su consumidor, creación y borrado por la API de administración, validaciones (400) y un mensaje inválido hacia la DLQ. Pide los tokens con entrada oculta, y si se omiten, corre solo las pruebas sin sesión.
