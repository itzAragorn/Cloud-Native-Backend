#!/bin/bash

# ============================================
# Script para Ejecutar Scripts SQL
# ============================================
# Ejecuta automáticamente los scripts de población
# de todas las bases de datos de Banco Cloud

set -e  # Salir si hay error

# Colores para la salida
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # Sin color

# Configuración
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:-}"
MYSQL_HOST="${MYSQL_HOST:-localhost}"
SCRIPTS_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Poblador de Bases de Datos - Banco Cloud${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Función para ejecutar script
execute_script() {
  local script=$1
  local db=$2
  local description=$3

  echo -e "${YELLOW}→${NC} $description"
  echo -e "${YELLOW}  Ejecutando: $script en base de datos '$db'${NC}"
  
  if [ -n "$MYSQL_PASSWORD" ]; then
    mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$db" < "$SCRIPTS_DIR/$script"
  else
    mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" "$db" < "$SCRIPTS_DIR/$script"
  fi
  
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ $description completado${NC}"
  else
    echo -e "${RED}✗ Error en $description${NC}"
    exit 1
  fi
  echo ""
}

# Verificar que MySQL esté disponible
echo -e "${BLUE}Verificando conexión a MySQL...${NC}"
if [ -n "$MYSQL_PASSWORD" ]; then
  mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" -e "SELECT 1" > /dev/null 2>&1
else
  mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -e "SELECT 1" > /dev/null 2>&1
fi

if [ $? -ne 0 ]; then
  echo -e "${RED}✗ No se pudo conectar a MySQL${NC}"
  echo -e "${YELLOW}Asegúrate que:${NC}"
  echo "  1. MySQL está corriendo"
  echo "  2. Usuario: $MYSQL_USER"
  echo "  3. Host: $MYSQL_HOST"
  echo ""
  echo -e "${YELLOW}Para especificar contraseña:${NC}"
  echo "  export MYSQL_PASSWORD='tu_contraseña'"
  exit 1
fi
echo -e "${GREEN}✓ Conexión a MySQL establecida${NC}"
echo ""

# Ejecutar scripts
echo -e "${BLUE}Iniciando población de bases de datos...${NC}"
echo ""

execute_script "01-populate-usuarios.sql" "usuarios_db" "Poblando usuarios-service"
execute_script "02-populate-fondos.sql" "fondos_db" "Poblando fondos-service"
execute_script "03-populate-inversiones.sql" "inversiones_db" "Poblando inversiones-service"

# Resumen
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✓ Población de bases de datos completada${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""
echo -e "${YELLOW}Datos creados:${NC}"
echo "  • 4 usuarios en usuarios-service"
echo "  • 6 fondos en fondos-service"
echo "  • 8 inversiones en inversiones-service"
echo ""
echo -e "${YELLOW}Siguientes pasos:${NC}"
echo "  1. Inicia todos los servicios"
echo "  2. Prueba el login con los datos del README.md"
echo "  3. Usa los comandos cURL en test-endpoints.sh"
echo ""
echo -e "${YELLOW}Usuarios de prueba:${NC}"
echo "  • Admin: admin@bancocloud.com | Admin@123456"
echo "  • Cliente (autorizado): cliente2@bancocloud.com | Cliente456@789"
echo "  • Cliente (no autorizado): cliente1@bancocloud.com | Cliente123@456"
echo ""
