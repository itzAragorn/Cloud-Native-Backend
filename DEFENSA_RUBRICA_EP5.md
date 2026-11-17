# Defensa EP5 — Kafka en Banco Cloud

Guía para la presentación: para cada indicador de la rúbrica, dónde está en el código, cómo se cumple, preguntas probables con su respuesta y brechas conocidas. Las rutas son relativas a la raíz del backend.

**Resumen.** Kafka se agregó junto a RabbitMQ, sin cambiar su comportamiento:

- RabbitMQ sigue con las tareas asíncronas (comprobantes, avisos).
- Kafka lleva el flujo de eventos para analítica y auditoría, con historial que se puede volver a leer.
- Hay dos tópicos: `bancocloud.inversiones.eventos` y `bancocloud.fondos.valorcuota`, más sus `.DLT`.
- Hay dos servicios nuevos: `analitica-service` (8086) y `kafka-admin-service` (8087).
- 195 tests en verde (eran 110).
- Quedó probado de punta a punta en Docker local: 3 brokers, clúster RabbitMQ de 3 nodos, Kafka-UI y MySQL local en lugar de RDS.

---

## 1. Configuración del cliente Kafka (12%)

*Bootstrap servers, serializers, consumer groups, propiedades avanzadas, tópicos con particiones y réplicas.*

**Dónde está**
- Productores: `inversiones-service/src/main/resources/application.yml` y `fondos-service/.../application.yml`, bloque `spring.kafka`.
- Consumidores: `analitica-service/.../application.yml` y `notificaciones-service/.../application.yml`.
- `messaging/kafka/config/KafkaProducerConfig` arma el `ProducerFactory` y el `KafkaTemplate` desde `application.yml`.
- `analitica-service/.../messaging/kafka/config/KafkaConsumerConfig`: una ContainerFactory por tópico, más la de los DLT.
- `BancoKafkaProperties`: `@ConfigurationProperties(prefix = "app.kafka")` con `@Validated`, en cada servicio.

**Cómo se cumple**
- `bootstrap-servers` lista los 3 brokers y sale de `KAFKA_BOOTSTRAP_SERVERS`. Dentro de Docker es `kafka-N:29092`; en el host, `localhost:909N`.
- **Serializers.** Productor: `StringSerializer` para la key y `JacksonJsonSerializer` para el valor. Es el serializer vigente de Spring Kafka 4 (Jackson 3); `JsonSerializer` está deprecado, y lo verificamos en el jar 4.1.1. Consumidor: `ErrorHandlingDeserializer` envolviendo a `JacksonJsonDeserializer`, con `spring.json.value.default.type` por tópico.
- **Propiedades avanzadas del productor:**
  - `acks=all`, `enable.idempotence=true`, `retries=3`;
  - `max.in.flight.requests.per.connection=5`;
  - `linger.ms=5`, `compression.type=lz4`;
  - `max.block.ms=5000`, para que un Kafka caído no retenga la respuesta HTTP;
  - `spring.json.add.type.headers=false`.
- **Propiedades avanzadas del consumidor:**
  - `auto-offset-reset=earliest`, `enable-auto-commit=false`, `ack-mode=record`;
  - `concurrency=3`, `max-poll-records=50`;
  - `isolation.level=read_committed`;
  - `partition.assignment.strategy=CooperativeStickyAssignor`.
- Los tópicos se declaran con 3 particiones y 3 réplicas: ver el indicador 7.
- Tests: `KafkaProducerConfigTest` arma el productor desde el `application.yml` real y revisa serializers, acks, idempotencia y `max.block.ms`. `BancoKafkaPropertiesTest` revisa el enlace y la validación.

**Preguntas probables**
- *¿Por qué `acks=all` con `min.insync.replicas=2`?* Una escritura se confirma solo cuando la tienen al menos 2 de las 3 réplicas. Si cae un broker, el sistema sigue aceptando escrituras y no pierde datos.
- *¿Para qué la idempotencia?* Si un reintento del productor reenvía un mensaje que ya había llegado, el broker lo descarta. Tampoco se desordenan los mensajes dentro de una partición.
- *¿Por qué no se mandan headers de tipo?* El consumidor no debe depender de las clases Java del productor: el tipo lo fija la configuración del consumidor. Así cada microservicio tiene su propia copia del DTO.
- *¿Por qué `max.block.ms=5000`?* El envío ocurre en el hilo de la petición HTTP (listener AFTER_COMMIT). Con el valor por defecto (60 s), un Kafka caído dejaría al usuario esperando un minuto.

**Brechas**
- Sin TLS ni SASL entre los servicios y Kafka: todo va dentro de la red Docker privada de la EC2 y los puertos externos solo escuchan en `127.0.0.1`.

---

## 2. Productores y consumidores con los dos tópicos y DTO tipados (12%)

**Dónde está**
- Productores:
  - `inversiones-service/.../messaging/kafka/producer/KafkaInversionStreamPublisher`
  - `fondos-service/.../messaging/kafka/producer/KafkaValorCuotaStreamPublisher`
- Consumidores:
  - `analitica-service/.../consumer/inversiones/InversionAnaliticaListener`
  - `analitica-service/.../consumer/fondos/ValorCuotaAnaliticaListener`
  - `notificaciones-service/.../consumer/auditoria/InversionAuditoriaKafkaListener`
- DTO: `InversionRegistradaKafkaEvent` y `ValorCuotaKafkaEvent` (records), en `messaging/kafka/event`.

**Cómo se cumple**
- Tópico 1, inversiones: lo produce inversiones-service y lo consumen 2 grupos (`analitica-inversiones` y `auditoria-inversiones`).
- Tópico 2, valor cuota: lo produce fondos-service y lo consume el grupo `analitica-fondos`.
- Los listeners reciben el DTO ya deserializado; nunca un `String` ni un `Map`.
- Prueba local de punta a punta: 1 fondo, 1 cambio de valor cuota y 4 inversiones aparecieron en el ranking de analítica, en la serie del valor cuota y en la auditoría.

**Preguntas probables**
- *¿Por qué cada servicio tiene su propia copia del DTO?* Para no acoplar los despliegues con una librería compartida. El contrato es el JSON y los dos lados lo validan.

---

## 3. Microservicio administrador (6%)

**Dónde está:** `kafka-admin-service`.
- `service/KafkaAdminService` tiene toda la lógica y encapsula el `Admin` de Kafka, que crea `config/KafkaAdminClientConfig` desde el `KafkaAdmin` de Spring.
- Los controladores (`controller/TopicController`, `ConsumerGroupController`) solo llaman a métodos de alto nivel.
- El BFF lo expone en `/api/v1/admin/kafka/**`, solo para ADMIN (`KafkaAdminProxyController`).

**Cómo se cumple**
- Tópicos:
  - listar (sin los internos `__*`);
  - ver el detalle (particiones, líder, réplicas, ISR, configuraciones);
  - crear;
  - modificar configuraciones con `incrementalAlterConfigs` (SET, o DELETE con valor `null`);
  - aumentar particiones;
  - eliminar.
- Validación:
  - nombre de hasta 249 caracteres, con patrón y sin `__`;
  - `partitions` y `replicationFactor` con mínimos y máximos;
  - `replicationFactor` no mayor que la cantidad de brokers, revisado contra el clúster;
  - lista blanca de configuraciones, cada una con su validación de valor;
  - `min.insync.replicas` no mayor que `replicationFactor`.
- Errores en JSON claro (`GlobalExceptionHandler`): 400 con detalle por campo, 404, 409 y 503.
- Tests: 33, entre ellos `KafkaAdminIntegrationTest`, que recorre la API completa contra un broker embebido.

**Preguntas probables**
- *¿Por qué no se pueden disminuir las particiones?* Kafka no lo permite: cambiaría en qué partición cae cada key y rompería el orden. La API responde 400 con la explicación.
- *¿Qué pasa si se borra un tópico que usa un servicio?* El productor que es dueño del tópico lo vuelve a crear al reiniciar (`NewTopic`). Los consumidores no fallan, porque tienen `missing-topics-fatal: false`.

---

## 4. Productores con DTO y serializers adecuados (14%)

**Dónde está:** `messaging/kafka/event/*KafkaEvent` y `messaging/kafka/producer/*`, en inversiones y fondos.

**Cómo se cumple**
- Los DTO son `record` con validaciones (`@NotBlank`, `@NotNull`, `@Positive`) y llevan `eventId` (UUID), `eventType` y fecha.
- El valor cuota trae además `variacionPct`: 4 decimales, `HALF_UP`, y `null` si el valor anterior era 0.
- El publisher valida el DTO **antes** de enviarlo: un payload inválido no sale del servicio y queda en el log.
- `ProducerRecord` con:
  - key `usuarioId` (orden por usuario) o `fondoId` (cada fondo en una partición, para la compactación);
  - headers `eventType` y `source` (el nombre del servicio).
- El resultado del `CompletableFuture` va al log: en éxito, tópico, partición y offset; en error, el motivo.
- **Separado de RabbitMQ:** un segundo `@TransactionalEventListener(AFTER_COMMIT)` propio de Kafka (`InversionKafkaEventListener`, `FondoKafkaEventListener`) se suscribe al mismo evento de Spring. No se tocó ningún archivo de RabbitMQ.
- El negocio solo conoce las interfaces `InversionStreamPublisher` y `ValorCuotaStreamPublisher`, que están en `service`.
- Ningún error de Kafka se propaga: la operación ya quedó guardada.
- Tests: `Kafka*StreamPublisherTest` revisan la key, los headers, el DTO, que un payload inválido no se envíe y que los errores sincrónicos y asíncronos no se propaguen.

**Preguntas probables**
- *¿Por qué AFTER_COMMIT?* Para no anunciar una inversión que terminó en rollback.
- *¿Qué pasa si la app cae entre el commit y el envío?* Ese evento se pierde para Kafka, porque no hay outbox. Ver brechas.
- *¿Por qué el mismo `eventId` en RabbitMQ y Kafka?* Para correlacionar los dos flujos en los logs.

**Brechas**
- No hay *transactional outbox*: si el servicio cae justo después del commit, el evento no llega ni a RabbitMQ ni a Kafka. Es la misma decisión de la EP3.

---

## 5. Consumidores con `@KafkaListener` y dominio separado (14%)

**Dónde está**
- `analitica-service`:
  - `consumer/inversiones`: `InversionAnaliticaListener` y `AnaliticaInversionService`;
  - `consumer/fondos`: `ValorCuotaAnaliticaListener` y `AnaliticaFondoService`;
  - `consumer/dlt`: `DltListener` y `DltService`.
- `notificaciones-service`: `consumer/auditoria` (`InversionAuditoriaKafkaListener` y `AuditoriaInversionService`).

**Cómo se cumple**
- Los listeners solo traducen el mensaje y dejan en el log el contexto (`@Header` de `RECEIVED_KEY`, `RECEIVED_PARTITION` y `OFFSET`); la lógica está en el `*Service`, que no conoce Kafka.
- Analítica guarda en `analitica_db` (MySQL):
  - `metrica_inversion_diaria`: monto y cantidad por fondo y día;
  - `valor_cuota_historico`: la serie del valor cuota;
  - `evento_procesado`: la idempotencia.
- Lectura: `GET /analitica/fondos/resumen` (ranking), `/{id}/valor-cuota/historial` y `/{id}/inversiones/diario`.
- Concurrencia: dos hebras pueden sumar al mismo fondo y día, porque la key es el usuario. La fila se bloquea con `PESSIMISTIC_WRITE`. Si dos hebras crean la fila al mismo tiempo, el índice único rechaza una y el reintento la suma.
- Tópico compactado: un *tombstone* (valor nulo) en valor cuota se ignora con un log.

**Preguntas probables**
- *¿Por qué analítica tiene su propia base?* Cada microservicio es dueño de sus datos. Además, si se pierde la base, se puede reconstruir lo que el tópico aún retiene (7 días de inversiones, 30 de valor cuota) releyéndolo desde `earliest` con un reset de offsets.

---

## 6. Manejo de errores (12%)

**Dónde está**
- `analitica-service/.../messaging/kafka/config/KafkaConsumerConfig#kafkaErrorHandler`
- `notificaciones-service/.../messaging/kafka/config/KafkaConsumerConfig`
- `consumer/support/EventoValidator` y `exception/EventoInvalidoException`

**Cómo se cumple**
- **Payload roto:** `ErrorHandlingDeserializer` lo convierte en `DeserializationException`, que va directo al `.DLT` sin detener el consumidor.
- **Payload inválido:** el JSON se lee bien pero falla `@NotNull` o `@Positive`, o trae otro `eventType`. Lanza `EventoInvalidoException`, que no es reintentable, y va directo al `.DLT`.
- **Errores transitorios** (MySQL caído): `DefaultErrorHandler` con `ExponentialBackOffWithMaxRetries` (1 s, 2 s, 4 s; configurable en `app.kafka.reintentos`). Después, al `.DLT`.
- `DeadLetterPublishingRecoverer` publica en la **misma partición**, con los headers `kafka_dlt-original-topic`, `-partition`, `-offset`, `-consumer-group` y `-exception-message`. Si el JSON estaba roto, usa una plantilla de bytes para conservar el mensaje original.
- `addNotRetryableExceptions(EventoInvalidoException, ValidationException, MethodArgumentNotValidException)`. `DeserializationException` ya viene marcada así por defecto.
- Un `RetryListener` deja cada intento en el log.
- **Logging contextual:** cada intento y cada envío al DLT registra tópico, partición, offset, key, `eventId` y la causa raíz.
- `DltListener` lee los dos `.DLT` como texto, registra el origen y el motivo, y nunca falla. Su error handler no reintenta.
- **Idempotencia:** `evento_procesado` se guarda en la misma transacción que la métrica, así que un duplicado se descarta.
- Tests: `AnaliticaKafkaIntegrationTest` con broker embebido:
  - JSON roto → `.DLT` con `DeserializationException`, y el siguiente mensaje se procesa;
  - payload inválido → `.DLT` sin reintentos;
  - duplicado → se cuenta una vez.
  `AuditoriaKafkaIntegrationTest` revisa lo mismo para el grupo de auditoría.

**Preguntas probables**
- *¿Por qué los reintentos bloquean la partición?* `DefaultErrorHandler` vuelve a leer desde el mismo offset para no desordenar los mensajes. El máximo son unos 7 s por mensaje malo, lejos de `max.poll.interval.ms`.
- *¿Por qué hay 2 mensajes en el DLT por un mismo mensaje roto?* Porque fallaron dos grupos (analítica y auditoría). El header `kafka_dlt-original-consumer-group` dice cuál.

---

## 7. Parámetros de tópicos por flujo (12%)

**Dónde está:** `app.kafka.topics.*` en el `application.yml` de inversiones y fondos, con comentarios, y `messaging/kafka/config/KafkaTopicConfig` (Javadoc), que usa `TopicBuilder` con `TopicConfig.*`.

| | Inversiones | Valor cuota | DLT |
|---|---|---|---|
| Particiones | 3: tres consumidores en paralelo por grupo | 3 | 3: el mensaje vuelve a la misma partición |
| Réplicas / `min.insync.replicas` | 3 / 2 | 3 / 2 | 3 / 2 |
| `cleanup.policy` | `delete`: historial de transacciones; ningún evento reemplaza a otro | `compact,delete`: lo importante es el último valor de cada fondo, y además se acota el historial | `delete`: cada fallo queda registrado |
| `retention.ms` | 7 días: margen para reprocesar | 30 días: serie histórica reciente | 14 días: tiempo para revisar los fallos |

- Clúster (`docker-compose.kafka.yml`): `default.replication.factor=3`, `min.insync.replicas=2`, `offsets.topic.replication.factor=3` y `auto.create.topics.enable=false`. Solo se crean los tópicos declarados.
- Verificado en el clúster local: `kafka-admin.sh describe` mostró las configuraciones y líderes repartidos entre los brokers 1, 2 y 3.

**Preguntas probables**
- *¿Por qué compactar el valor cuota y no las inversiones?* En el valor cuota cada mensaje nuevo reemplaza al anterior del mismo fondo (misma key). Las inversiones son hechos independientes: compactarlas haría perder transacciones.

---

## 8. Consumer groups (12%)

**Cómo se cumple**
- **Dividir la carga.** El grupo `analitica-inversiones` tiene `concurrency 3` y el tópico 3 particiones. En la prueba local, `GET /consumer-groups/analitica-inversiones` mostró 3 miembros (`analitica-service-0`, `-1` y `-2`), con una partición cada uno. Con dos instancias del servicio, Kafka reparte las particiones; `CooperativeStickyAssignor` lo hace sin detener a todos.
- **Compartir el mismo evento.** `analitica-inversiones` y `auditoria-inversiones` son grupos distintos sobre el mismo tópico, y cada uno recibió las 4 inversiones.
- **Grupos por responsabilidad:** `analitica-inversiones`, `analitica-fondos`, `auditoria-inversiones` y `analitica-dlt`. Los nombres están en `app.kafka.groups`.
- Documentado en el Javadoc de los listeners y en el README.

**Demostración sugerida:**

```bash
./kafka/scripts/kafka-admin.sh group analitica-inversiones   # partición por hebra y lag
docker compose ... up -d --scale analitica-service=2        # (requiere quitar container_name) rebalanceo
```

**Brechas**
- `container_name` impide `--scale` en Compose. Para mostrar el rebalanceo entre instancias, sirve levantar un segundo analitica-service fuera de Docker con `KAFKA_BOOTSTRAP_SERVERS=localhost:9092,...`.

---

## 9. Administración de tópicos y consumer groups (6%)

**Cómo se cumple**
- **API:** kafka-admin-service. Consumer groups: listar con estado, detalle con miembros, offsets y **lag** por partición, reset de offsets (`EARLIEST`, `LATEST` u `OFFSET`; responde 409 si el grupo está activo) y eliminar.
- **Script:** `kafka/scripts/kafka-admin.sh` con `init`, `topics`, `describe`, `create`, `alter`, `reset-config`, `partitions`, `delete`, `groups`, `group`, `reset-offsets` (con *dry run* por defecto), `delete-group` y `brokers`.
- **Kafka-UI:** `docker-compose.kafka-ui.yml` (`kafbat/kafka-ui:v1.5.0`) en `127.0.0.1:8089`. En la prueba local mostró el clúster `bancocloud` ONLINE, con 3 brokers y 5 tópicos.

---

## Docker Compose (requisito del enunciado)

| Archivo | Contenido | Verificado localmente |
|---|---|---|
| `docker-compose.rabbitmq.yml` | 3 nodos `rabbitmq:3.13-management`, misma cookie, `classic_config` y `pause_minority` | `rabbitmqctl cluster_status`: 3 *Running Nodes* |
| `docker-compose.kafka.yml` | `cp-zookeeper` y 3 `cp-kafka` 7.9.11 (Kafka 3.9, la última versión con Zookeeper); listeners `INTERNAL`/`EXTERNAL`; heap de 256 MB | 3 brokers `healthy` y tópicos creados con réplicas 3 |
| `docker-compose.kafka-ui.yml` | Kafka-UI | HTTP 200 y clúster ONLINE |
| `docker-compose.yml` | Los 8 servicios; RabbitMQ por `SPRING_RABBITMQ_ADDRESSES` (los 3 nodos), sin tocar ningún `application.yml` | Los 17 contenedores `healthy` |

**Brechas**
- Las colas de RabbitMQ siguen siendo clásicas: cada una vive en un nodo. Si ese nodo cae, la cola no está disponible hasta que vuelva. Para tolerar caídas habría que pasar a *quorum queues*, lo que exige redeclararlas; no se hizo, para no cambiar el comportamiento de la EP3.
- Kafka-UI v1 ya no usa Zookeeper, así que no lleva `KAFKA_CLUSTERS_0_ZOOKEEPER`.
- Kafka 3.9 con Zookeeper es lo que pide el enunciado. En producción nueva se usaría KRaft (Kafka 4).
