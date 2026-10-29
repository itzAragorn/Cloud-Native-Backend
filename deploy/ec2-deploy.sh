#!/usr/bin/env bash
# Banco Cloud - despliegue del backend en la EC2 (Amazon Linux 2023 o Ubuntu).
#
# En la EC2 corre solo el backend (RabbitMQ + 6 servicios). El frontend se publica
# en S3/CloudFront con Cloud-Native-Front-end/scripts/deploy-s3.sh.
#
# Uso (desde la carpeta del repo, con EV1/.env completo):
#   ./deploy/ec2-deploy.sh preparar     instala Docker, Compose, Buildx y swap (una vez)
#   ./deploy/ec2-deploy.sh rds-init     crea las bases y el usuario de la app en RDS (una vez)
#   ./deploy/ec2-deploy.sh desplegar    levanta el backend (ver abajo)
#   ./deploy/ec2-deploy.sh estado       contenedores y salud
#   ./deploy/ec2-deploy.sh logs [svc]   logs de un servicio (o de todos)
#
# desplegar tiene dos modos:
#   - Con imagenes.tar.gz en la carpeta (paquete de deploy/build-bundle.sh): carga las
#     imágenes y levanta sin compilar ni descargar nada. Para una EC2 sin internet.
#   - Sin él: git pull (si es un clon) y build de las imágenes. Requiere internet.

set -euo pipefail

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$REPO_DIR/.env"
BACKEND_SERVICES=(rabbitmq usuarios-service fondos-service inversiones-service
                  notificaciones-service rabbit-admin-service bff-service)
REQUIRED_VARS=(USUARIOS_DB_URL FONDOS_DB_URL INVERSIONES_DB_URL DB_USERNAME DB_PASSWORD
               COGNITO_ISSUER COGNITO_CLIENT_ID RABBITMQ_USERNAME RABBITMQ_PASSWORD
               APP_CORS_ALLOWED_ORIGINS)

info()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()    { printf '\033[1;32m ✔\033[0m %s\n' "$*"; }
warn()  { printf '\033[1;33m !\033[0m %s\n' "$*"; }
fail()  { printf '\033[1;31m ✘\033[0m %s\n' "$*" >&2; exit 1; }

PAQUETE_IMAGENES="$REPO_DIR/imagenes.tar.gz"

compose() {
  docker compose -f "$REPO_DIR/docker-compose.yml" -f "$REPO_DIR/docker-compose.aws.yml" \
    --project-directory "$REPO_DIR" "$@"
}

env_value() { { grep -E "^$1=" "$ENV_FILE" || true; } | tail -1 | cut -d= -f2-; }

# ---------------------------------------------------------------- preparar
preparar() {
  . /etc/os-release
  info "Sistema: $PRETTY_NAME ($(uname -m))"

  case "$ID" in
    amzn)
      sudo dnf install -y docker git
      instalar_plugins_docker
      ;;
    ubuntu|debian)
      sudo apt-get update
      sudo apt-get install -y docker.io docker-compose-v2 docker-buildx git
      ;;
    *) fail "Distribución no soportada: $ID" ;;
  esac

  sudo systemctl enable --now docker
  sudo usermod -aG docker "$USER"
  ok "Docker $(sudo docker version --format '{{.Server.Version}}')"
  ok "$(sudo docker compose version)"

  # 6 JVMs + RabbitMQ + build de Maven: con menos de ~6 GB de RAM hace falta swap
  local ram_mb
  ram_mb=$(free -m | awk '/^Mem:/ {print $2}')
  if [[ $ram_mb -lt 6000 && ! -f /swapfile ]]; then
    info "RAM ${ram_mb} MB: creando 2 GB de swap"
    sudo dd if=/dev/zero of=/swapfile bs=1M count=2048 status=none
    sudo chmod 600 /swapfile
    sudo mkswap /swapfile >/dev/null
    sudo swapon /swapfile
    echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab >/dev/null
    ok "Swap activo"
  fi

  warn "Cierra la sesión y vuelve a entrar para usar docker sin sudo"
}

# Amazon Linux trae Docker sin los plugins compose y buildx
instalar_plugins_docker() {
  local arch dir
  arch=$(uname -m); [[ $arch == aarch64 ]] && arch=arm64 || arch=amd64
  dir=/usr/local/lib/docker/cli-plugins
  sudo mkdir -p "$dir"

  if [[ ! -x $dir/docker-compose ]]; then
    sudo curl -fsSL -o "$dir/docker-compose" \
      "https://github.com/docker/compose/releases/latest/download/docker-compose-linux-$(uname -m)"
    sudo chmod +x "$dir/docker-compose"
  fi

  if [[ ! -x $dir/docker-buildx ]]; then
    local tag
    tag=$(curl -fsSL https://api.github.com/repos/docker/buildx/releases/latest \
          | grep -m1 '"tag_name"' | cut -d'"' -f4)
    sudo curl -fsSL -o "$dir/docker-buildx" \
      "https://github.com/docker/buildx/releases/download/$tag/buildx-$tag.linux-$arch"
    sudo chmod +x "$dir/docker-buildx"
  fi
}

# ---------------------------------------------------------------- validar .env
validar_env() {
  [[ -f $ENV_FILE ]] || fail "No existe $ENV_FILE (copia .env.example y complétalo)"

  local var value errores=0
  for var in "${REQUIRED_VARS[@]}"; do
    value=$(env_value "$var")
    if [[ -z $value ]]; then
      warn "$var está vacía"; errores=1
    elif [[ $value == *PENDIENTE* || $value == *XXXXXXXXX* || $value == *your_* || $value == *'<rds-endpoint>'* ]]; then
      warn "$var todavía tiene un valor de ejemplo"; errores=1
    fi
  done

  for var in USUARIOS_DB_URL FONDOS_DB_URL INVERSIONES_DB_URL; do
    if [[ $(env_value "$var") == *localhost* ]]; then
      warn "$var apunta a localhost, debe ser el endpoint de RDS"; errores=1
    fi
  done

  if [[ $(env_value RABBITMQ_HOST) == localhost ]]; then
    warn "RABBITMQ_HOST=localhost no funciona dentro de Docker (usa rabbitmq)"; errores=1
  fi
  if [[ $(env_value RABBITMQ_USERNAME) == guest ]]; then
    warn "El usuario guest de RabbitMQ solo acepta conexiones locales"; errores=1
  fi
  if [[ $(env_value APP_CORS_ALLOWED_ORIGINS) != *https://* ]]; then
    warn "APP_CORS_ALLOWED_ORIGINS no incluye la URL pública (https) del frontend"
  fi

  [[ $errores -eq 0 ]] || fail "Corrige $ENV_FILE antes de continuar"
  chmod 600 "$ENV_FILE"
  ok ".env válido"
}

# ---------------------------------------------------------------- rds-init
rds_init() {
  validar_env

  local url host port app_user app_pass master master_pass
  url=$(env_value USUARIOS_DB_URL)
  host=$(sed -E 's#jdbc:mysql://([^:/]+).*#\1#' <<< "$url")
  port=$(sed -nE 's#jdbc:mysql://[^:/]+:([0-9]+).*#\1#p' <<< "$url")
  port=${port:-3306}
  app_user=$(env_value DB_USERNAME)
  app_pass=$(env_value DB_PASSWORD)

  info "RDS: $host:$port"
  read -rp "Usuario maestro de RDS [admin]: " master
  master=${master:-admin}
  read -rsp "Contraseña de $master: " master_pass; echo

  # La contraseña de la app va dentro de un literal SQL: se duplican las comillas simples
  local pass_sql=${app_pass//\'/\'\'}

  {
    cat "$REPO_DIR/scripts-sql/00-init-databases.sql"
    cat <<SQL
CREATE USER IF NOT EXISTS '$app_user'@'%' IDENTIFIED BY '$pass_sql';
ALTER USER '$app_user'@'%' IDENTIFIED BY '$pass_sql';
GRANT ALL PRIVILEGES ON usuarios_db.* TO '$app_user'@'%';
GRANT ALL PRIVILEGES ON fondos_db.* TO '$app_user'@'%';
GRANT ALL PRIVILEGES ON inversiones_db.* TO '$app_user'@'%';
SQL
  } | docker run --rm -i -e MYSQL_PWD="$master_pass" mysql:8.4 \
        mysql -h "$host" -P "$port" -u "$master" --connect-timeout=10

  ok "Bases y usuario $app_user listos. Comprobando acceso con el usuario de la app:"
  docker run --rm -e MYSQL_PWD="$app_pass" mysql:8.4 \
    mysql -h "$host" -P "$port" -u "$app_user" --connect-timeout=10 -e "SHOW DATABASES;"
}

# ---------------------------------------------------------------- desplegar
desplegar() {
  validar_env

  if [[ -f $PAQUETE_IMAGENES ]]; then
    cargar_paquete
    info "Levantando el backend con las imágenes del paquete"
    compose up -d --no-build --pull never --remove-orphans "${BACKEND_SERVICES[@]}"
  else
    if [[ -d $REPO_DIR/.git ]]; then
      info "Actualizando código ($(git -C "$REPO_DIR" rev-parse --abbrev-ref HEAD))"
      git -C "$REPO_DIR" pull --ff-only
      ok "Commit $(git -C "$REPO_DIR" log -1 --format='%h %s')"
    else
      warn "La carpeta no es un clon de git: se usa el código tal como está"
    fi
    info "Construyendo y levantando el backend (la primera vez tarda 10-20 minutos)"
    compose up -d --build --remove-orphans "${BACKEND_SERVICES[@]}"
  fi

  # El BFF depende de todos los demás: cuando está healthy, el backend completo lo está
  info "Esperando a que el BFF esté healthy"
  local estado
  for _ in $(seq 1 60); do
    estado=$(docker inspect -f '{{.State.Health.Status}}' bancocloud-bff-service 2>/dev/null || echo creando)
    [[ $estado == healthy ]] && break
    sleep 10
  done

  estado_servicios
  [[ $estado == healthy ]] || fail "El backend no quedó healthy; revisa: $0 logs <servicio>"
  ok "Backend desplegado. El ALB debe usar el health check /api/v1/health en el puerto 8090"
}

# Carga en Docker las imágenes del paquete, una sola vez por versión del paquete
cargar_paquete() {
  local version cargada
  version=$(cat "$REPO_DIR/VERSION" 2>/dev/null | tr '\n' ' ')
  cargada=$(cat "$REPO_DIR/.imagenes-cargadas" 2>/dev/null || true)
  [[ -f $REPO_DIR/VERSION ]] && info "Paquete: $version"

  if [[ $cargada == "$version" ]] && docker image inspect bancocloud/bff-service:ep3 >/dev/null 2>&1; then
    ok "Imágenes de este paquete ya cargadas"
    return
  fi

  # docker load descomprime: necesita ~2,5 veces el tamaño del archivo en el disco de Docker
  local tamano necesario_mb raiz libre_mb
  tamano=$(stat -c %s "$PAQUETE_IMAGENES" 2>/dev/null || stat -f %z "$PAQUETE_IMAGENES")
  necesario_mb=$(( tamano * 5 / 2 / 1048576 ))
  raiz=$(docker info -f '{{.DockerRootDir}}')
  [[ -d $raiz ]] || raiz=/    # Docker Desktop: la raíz está dentro de su VM
  libre_mb=$(df -Pm "$raiz" | awk 'NR == 2 {print $4}')
  info "Disco libre para Docker: ${libre_mb} MB (se necesitan ~${necesario_mb} MB)"
  [[ $libre_mb -ge $necesario_mb ]] || fail "No hay espacio suficiente: libera disco o amplía el volumen"

  info "Cargando imágenes (unos minutos)"
  docker load -i "$PAQUETE_IMAGENES"
  echo "$version" > "$REPO_DIR/.imagenes-cargadas"
  ok "Imágenes cargadas"
}

estado_servicios() {
  compose ps --format 'table {{.Service}}\t{{.Status}}'
  echo
  printf 'BFF /api/v1/health: '
  curl -fsS http://localhost:8090/api/v1/health || echo "sin respuesta"
  echo
}

# ---------------------------------------------------------------- main
case "${1:-}" in
  preparar)  preparar ;;
  rds-init)  rds_init ;;
  desplegar) desplegar ;;
  estado)    estado_servicios ;;
  logs)      compose logs --tail=200 -f ${2:+"$2"} ;;
  *)         sed -n '2,13p' "$0" | sed 's/^# \{0,1\}//' ;;
esac
