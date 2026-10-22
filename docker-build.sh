#!/bin/bash

# BancoCloud - Docker Build and Deploy Script
# Script para compilar y desplegar BancoCloud en Docker

set -e

echo "================================================"
echo "BancoCloud - Docker Build Script"
echo "================================================"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}ADVERTENCIA: Archivo .env no encontrado${NC}"
    echo "Usando valores por defecto para AWS RDS"
    echo "Crea un archivo .env con tus credenciales RDS reales"
fi

# Step 1: Clean previous builds
echo ""
echo -e "${YELLOW}PASO 1: Limpiando builds anteriores...${NC}"
cd bff-service && ./mvnw clean > /dev/null 2>&1 || mvn clean > /dev/null 2>&1
cd ../fondos-service && ./mvnw clean > /dev/null 2>&1 || mvn clean > /dev/null 2>&1
cd ../inversiones-service && ./mvnw clean > /dev/null 2>&1 || mvn clean > /dev/null 2>&1
cd ../usuarios-service && ./mvnw clean > /dev/null 2>&1 || mvn clean > /dev/null 2>&1
cd ..
echo -e "${GREEN}✓ Builds anteriores limpios${NC}"

# Step 2: Compile Backend Services
echo ""
echo -e "${YELLOW}PASO 2: Compilando servicios backend (esto puede tomar varios minutos)...${NC}"

echo "  - Compilando bff-service..."
cd bff-service && ./mvnw clean package -DskipTests -q && cd ..
echo -e "    ${GREEN}✓ bff-service compilado${NC}"

echo "  - Compilando fondos-service..."
cd fondos-service && ./mvnw clean package -DskipTests -q && cd ..
echo -e "    ${GREEN}✓ fondos-service compilado${NC}"

echo "  - Compilando inversiones-service..."
cd inversiones-service && ./mvnw clean package -DskipTests -q && cd ..
echo -e "    ${GREEN}✓ inversiones-service compilado${NC}"

echo "  - Compilando usuarios-service..."
cd usuarios-service && ./mvnw clean package -DskipTests -q && cd ..
echo -e "    ${GREEN}✓ usuarios-service compilado${NC}"

# Step 3: Build Docker Images
echo ""
echo -e "${YELLOW}PASO 3: Construyendo imágenes Docker...${NC}"

echo "  - Construyendo bff-service..."
docker build -t bancocloud/bff-service:latest ./bff-service -q
echo -e "    ${GREEN}✓ Imagen bff-service lista${NC}"

echo "  - Construyendo fondos-service..."
docker build -t bancocloud/fondos-service:latest ./fondos-service -q
echo -e "    ${GREEN}✓ Imagen fondos-service lista${NC}"

echo "  - Construyendo inversiones-service..."
docker build -t bancocloud/inversiones-service:latest ./inversiones-service -q
echo -e "    ${GREEN}✓ Imagen inversiones-service lista${NC}"

echo "  - Construyendo usuarios-service..."
docker build -t bancocloud/usuarios-service:latest ./usuarios-service -q
echo -e "    ${GREEN}✓ Imagen usuarios-service lista${NC}"

echo "  - Construyendo frontend..."
docker build -t bancocloud/frontend:latest ../Cloud-Native-Front-end-main -q
echo -e "    ${GREEN}✓ Imagen frontend lista${NC}"

# Step 4: Summary
echo ""
echo -e "${GREEN}================================================${NC}"
echo -e "${GREEN}✓ Todas las imágenes Docker construidas${NC}"
echo -e "${GREEN}================================================${NC}"
echo ""
echo "Imágenes disponibles:"
docker images | grep bancocloud || echo "No hay imágenes bancocloud"
echo ""
echo -e "${YELLOW}PRÓXIMO PASO:${NC}"
echo "Para levantar los contenedores:"
echo ""
echo "  docker-compose up -d"
echo ""
echo "Para ver los logs:"
echo ""
echo "  docker-compose logs -f"
echo ""
echo "Para detener los contenedores:"
echo ""
echo "  docker-compose down"
echo ""
