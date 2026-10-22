#!/bin/bash

# BancoCloud - Docker Compose Helper
# Script simplificado para gestionar contenedores

set -e

COMMAND=${1:-help}

case $COMMAND in
  up)
    echo "Levantando BancoCloud en Docker..."
    docker-compose up -d
    echo ""
    echo "Servicios levantados:"
    docker-compose ps
    echo ""
    echo "Esperando que los servicios estén listos..."
    sleep 10
    echo ""
    echo "URLs disponibles:"
    echo "  BFF Service:           http://localhost:8090/api/v1/health"
    echo "  Frontend:              http://localhost/"
    echo "  Fondos Service:        http://localhost:8081/actuator/health"
    echo "  Inversiones Service:   http://localhost:8082/actuator/health"
    echo "  Usuarios Service:      http://localhost:8083/actuator/health"
    ;;

  down)
    echo "Deteniendo BancoCloud..."
    docker-compose down
    echo "✓ Contenedores detenidos"
    ;;

  logs)
    SERVICE=${2:-}
    if [ -z "$SERVICE" ]; then
      docker-compose logs -f
    else
      docker-compose logs -f $SERVICE
    fi
    ;;

  ps)
    docker-compose ps
    ;;

  build)
    echo "Compilando y construyendo imágenes Docker..."
    bash docker-build.sh
    ;;

  rebuild)
    echo "Reconstruyendo todo desde cero..."
    docker-compose down
    bash docker-build.sh
    docker-compose up -d
    sleep 10
    docker-compose ps
    ;;

  shell)
    SERVICE=${2:bff-service}
    echo "Abriendo shell en $SERVICE..."
    docker-compose exec $SERVICE /bin/sh
    ;;

  health)
    echo "Verificando salud de los servicios..."
    echo ""
    echo "BFF Service:"
    curl -s http://localhost:8090/api/v1/health | jq . || echo "No disponible"
    echo ""
    echo "Fondos Service:"
    curl -s http://localhost:8081/actuator/health | jq . || echo "No disponible"
    echo ""
    echo "Inversiones Service:"
    curl -s http://localhost:8082/actuator/health | jq . || echo "No disponible"
    echo ""
    echo "Usuarios Service:"
    curl -s http://localhost:8083/actuator/health | jq . || echo "No disponible"
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
  build           Compilar y construir imágenes Docker
  rebuild         Reconstruir todo desde cero
  shell [SERVICE] Abrir shell en contenedor (default: bff-service)
  health          Verificar salud de todos los servicios
  help            Mostrar esta ayuda

Ejemplos:
  ./docker-compose.sh up
  ./docker-compose.sh logs bff-service
  ./docker-compose.sh shell usuarios-service
  ./docker-compose.sh health
EOF
    ;;
esac
