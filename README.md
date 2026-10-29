# Banco Cloud — Backend

Backend de Banco Cloud, una aplicación de fondos mutuos. Son seis aplicaciones Java 17 + Spring Boot 4.1: un BFF, tres microservicios de dominio con MySQL en AWS RDS y dos servicios de mensajería sobre RabbitMQ. La autenticación usa AWS Cognito.

Frontend: [Cloud-Native-Front-end](https://github.com/Nathaaam/Cloud-Native-Front-end)

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
| `RABBITMQ_HOST`, `RABBITMQ_PORT`, `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` | Broker RabbitMQ |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos por el BFF |
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

```text
Navegador ─ CloudFront ─ S3 (frontend)
    │
    └─ API Gateway (HTTP API) ─ VPC Link ─ ALB interno :80 ─ EC2 :8090 (BFF)
                                                             ├─ RabbitMQ y 5 servicios (Docker Compose)
                                                             └─ RDS MySQL (privado)
```

En la EC2 corre solo el backend. El frontend se publica en S3 con `scripts/deploy-s3.sh` del repositorio del frontend.

Requisitos de la infraestructura:

- **EC2:** salida a internet (NAT) para descargar el código, las imágenes y las dependencias de Maven. Se recomienda una `t3.medium` o mayor. El script agrega 2 GB de swap si hay menos de 6 GB de RAM.
- **Security groups:** el de la EC2 abre el 8090 solo al security group del ALB. El de RDS abre el 3306 solo al de la EC2.
- **ALB:** el target group apunta a la EC2 en el puerto 8090, con health check en `/api/v1/health`.
- **API Gateway:** ruta `ANY /{proxy+}` hacia el ALB y stage `$default`. No debe tener CORS configurado, porque lo maneja el BFF.

Pasos en la EC2, por ejemplo desde Session Manager:

```bash
sudo dnf install -y git                  # Ubuntu: sudo apt-get install -y git
sudo mkdir -p /opt/bancocloud && sudo chown "$USER" /opt/bancocloud
git clone https://github.com/itzAragorn/Cloud-Native-Backend.git /opt/bancocloud/backend
cd /opt/bancocloud/backend

./deploy/ec2-deploy.sh preparar          # Docker, Compose, Buildx y swap; luego cerrar sesión y volver a entrar
cp .env.example .env && nano .env        # valores reales; RABBITMQ_HOST=rabbitmq
./deploy/ec2-deploy.sh rds-init          # una vez: bases y usuario de la app (pide el usuario maestro de RDS)
./deploy/ec2-deploy.sh desplegar         # git pull + build + espera a que todo esté healthy
```

Para ver el estado usa `./deploy/ec2-deploy.sh estado` y, para revisar un servicio, `./deploy/ec2-deploy.sh logs <servicio>`. Antes de levantar nada, `desplegar` valida el `.env`: rechaza valores de ejemplo, la base en `localhost`, el usuario `guest` de RabbitMQ y la falta de una URL `https` en CORS.

**EC2 sin salida a internet:** en lugar de compilar en la EC2, se arma un paquete con las imágenes y se lleva por S3, a través del endpoint Gateway de S3. Así no hace falta NAT.

```bash
# En una máquina con Docker (la EC2 t3.* es linux/amd64)
./deploy/build-bundle.sh linux/amd64      # dist/bancocloud-backend-<commit>.tar (~470 MB) + .sha256

# En la EC2, después de copiar el paquete desde S3
tar -xf bancocloud-backend-<commit>.tar && cd bancocloud-backend
cp <ruta>/.env .env
./deploy/ec2-deploy.sh desplegar          # detecta imagenes.tar.gz: carga las imágenes y levanta sin internet
```

`docker-compose.aws.yml` acota la memoria de la JVM y reemplaza los healthchecks con `curl` por uno con bash, porque las imágenes del paquete solo traen el jar. Medido en Linux, los 7 contenedores ocupan entre 1,1 y 1,5 GB, así que la EC2 necesita al menos 2 GB de RAM.

Para ver la consola de RabbitMQ sin abrir puertos, haz port forwarding de Session Manager al 15672 de la EC2:

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
