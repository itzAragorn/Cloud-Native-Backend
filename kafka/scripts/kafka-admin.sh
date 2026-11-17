#!/usr/bin/env bash
# Banco Cloud - administración de Kafka por línea de comandos.
#
# Usa las herramientas que trae la imagen del broker (kafka-topics, kafka-configs,
# kafka-consumer-groups) dentro del contenedor bancocloud-kafka-1, así que no hace falta instalar
# nada en el host. Es la alternativa por script a kafka-admin-service.
#
# Uso: ./kafka/scripts/kafka-admin.sh <comando> [argumentos]
#
#   init                                  crea los 4 tópicos de Banco Cloud con sus parámetros
#   topics                                lista los tópicos
#   describe <tópico>                     particiones, líderes, réplicas, ISR y configuraciones
#   create <tópico> <particiones> <réplicas> [clave=valor ...]
#   alter <tópico> <clave=valor>[,<clave=valor>...]     cambia configuraciones (p. ej. retention.ms=86400000)
#   reset-config <tópico> <clave>[,<clave>...]          vuelve claves al valor por defecto
#   partitions <tópico> <total>           aumenta las particiones (no se pueden disminuir)
#   delete <tópico>                       elimina un tópico
#   groups                                lista los consumer groups con su estado
#   group <grupo>                         miembros, offsets y lag por partición
#   reset-offsets <grupo> <tópico> <earliest|latest|offset:N> [--execute]
#                                         sin --execute solo muestra el resultado (dry run)
#   delete-group <grupo>                  elimina un grupo sin consumidores activos
#   brokers                               brokers del clúster y versión de la API
#
# Variables: KAFKA_CONTAINER (por defecto bancocloud-kafka-1), KAFKA_BOOTSTRAP
# (por defecto kafka-1:29092,kafka-2:29092,kafka-3:29092).

set -euo pipefail

CONTAINER=${KAFKA_CONTAINER:-bancocloud-kafka-1}
BOOTSTRAP=${KAFKA_BOOTSTRAP:-kafka-1:29092,kafka-2:29092,kafka-3:29092}

fail() { printf '\033[1;31m ✘\033[0m %s\n' "$*" >&2; exit 1; }
info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }

k() { docker exec -i "$CONTAINER" "$@"; }

requiere() {
  local n=$1; shift
  [[ $# -ge $n ]] || fail "Faltan argumentos. Ejecuta '$0 help'"
}

crear() {
  local topic=$1 parts=$2 reps=$3; shift 3
  local args=()
  for c in "$@"; do args+=(--config "$c"); done
  k kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$topic" --partitions "$parts" --replication-factor "$reps" "${args[@]}"
}

docker inspect "$CONTAINER" >/dev/null 2>&1 \
  || fail "No existe el contenedor $CONTAINER: levanta docker-compose.kafka.yml"

CMD=${1:-help}
shift || true

case $CMD in
  init)
    # Los mismos valores que declaran inversiones-service y fondos-service (app.kafka en application.yml)
    info "Creando tópicos de Banco Cloud"
    crear bancocloud.inversiones.eventos 3 3 cleanup.policy=delete retention.ms=604800000 min.insync.replicas=2
    crear bancocloud.inversiones.eventos.DLT 3 3 cleanup.policy=delete retention.ms=1209600000 min.insync.replicas=2
    crear bancocloud.fondos.valorcuota 3 3 cleanup.policy=compact,delete retention.ms=2592000000 min.insync.replicas=2
    crear bancocloud.fondos.valorcuota.DLT 3 3 cleanup.policy=delete retention.ms=1209600000 min.insync.replicas=2
    ;;
  topics)
    k kafka-topics --bootstrap-server "$BOOTSTRAP" --list --exclude-internal
    ;;
  describe)
    requiere 1 "$@"
    k kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic "$1"
    echo
    k kafka-configs --bootstrap-server "$BOOTSTRAP" --describe --entity-type topics --entity-name "$1"
    ;;
  create)
    requiere 3 "$@"
    crear "$@"
    ;;
  alter)
    requiere 2 "$@"
    k kafka-configs --bootstrap-server "$BOOTSTRAP" --alter --entity-type topics --entity-name "$1" \
      --add-config "$2"
    ;;
  reset-config)
    requiere 2 "$@"
    k kafka-configs --bootstrap-server "$BOOTSTRAP" --alter --entity-type topics --entity-name "$1" \
      --delete-config "$2"
    ;;
  partitions)
    requiere 2 "$@"
    k kafka-topics --bootstrap-server "$BOOTSTRAP" --alter --topic "$1" --partitions "$2"
    ;;
  delete)
    requiere 1 "$@"
    k kafka-topics --bootstrap-server "$BOOTSTRAP" --delete --topic "$1"
    ;;
  groups)
    k kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --list --state
    ;;
  group)
    requiere 1 "$@"
    k kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --describe --group "$1"
    echo
    k kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --describe --group "$1" --members
    ;;
  reset-offsets)
    requiere 3 "$@"
    grupo=$1 topic=$2 destino=$3 modo=${4:---dry-run}
    case $destino in
      earliest) opcion=(--to-earliest) ;;
      latest)   opcion=(--to-latest) ;;
      offset:*) opcion=(--to-offset "${destino#offset:}") ;;
      *)        fail "Destino inválido: usa earliest, latest u offset:N" ;;
    esac
    [[ $modo == --execute || $modo == --dry-run ]] || fail "Cuarto argumento: --execute (o nada para dry run)"
    # Kafka exige que el grupo no tenga consumidores activos: detén el servicio antes
    k kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --group "$grupo" --topic "$topic" \
      --reset-offsets "${opcion[@]}" "$modo"
    ;;
  delete-group)
    requiere 1 "$@"
    k kafka-consumer-groups --bootstrap-server "$BOOTSTRAP" --delete --group "$1"
    ;;
  brokers)
    k kafka-broker-api-versions --bootstrap-server "$BOOTSTRAP" | grep -E '^[a-z0-9.-]+:[0-9]+ \(id'
    ;;
  help|*)
    sed -n '2,30p' "$0" | sed 's/^# \{0,1\}//'
    ;;
esac
