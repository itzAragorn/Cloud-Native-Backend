#!/usr/bin/env bash
# Banco Cloud - arma un paquete del backend para una EC2 sin acceso a internet.
#
# Compila los 6 servicios, construye sus imágenes para la arquitectura de la EC2
# (sin emulación: deploy/Dockerfile.runtime solo copia el jar), agrega RabbitMQ y
# empaqueta imágenes + compose + scripts en un .tar.gz para subir a S3.
#
# Uso:
#   ./deploy/build-bundle.sh [linux/amd64|linux/arm64]   (por defecto linux/amd64, p. ej. t3.*)
#   SOLO="bff-service" ./deploy/build-bundle.sh          paquete de actualización: solo esos
#                                                         servicios, sin RabbitMQ (más liviano)
#
# Resultado: dist/bancocloud-backend-<commit>.tar y su .sha256. Sin secretos: el .env
# se crea aparte en la EC2. Allí: tar -xf <paquete>, copiar el .env y
# ./deploy/ec2-deploy.sh desplegar (detecta imagenes.tar.gz y no necesita internet).

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PLATFORM=${1:-linux/amd64}
SERVICIOS=(usuarios-service fondos-service inversiones-service notificaciones-service
           rabbit-admin-service bff-service)
RABBIT_IMAGE=rabbitmq:3.13-management
SOLO=${SOLO:-}
if [[ -n $SOLO ]]; then
  read -ra SERVICIOS <<< "$SOLO"
fi
TAG=ep3

info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m ✔\033[0m %s\n' "$*"; }
fail() { printf '\033[1;31m ✘\033[0m %s\n' "$*" >&2; exit 1; }

cd "$REPO_DIR"
[[ -z $(git status --porcelain -- '*/src' '*/pom.xml') ]] \
  || fail "Hay cambios sin commit en el código: el paquete debe corresponder a un commit"
COMMIT=$(git rev-parse --short HEAD)
TRABAJO=$(mktemp -d)
trap 'rm -rf "$TRABAJO"' EXIT

# ---------------------------------------------------------------- jars
info "Compilando y probando los 6 servicios (commit $COMMIT)"
for s in "${SERVICIOS[@]}"; do
  (cd "$s" && ./mvnw -q clean package) || fail "Falló el build de $s"
  ok "$s"
done

# ---------------------------------------------------------------- imágenes
info "Construyendo imágenes para $PLATFORM"
IMAGENES=()
for s in "${SERVICIOS[@]}"; do
  jar=$(ls "$s"/target/"$s"-*.jar | grep -v plain | head -1)
  cp "$jar" "$TRABAJO/$s.jar"
  docker buildx build --quiet --platform "$PLATFORM" --load \
    -f deploy/Dockerfile.runtime --build-arg JAR="$s.jar" \
    -t "bancocloud/$s:$TAG" "$TRABAJO" >/dev/null
  IMAGENES+=("bancocloud/$s:$TAG")
  ok "bancocloud/$s:$TAG"
done

if [[ -z $SOLO ]]; then
  docker pull --quiet --platform "$PLATFORM" "$RABBIT_IMAGE" >/dev/null
  IMAGENES+=("$RABBIT_IMAGE")
  ok "$RABBIT_IMAGE"
fi

# Con el almacén containerd una etiqueta puede tener varias plataformas: se pide la de destino
for imagen in "${IMAGENES[@]}"; do
  docker image inspect --platform "$PLATFORM" "$imagen" >/dev/null 2>&1 \
    || fail "$imagen no tiene variante $PLATFORM"
done

# ---------------------------------------------------------------- paquete
info "Exportando imágenes"
PAQUETE="$TRABAJO/bancocloud-backend"
mkdir -p "$PAQUETE/deploy" "$PAQUETE/scripts-sql"
docker save --platform "$PLATFORM" "${IMAGENES[@]}" | gzip -1 > "$PAQUETE/imagenes.tar.gz"

cp docker-compose.yml docker-compose.aws.yml .env.example README.md "$PAQUETE/"
cp deploy/ec2-deploy.sh deploy/e2e.sh "$PAQUETE/deploy/"
cp scripts-sql/*.sql "$PAQUETE/scripts-sql/"
printf 'commit=%s\nplataforma=%s\nservicios=%s\ncreado=%s\n' \
  "$COMMIT" "$PLATFORM" "${SOLO:-todos}" "$(date -u +%FT%TZ)" > "$PAQUETE/VERSION"

mkdir -p dist
# Las imágenes ya van comprimidas: el paquete exterior es un tar simple.
# Sin metadatos de macOS (xattrs), que el tar de Linux reporta como avisos.
SALIDA="dist/bancocloud-backend-$COMMIT${SOLO:+-${SOLO// /-}}.tar"
COPYFILE_DISABLE=1 tar --no-xattrs -C "$TRABAJO" -cf "$SALIDA" bancocloud-backend
(cd dist && shasum -a 256 "$(basename "$SALIDA")" > "$(basename "$SALIDA").sha256")

ok "Paquete: $SALIDA ($(du -h "$SALIDA" | cut -f1))"
ok "SHA-256: $(cut -d' ' -f1 "$SALIDA.sha256")"
