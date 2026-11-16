# Banco Cloud — Backend

Backend de Banco Cloud, una aplicación de fondos mutuos. Son ocho aplicaciones Java 17 + Spring Boot 4.1:

- un BFF;
- tres microservicios de dominio con MySQL en AWS RDS;
- dos servicios de mensajería sobre RabbitMQ;
- dos servicios de streaming sobre Kafka (EP5): analítica y administración de Kafka.

La autenticación usa AWS Cognito.

Frontend: [Cloud-Native-Front-end](https://github.com/Nathaaam/Cloud-Native-Front-end) · Aplicación desplegada: https://3cwdl02ue4.execute-api.us-east-1.amazonaws.com (ver [Despliegue en AWS](#despliegue-en-aws))

## Arquitectura

```text
Frontend (React) ──JWT Cognito──▶ bff-service :8090  /api/v1
                                   ├─▶ usuarios-service    :8083 ── usuarios_db
                                   ├─▶ fondos-service      :8081 ── fondos_db
                                   ├─▶ inversiones-service :8082 ── inversiones_db
                                   │       └─▶ fondos-service (valor cuota)
                                   ├─▶ rabbit-admin-service :8085 ──▶ RabbitMQ (3 nodos)
                                   ├─▶ notificaciones-service :8084 (comprobantes)
                                   ├─▶ analitica-service   :8086 ── analitica_db
                                   └─▶ kafka-admin-service :8087 ──▶ Kafka (3 brokers)

usuarios / fondos / inversiones ──eventos──▶ RabbitMQ ──▶ notificaciones-service  (tareas: comprobantes, avisos)
fondos / inversiones ──────────eventos──▶ Kafka ────▶ analitica-service        (streaming y métricas)
                                                  └─▶ notificaciones-service  (auditoría)
```

| Servicio | Puerto | Responsabilidad |
|---|---|---|
| `bff-service` | 8090 | Única entrada del frontend. Valida el Access Token de Cognito (firma, `iss`, expiración, `token_use`, `client_id`), asigna el rol local y reenvía a los servicios. |
| `usuarios-service` | 8083 | Perfiles asociados a Cognito (`cognito_sub`), rol `ADMIN`/`CLIENTE`, autorización y bloqueo. |
| `fondos-service` | 8081 | Fondos mutuos y valor cuota. |
| `inversiones-service` | 8082 | Inversiones, portafolio y rendimiento. Cuotas con 4 decimales `HALF_UP`. |
| `notificaciones-service` | 8084 | Consume los eventos: comprobantes de inversión y notificaciones. Expone la consulta de comprobantes. |
| `rabbit-admin-service` | 8085 | API REST para administrar colas, exchanges y bindings. |
| `analitica-service` | 8086 | Consume los tópicos de Kafka y arma métricas: monto y cantidad por fondo y día, ranking de fondos y serie histórica del valor cuota. |
| `kafka-admin-service` | 8087 | API REST para crear, modificar y eliminar tópicos y sus configuraciones, y para inspeccionar y administrar consumer groups. |

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

## Streaming con Kafka (EP5)

Kafka funciona junto a RabbitMQ y no lo reemplaza. RabbitMQ sigue a cargo de las tareas asíncronas (comprobantes, avisos). Kafka lleva un flujo de eventos para analítica y auditoría, con un historial que se puede volver a leer.

Los productores reutilizan los eventos de Spring que ya existían. Un segundo `@TransactionalEventListener(AFTER_COMMIT)`, propio de Kafka (`messaging.kafka.producer`), publica el DTO después del commit. Si Kafka falla, ni RabbitMQ ni la operación se ven afectados: el error queda en el log. Los dos canales usan el mismo `eventId`.

### Tópicos

| | `bancocloud.inversiones.eventos` | `bancocloud.fondos.valorcuota` |
|---|---|---|
| Productor | inversiones-service | fondos-service |
| DTO | `InversionRegistradaKafkaEvent` | `ValorCuotaKafkaEvent` |
| Key | `usuarioId`: orden por usuario | `fondoId`: cada fondo en una partición |
| Particiones / réplicas / `min.insync.replicas` | 3 / 3 / 2 | 3 / 3 / 2 |
| `cleanup.policy` | `delete`: historial de transacciones | `compact,delete`: se conserva el último valor de cada fondo |
| `retention.ms` | 7 días | 30 días |
| Consumer groups | `analitica-inversiones` (analitica-service), `auditoria-inversiones` (notificaciones-service) | `analitica-fondos` (analitica-service) |

Cada tópico tiene su `<tópico>.DLT`: 3 particiones, `delete` y 14 días. Los declara con `TopicBuilder` el servicio dueño (`messaging/kafka/config/KafkaTopicConfig`), y los valores están en `app.kafka` de su `application.yml`, comentados con la justificación de cada uno.

**Cliente.** Los productores usan `acks=all`, idempotencia, `lz4`, `linger.ms=5` y `max.block.ms=5000`, para que un Kafka caído no retenga la respuesta HTTP. Los consumidores usan `earliest`, sin auto commit, `ack-mode record`, `concurrency 3`, `read_committed` y `CooperativeStickyAssignor`. El serializer es `JacksonJsonSerializer` (Spring Kafka 4, Jackson 3), sin headers de tipo Java.

**Consumer groups.**

- *Dividir la carga:* `analitica-inversiones` corre con 3 hebras y el tópico tiene 3 particiones, así que a cada hebra le toca una. Si se levanta otra instancia, Kafka reparte las particiones entre las dos.
- *Compartir el evento:* `analitica-inversiones` y `auditoria-inversiones` son grupos distintos, así que cada uno recibe todas las inversiones para su propia responsabilidad.

**Errores.**

- `ErrorHandlingDeserializer`: un JSON roto no detiene al consumidor.
- `DefaultErrorHandler` reintenta con backoff exponencial (1 s, 2 s, 4 s) y después publica en `<tópico>.DLT`, en la misma partición, con los headers `kafka_dlt-*`. No se reintentan `DeserializationException`, `EventoInvalidoException` (payload que no pasa la validación del DTO), `ValidationException` ni `MethodArgumentNotValidException`.
- Cada intento y cada envío al DLT queda en el log con tópico, partición, offset, key, `eventId` y motivo.
- `DltListener` (grupo `analitica-dlt`) registra cada mensaje muerto con su origen y motivo, y nunca falla.
- Analítica es idempotente: guarda cada `eventId` procesado (tabla `evento_procesado`) en la misma transacción que la métrica.

### API de analítica

Expuesta por el BFF en `/api/v1/analitica/fondos`.

| Método | Ruta | Rol | Respuesta |
|---|---|---|---|
| GET | `/resumen` | ADMIN | Ranking de fondos por monto invertido, con cantidad de inversiones y último valor cuota |
| GET | `/{id}/inversiones/diario` | ADMIN | Monto y cantidad por día |
| GET | `/{id}/valor-cuota/historial` | CLIENTE o ADMIN | Serie del valor cuota con su variación porcentual |

### API de administración de Kafka

Expuesta por el BFF en `/api/v1/admin/kafka` (solo rol `ADMIN`). Dentro de la red Docker, kafka-admin-service atiende las mismas rutas sin el prefijo.

| Método | Ruta | Cuerpo | Respuestas |
|---|---|---|---|
| GET | `/topics` | — | 200 (sin tópicos internos `__*`) |
| GET | `/topics/{name}` | — | 200 (particiones con líder, réplicas e ISR; configuraciones) · 404 |
| POST | `/topics` | `name`, `partitions`, `replicationFactor`, `configs` | 201 · 400 · 409 |
| PUT | `/topics/{name}/configs` | `configs` (un valor `null` vuelve al valor por defecto) | 200 · 400 · 404 |
| POST | `/topics/{name}/partitions` | `total` (solo aumentar) | 200 · 400 · 404 |
| DELETE | `/topics/{name}` | — | 204 · 404 |
| GET | `/consumer-groups` | — | 200 (grupo y estado) |
| GET | `/consumer-groups/{id}` | — | 200 (miembros, particiones asignadas, offsets y lag) · 404 |
| POST | `/consumer-groups/{id}/reset-offsets` | `topic`, `estrategia` (`EARLIEST`, `LATEST`, `OFFSET`), `partition`, `offset` | 200 · 400 · 409 si el grupo tiene consumidores activos |
| DELETE | `/consumer-groups/{id}` | — | 204 · 404 · 409 |

Reglas de validación:

- Nombre de tópico: hasta 249 caracteres; solo letras, números, `.`, `_` y `-`; no puede empezar con `__`.
- `replicationFactor`: no puede ser mayor que la cantidad de brokers del clúster.
- `configs`: lista blanca `retention.ms`, `cleanup.policy`, `min.insync.replicas`, `segment.bytes` y `max.message.bytes`, cada una con su validación de valor.
- Si Kafka no responde: 503.

**Por script.** `kafka/scripts/kafka-admin.sh` hace lo mismo con las herramientas del broker (`kafka-topics`, `kafka-configs`, `kafka-consumer-groups`). Comandos: `init`, `topics`, `describe`, `create`, `alter`, `reset-config`, `partitions`, `delete`, `groups`, `group <id>` (lag), `reset-offsets` (con *dry run*), `delete-group` y `brokers`.

**Kafka-UI.** En http://localhost:8089 (solo desde la misma máquina) muestra brokers, tópicos, mensajes con su partición y offset, y el lag de cada grupo.

## Configuración

Copia `.env.example` a `.env` y completa los valores. `.env` está en `.gitignore` y nunca se sube al repositorio.

| Variables | Uso |
|---|---|
| `USUARIOS_DB_URL`, `FONDOS_DB_URL`, `INVERSIONES_DB_URL`, `ANALITICA_DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a RDS |
| `COGNITO_ISSUER`, `COGNITO_CLIENT_ID` | User Pool y App Client que valida el BFF |
| `COGNITO_JWKS` | Opcional: claves públicas del User Pool (JSON de `<issuer>/.well-known/jwks.json`) para validar tokens sin acceso a internet |
| `RABBITMQ_USERNAME`, `RABBITMQ_PASSWORD`, `RABBITMQ_VHOST` | Clúster RabbitMQ. En Docker los servicios se conectan a los 3 nodos; `RABBITMQ_HOST`/`PORT` solo sirven para correr un jar fuera de Docker |
| `RABBITMQ_ERLANG_COOKIE` | Secreto compartido de los 3 nodos (`openssl rand -hex 24`) |
| `KAFKA_BOOTSTRAP_SERVERS` | Brokers de Kafka. En Docker: `kafka-1:29092,kafka-2:29092,kafka-3:29092`. Fuera de Docker, por defecto `localhost:9092,9093,9094` |
| `APP_CORS_ALLOWED_ORIGINS` | Orígenes permitidos por el BFF (en AWS, la URL de API Gateway) |
| `VITE_*` | Build del frontend en Docker Compose |

## Ejecución

**Docker Compose.** Hay cuatro archivos, que se levantan juntos como un solo proyecto:

| Archivo | Qué levanta |
|---|---|
| `docker-compose.rabbitmq.yml` | Clúster RabbitMQ de 3 nodos (`rabbitmq/rabbitmq.conf`: `classic_config`, `pause_minority`) |
| `docker-compose.kafka.yml` | Zookeeper y 3 brokers Kafka (Confluent 7.9, Kafka 3.9). Listener interno `kafka-N:29092` y externo `localhost:909N` |
| `docker-compose.kafka-ui.yml` | Kafka-UI (`kafbat/kafka-ui`) en `127.0.0.1:8089` |
| `docker-compose.yml` | Los 8 servicios y el frontend |

```bash
cp .env.example .env    # completar valores
docker compose -f docker-compose.rabbitmq.yml -f docker-compose.kafka.yml \
  -f docker-compose.kafka-ui.yml -f docker-compose.yml up -d --build
```

Cada broker también se puede levantar por separado, por ejemplo para probar un servicio fuera de Docker:

```bash
docker compose -f docker-compose.rabbitmq.yml up -d                                # solo el clúster RabbitMQ
docker compose -f docker-compose.kafka.yml up -d                                   # solo Kafka
docker compose -f docker-compose.kafka.yml -f docker-compose.kafka-ui.yml up -d    # Kafka + Kafka-UI
```

`docker-compose.yml` necesita los brokers, así que siempre va con los otros archivos.

Para comprobar el estado:

- Clúster RabbitMQ: `docker exec bancocloud-rabbitmq-1 rabbitmqctl cluster_status` muestra los 3 nodos en *Running Nodes*.
- Kafka: `./kafka/scripts/kafka-admin.sh brokers`.
- Consolas: RabbitMQ en http://localhost:15672 y Kafka-UI en http://localhost:8089, las dos solo desde la misma máquina.

El script `docker-compose.sh` ofrece atajos: `up`, `down`, `logs`, `ps`, `build`, `rebuild`, `shell` y `health`.

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
                                                                              ├─ 7 microservicios
                                                                              ├─ RabbitMQ (3 nodos, colas + DLQ)
                                                                              ├─ Kafka (Zookeeper + 3 brokers) + Kafka-UI
                                                                              └─▶ RDS MySQL (privado)
Autenticación: Amazon Cognito (User Pool + App Client público, flujo SRP)
```

| Componente | Configuración |
|---|---|
| EC2 `bc-app-server` | `t3.large` (8 GB) desde la EP5, Amazon Linux 2023, subred privada **sin NAT**. Acceso por Session Manager. Docker Compose con el clúster RabbitMQ, Kafka (Zookeeper + 3 brokers), Kafka-UI y los 8 servicios. |
| RDS `bancocloud-mysql` | MySQL 8.4 privado. Bases `usuarios_db`, `fondos_db`, `inversiones_db` y `analitica_db`; usuario de aplicación `bancocloud_app` con permisos solo sobre ellas. |
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
./deploy/build-bundle.sh                       # todo, con brokers: dist/bancocloud-backend-<commit>.tar (~1,5 GB)
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

1. Valida el `.env`. Rechaza:
   - valores de ejemplo;
   - una base en `localhost`;
   - el usuario `guest` de RabbitMQ;
   - una cookie de Erlang de menos de 20 caracteres;
   - Kafka en `localhost`;
   - la falta de una URL `https` en CORS.
2. Comprueba el disco.
3. Carga las imágenes nuevas.
4. Levanta los brokers y los servicios en un solo proyecto (los 4 compose más `docker-compose.aws.yml`) y espera a que estén `healthy`. Solo se reinician los contenedores que cambiaron.
5. Muestra los nodos del clúster RabbitMQ y los brokers de Kafka.

**Al pasar de la EP3 a la EP5:**

- `--remove-orphans` borra el contenedor `bancocloud-rabbitmq` de un solo nodo; el clúster arranca con volúmenes nuevos y los servicios vuelven a declarar sus colas al arrancar. Conviene desplegar con las colas vacías.
- El volumen `comprobantes-data` se conserva, porque el nombre del proyecto Compose no cambia.

`docker-compose.aws.yml` acota la memoria de la JVM y usa healthchecks con bash, porque las imágenes solo traen el jar. En una prueba local, el stack completo (sin MySQL, que en AWS es RDS) usó unos 4,2 GB: cada broker Kafka unos 380 MB, cada nodo RabbitMQ unos 150 MB, Kafka-UI 300 MB y cada servicio Java entre 180 y 300 MB. Por eso la EC2 necesita 8 GB.

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

### Consolas de RabbitMQ y Kafka-UI

Quedan publicadas solo en `127.0.0.1` de la EC2: RabbitMQ en el puerto 15672 y Kafka-UI en el 8089. Para verlas sin abrir puertos, usa el port forwarding de Session Manager, una sesión por consola:

```bash
aws ssm start-session --target <instance-id> --document-name AWS-StartPortForwardingSession \
  --parameters portNumber=15672,localPortNumber=15672
aws ssm start-session --target <instance-id> --document-name AWS-StartPortForwardingSession \
  --parameters portNumber=8089,localPortNumber=8089
```

## Base de datos

Scripts en `scripts-sql/`:

1. `00-init-databases.sql`: crea `usuarios_db`, `fondos_db`, `inversiones_db` y `analitica_db`. Las tablas las crea Hibernate al arrancar cada servicio. En RDS conviene usar `./deploy/ec2-deploy.sh rds-init`, que además crea el usuario de la app con permisos solo sobre esas bases.
2. `01-promover-admin.sql`: convierte en `ADMIN` a un usuario ya registrado desde el frontend.
3. `02-populate-fondos.sql`: fondos de ejemplo.

## Pruebas

```bash
cd <servicio> && ./mvnw clean package
```

Los tests no requieren AWS, RabbitMQ ni un clúster Kafka:

- Los tests de contexto usan H2 en memoria.
- Los de RabbitMQ usan `RabbitTemplate` y `Channel` simulados con Mockito.
- Los de Kafka usan `KafkaTemplate` o `AdminClient` simulados, o un broker embebido (`@EmbeddedKafka`).

| Servicio | Tests | Qué cubren |
|---|---|---|
| rabbit-admin-service | 47 | `RabbitAdminService`, validación de los DTO y controladores |
| kafka-admin-service | 33 | `KafkaAdminService` con `AdminClient` simulado, validación de DTO (lista blanca de configs), códigos HTTP, y la API completa contra un broker embebido (tópicos, configs, particiones, lag y reset de offsets) |
| notificaciones-service | 31 | ACK, NACK a DLQ y reintentos con `Channel` simulado; `DlqListener`; comprobantes idempotentes; auditoría Kafka y JSON roto al `.DLT` con broker embebido |
| bff-service | 29 | Validación de tokens (firma, issuer, expiración, `token_use`, `client_id`, JWKS fijo) y permisos por rol en los proxys (RabbitMQ, Kafka, analítica) |
| analitica-service | 21 | Servicios de analítica (idempotencia, validación), `DltService`, controlador, y flujo real con broker embebido: métricas, duplicado descartado, JSON roto y payload inválido al `.DLT` |
| fondos-service | 13 | Publicación a RabbitMQ y Kafka (key, headers, validación, errores), configuración del productor y de los tópicos |
| inversiones-service | 13 | Lo mismo para el tópico de inversiones |
| usuarios-service | 8 | Publicación de eventos de registro y cambio de estado |
| **Total** | **195** | |

`deploy/e2e.sh <URL_API>` prueba el sistema en ejecución, local o en AWS: seguridad (401/403), las 8 colas con su consumidor, creación y borrado por la API de administración, validaciones (400) y un mensaje inválido hacia la DLQ. Pide los tokens con entrada oculta, y si se omiten, corre solo las pruebas sin sesión.
