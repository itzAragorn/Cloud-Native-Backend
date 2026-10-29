#!/usr/bin/env bash
# Banco Cloud - pruebas end-to-end contra el BFF (local o API Gateway).
#
# Uso:
#   TOKEN_ADMIN=<jwt> [TOKEN_CLIENTE=<jwt>] ./deploy/e2e.sh [URL_API]
#
#   URL_API     por defecto http://localhost:8090/api/v1
#   TOKEN_*     Access Token de Cognito. Desde la consola del navegador con la app abierta:
#                 Object.entries(localStorage).find(([k]) => k.endsWith('.accessToken'))[1]
#               O, si el App Client permite USER_PASSWORD_AUTH:
#                 E2E_ADMIN_EMAIL=... E2E_ADMIN_PASSWORD=... (y E2E_CLIENTE_*)
#
# Prueba DLQ (opcional): necesita la API de administración de RabbitMQ, por ejemplo
# en la EC2 (http://localhost:15672) o por port forwarding de Session Manager:
#   RABBIT_MGMT_URL=http://localhost:15672  (usuario y clave de RABBITMQ_* en .env)
#   NOTIF_LOG_FILE=<archivo>                 en vez de docker logs, si notificaciones corre fuera de Docker

set -uo pipefail

API=${1:-http://localhost:8090/api/v1}
API=${API%/}
REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$REPO_DIR/.env"
SUFIJO="e2e.$(date +%s)"
PASADAS=0
FALLIDAS=0

env_value() { [[ -f $ENV_FILE ]] && { grep -E "^$1=" "$ENV_FILE" || true; } | tail -1 | cut -d= -f2-; }

titulo() { printf '\n\033[1m%s\033[0m\n' "$*"; }
omitir() { printf '  \033[1;33m-\033[0m %s\n' "$*"; }

# Verifica el código HTTP de una petición: comprobar <descripción> <esperado> <método> <ruta> [token] [cuerpo]
comprobar() {
  local descripcion=$1 esperado=$2 metodo=$3 ruta=$4 token=${5:-} cuerpo=${6:-}
  local args=(-s -o /tmp/e2e-respuesta.$$ -w '%{http_code}' -X "$metodo" "$API$ruta")
  [[ -n $token ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n $cuerpo ]] && args+=(-H 'Content-Type: application/json' -d "$cuerpo")

  local codigo
  codigo=$(curl "${args[@]}")
  RESPUESTA=$(cat /tmp/e2e-respuesta.$$ 2>/dev/null); rm -f /tmp/e2e-respuesta.$$

  if [[ $codigo == "$esperado" ]]; then
    printf '  \033[1;32m✔\033[0m %-58s %s\n' "$descripcion" "$codigo"
    PASADAS=$((PASADAS + 1))
  else
    printf '  \033[1;31m✘\033[0m %-58s %s (esperado %s)\n' "$descripcion" "$codigo" "$esperado"
    [[ -n $RESPUESTA ]] && printf '      %.200s\n' "$RESPUESTA"
    FALLIDAS=$((FALLIDAS + 1))
  fi
}

# Obtiene un Access Token con USER_PASSWORD_AUTH: token_por_password <email> <password>
token_por_password() {
  local client_id
  client_id=${COGNITO_CLIENT_ID:-$(env_value COGNITO_CLIENT_ID)}
  aws cognito-idp initiate-auth --no-sign-request --region "${AWS_REGION:-us-east-1}" \
    --client-id "$client_id" --auth-flow USER_PASSWORD_AUTH \
    --auth-parameters "USERNAME=$1,PASSWORD=$2" \
    --query AuthenticationResult.AccessToken --output text 2>/dev/null
}

TOKEN_ADMIN=${TOKEN_ADMIN:-}
TOKEN_CLIENTE=${TOKEN_CLIENTE:-}
[[ -z $TOKEN_ADMIN && -n ${E2E_ADMIN_EMAIL:-} ]] && TOKEN_ADMIN=$(token_por_password "$E2E_ADMIN_EMAIL" "$E2E_ADMIN_PASSWORD")
[[ -z $TOKEN_CLIENTE && -n ${E2E_CLIENTE_EMAIL:-} ]] && TOKEN_CLIENTE=$(token_por_password "$E2E_CLIENTE_EMAIL" "$E2E_CLIENTE_PASSWORD")

# En una terminal interactiva se piden los tokens que falten (entrada oculta)
if [[ -t 0 ]]; then
  if [[ -z $TOKEN_ADMIN ]]; then
    read -rsp "Pega el Access Token del ADMIN (Enter para omitir): " TOKEN_ADMIN; echo
  fi
  if [[ -z $TOKEN_CLIENTE ]]; then
    read -rsp "Pega el Access Token de un CLIENTE (Enter para omitir): " TOKEN_CLIENTE; echo
  fi
  TOKEN_ADMIN=${TOKEN_ADMIN//\"/}; TOKEN_CLIENTE=${TOKEN_CLIENTE//\"/}
fi

# Un Access Token es un JWT: tres partes separadas por punto que empiezan con eyJ
for var in TOKEN_ADMIN TOKEN_CLIENTE; do
  valor=${!var}
  if [[ -n $valor && ! $valor =~ ^eyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$ ]]; then
    printf '\033[1;31m✘\033[0m %s no es un JWT válido (debe empezar con eyJ y no llevar comillas)\n' "$var" >&2
    exit 2
  fi
done

printf 'Banco Cloud E2E contra \033[1m%s\033[0m\n' "$API"

# ---------------------------------------------------------------- sin autenticación
titulo "Seguridad sin token"
comprobar "GET /health es público"                         200 GET /health
comprobar "GET /fondos sin token"                          401 GET /fondos
comprobar "GET /usuarios/me con token inválido"            401 GET /usuarios/me "abc.def.ghi"
comprobar "GET /admin/rabbit sin token"                    401 GET /admin/rabbit/queues/x

# ---------------------------------------------------------------- administrador
titulo "Administrador"
if [[ -z $TOKEN_ADMIN ]]; then
  omitir "Sin TOKEN_ADMIN: se omiten las pruebas con sesión"
else
  comprobar "GET /usuarios/me"                             200 GET /usuarios/me "$TOKEN_ADMIN"
  [[ $RESPUESTA == *'"rol":"ADMIN"'* ]] || omitir "El perfil no tiene rol ADMIN: $RESPUESTA"
  comprobar "GET /usuarios (solo ADMIN)"                   200 GET /usuarios "$TOKEN_ADMIN"
  comprobar "GET /fondos"                                  200 GET /fondos "$TOKEN_ADMIN"

  titulo "Colas del sistema (consumidores conectados)"
  for cola in inversiones.comprobante.q usuarios.registro.q usuarios.estado.q fondos.valorcuota.q; do
    for nombre in "$cola" "$cola.dlq"; do
      comprobar "GET cola $nombre"                         200 GET "/admin/rabbit/queues/$nombre" "$TOKEN_ADMIN"
      [[ $RESPUESTA == *'"consumidores":0'* ]] && omitir "$nombre no tiene consumidores"
    done
  done

  titulo "Administrador de RabbitMQ (crear y borrar)"
  COLA="$SUFIJO.q"; EXCHANGE="$SUFIJO.ex"
  BINDING="{\"queue\":\"$COLA\",\"exchange\":\"$EXCHANGE\",\"routingKey\":\"e2e.#\"}"
  comprobar "POST /queues $COLA"                           201 POST /admin/rabbit/queues "$TOKEN_ADMIN" \
    "{\"name\":\"$COLA\",\"durable\":false,\"deadLetterExchange\":\"bancocloud.dlx\",\"deadLetterRoutingKey\":\"$COLA.dlq\",\"ttlMs\":60000}"
  comprobar "POST /queues repetida"                        409 POST /admin/rabbit/queues "$TOKEN_ADMIN" "{\"name\":\"$COLA\",\"durable\":false}"
  comprobar "GET /queues/$COLA"                            200 GET "/admin/rabbit/queues/$COLA" "$TOKEN_ADMIN"
  comprobar "POST /exchanges $EXCHANGE (TOPIC)"            201 POST /admin/rabbit/exchanges "$TOKEN_ADMIN" "{\"name\":\"$EXCHANGE\",\"type\":\"TOPIC\",\"durable\":false}"
  comprobar "POST /bindings"                               201 POST /admin/rabbit/bindings "$TOKEN_ADMIN" "$BINDING"
  comprobar "POST /bindings con cola inexistente"          404 POST /admin/rabbit/bindings "$TOKEN_ADMIN" \
    "{\"queue\":\"$SUFIJO.no-existe\",\"exchange\":\"$EXCHANGE\",\"routingKey\":\"x\"}"
  comprobar "DELETE /bindings"                             204 DELETE /admin/rabbit/bindings "$TOKEN_ADMIN" "$BINDING"
  comprobar "DELETE /queues/$COLA"                         204 DELETE "/admin/rabbit/queues/$COLA" "$TOKEN_ADMIN"
  comprobar "DELETE /exchanges/$EXCHANGE"                  204 DELETE "/admin/rabbit/exchanges/$EXCHANGE" "$TOKEN_ADMIN"
  comprobar "GET cola borrada"                             404 GET "/admin/rabbit/queues/$COLA" "$TOKEN_ADMIN"
  comprobar "DELETE cola inexistente"                      404 DELETE "/admin/rabbit/queues/$COLA" "$TOKEN_ADMIN"

  titulo "Validación de entrada"
  comprobar "Cola con nombre vacío"                        400 POST /admin/rabbit/queues "$TOKEN_ADMIN" '{"name":"","durable":true}'
  comprobar "Cola reservada amq.prueba"                    400 POST /admin/rabbit/queues "$TOKEN_ADMIN" '{"name":"amq.prueba","durable":true}'
  comprobar "Cola con caracteres inválidos"                400 POST /admin/rabbit/queues "$TOKEN_ADMIN" '{"name":"cola con espacios","durable":true}'
  comprobar "Cola con TTL negativo"                        400 POST /admin/rabbit/queues "$TOKEN_ADMIN" "{\"name\":\"$SUFIJO.ttl\",\"durable\":true,\"ttlMs\":-5}"
  comprobar "Cola con DLX inexistente"                     400 POST /admin/rabbit/queues "$TOKEN_ADMIN" "{\"name\":\"$SUFIJO.dlx\",\"durable\":true,\"deadLetterExchange\":\"$SUFIJO.no-existe\"}"
  comprobar "Exchange de tipo inválido"                    400 POST /admin/rabbit/exchanges "$TOKEN_ADMIN" "{\"name\":\"$SUFIJO.tipo\",\"type\":\"OTRO\",\"durable\":true}"
  [[ -n $RESPUESTA ]] && printf '      ejemplo de error: %.160s\n' "$RESPUESTA"
fi

# ---------------------------------------------------------------- cliente
titulo "Cliente"
if [[ -z $TOKEN_CLIENTE ]]; then
  omitir "Sin TOKEN_CLIENTE: se omiten las pruebas de permisos de cliente"
else
  comprobar "GET /usuarios/me"                             200 GET /usuarios/me "$TOKEN_CLIENTE"
  comprobar "GET /usuarios (endpoint de admin)"            403 GET /usuarios "$TOKEN_CLIENTE"
  comprobar "GET /admin/rabbit (endpoint de admin)"        403 GET /admin/rabbit/queues/usuarios.registro.q "$TOKEN_CLIENTE"
fi

# ---------------------------------------------------------------- DLQ
titulo "Mensaje inválido hacia la DLQ"
RABBIT_MGMT_URL=${RABBIT_MGMT_URL:-}
if [[ -z $RABBIT_MGMT_URL ]]; then
  omitir "Sin RABBIT_MGMT_URL: se omite la prueba de DLQ"
else
  usuario=${RABBIT_MGMT_USER:-$(env_value RABBITMQ_USERNAME)}
  clave=${RABBIT_MGMT_PASSWORD:-$(env_value RABBITMQ_PASSWORD)}
  id="$SUFIJO-dlq"
  publicado=$(curl -s -u "$usuario:$clave" -H 'Content-Type: application/json' \
    -X POST "$RABBIT_MGMT_URL/api/exchanges/%2F/bancocloud.inversiones/publish" \
    -d "{\"properties\":{\"message_id\":\"$id\",\"content_type\":\"application/json\"},\"routing_key\":\"inversion.creada\",\"payload\":\"{json roto\",\"payload_encoding\":\"string\"}")

  if [[ $publicado != *'"routed":true'* ]]; then
    printf '  \033[1;31m✘\033[0m No se pudo publicar en bancocloud.inversiones: %s\n' "$publicado"
    FALLIDAS=$((FALLIDAS + 1))
  else
    sleep 3
    if [[ -n ${NOTIF_LOG_FILE:-} ]]; then
      log=$(grep -F "$id" "$NOTIF_LOG_FILE")
    else
      log=$(docker logs bancocloud-notificaciones-service 2>&1 | grep -F "$id")
    fi
    if [[ $log == *"enviado a DLQ"* && $log == *"[DLQ]"* ]]; then
      printf '  \033[1;32m✔\033[0m %-58s\n' "NACK a DLQ y registro de x-death ($id)"
      grep -F "[DLQ]" <<< "$log" | sed -E 's/.*(\[DLQ\])/      \1/' | cut -c1-200
      PASADAS=$((PASADAS + 1))
    else
      printf '  \033[1;31m✘\033[0m %-58s\n' "El mensaje $id no aparece en el log de la DLQ"
      FALLIDAS=$((FALLIDAS + 1))
    fi
  fi
fi

printf '\n\033[1mResultado:\033[0m %d pasadas, %d fallidas\n' "$PASADAS" "$FALLIDAS"
[[ $FALLIDAS -eq 0 ]]
