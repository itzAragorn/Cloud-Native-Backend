#!/bin/bash

# BancoCloud - Docker Compose Helper
# Script simplificado para gestionar contenedores

set -e

cd "$(dirname "$0")"

COMMAND=${1:-help}

case $COMMAND in
  up)
    echo "Levantando BancoCloud en Docker..."
    docker compose up -d
    echo ""
    docker compose ps
    echo ""
    echo "URLs disponibles:"
    echo "  Frontend:          http://localhost/"
    echo "  BFF Service:       http://localhost:8090/api/v1/health"
    echo "  RabbitMQ consola:  http://localhost:15672"
    ;;

  down)
    echo "Deteniendo BancoCloud..."
    docker compose down
    echo "✓ Contenedores detenidos"
    ;;

  logs)
    SERVICE=${2:-}
    if [ -z "$SERVICE" ]; then
      docker compose logs -f
    else
      docker compose logs -f "$SERVICE"
    fi
    ;;

  ps)
    docker compose ps
    ;;

  build)
    echo "Construyendo imágenes Docker..."
    docker compose build
    ;;

  rebuild)
    echo "Reconstruyendo todo desde cero..."
    docker compose down
    docker compose up -d --build
    docker compose ps
    ;;

  shell)
    SERVICE=${2:-bff-service}
    echo "Abriendo shell en $SERVICE..."
    docker compose exec "$SERVICE" /bin/sh
    ;;

  health)
    # Solo el BFF y el frontend publican puertos; el resto se ve por su healthcheck
    echo "Verificando salud de los servicios..."
    echo ""
    docker compose ps --format 'table {{.Service}}\t{{.Status}}'
    echo ""
    echo "BFF Service:"
    curl -s http://localhost:8090/api/v1/health || echo "No disponible"
    echo ""
    echo "Frontend:"
    curl -s http://localhost/health || echo "No disponible"
    ;;

  *)
    cat << EOF
BancoCloud Docker Manager

Uso: ./docker-compose.sh COMANDO [OPCIONES]

Comandos:
  up              Levanta todos los contenedores
  down            Detiene todos los contenedores
  logs [SERVICE]  Ver logs (opcional: especificar servicio)
  ps              Ver estado de contenedores
  build           Construir imágenes Docker
  rebuild         Reconstruir todo desde cero
  shell [SERVICE] Abrir shell en contenedor (default: bff-service)
  health          Verificar salud de todos los servicios
  help            Mostrar esta ayuda

Ejemplos:
  ./docker-compose.sh up
  ./docker-compose.sh logs bff-service
  ./docker-compose.sh shell usuarios-service
  ./docker-compose.sh health

Despliegue en la EC2: ./deploy/ec2-deploy.sh
EOF
    ;;
esac
