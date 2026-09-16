#!/bin/bash

# ============================================
# Script de Pruebas - Endpoints Banco Cloud
# ============================================
# Ejecuta requests cURL contra los endpoints
# para probar la funcionalidad con datos de prueba

# Configuración
API_URL="${API_URL:-http://localhost:8090}"
ADMIN_EMAIL="admin@bancocloud.com"
ADMIN_PASSWORD="Admin@123456"
CLIENT_EMAIL="cliente2@bancocloud.com"
CLIENT_PASSWORD="Cliente456@789"
UNAUTHORIZED_EMAIL="cliente1@bancocloud.com"
UNAUTHORIZED_PASSWORD="Cliente123@456"

# Colores
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Test de Endpoints - Banco Cloud${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}API URL: $API_URL${NC}"
echo ""

# Función para hacer request y mostrar resultado
test_endpoint() {
  local name=$1
  local method=$2
  local endpoint=$3
  local data=$4
  local token=$5

  echo -e "${YELLOW}→${NC} $name"
  
  if [ -n "$token" ]; then
    echo -e "${BLUE}  Comando:${NC}"
    echo "  curl -X $method \"$API_URL$endpoint\" \\"
    echo "    -H \"Authorization: Bearer {TOKEN}\" \\"
    if [ -n "$data" ]; then
      echo "    -H \"Content-Type: application/json\" \\"
      echo "    -d '$data'"
    fi
    echo ""
    
    if [ -n "$data" ]; then
      response=$(curl -s -X $method "$API_URL$endpoint" \
        -H "Authorization: Bearer $token" \
        -H "Content-Type: application/json" \
        -d "$data")
    else
      response=$(curl -s -X $method "$API_URL$endpoint" \
        -H "Authorization: Bearer $token")
    fi
  else
    echo -e "${BLUE}  Comando:${NC}"
    echo "  curl -X $method \"$API_URL$endpoint\" \\"
    if [ -n "$data" ]; then
      echo "    -H \"Content-Type: application/json\" \\"
      echo "    -d '$data'"
    fi
    echo ""
    
    if [ -n "$data" ]; then
      response=$(curl -s -X $method "$API_URL$endpoint" \
        -H "Content-Type: application/json" \
        -d "$data")
    else
      response=$(curl -s -X $method "$API_URL$endpoint")
    fi
  fi
  
  echo -e "${BLUE}  Respuesta:${NC}"
  echo "$response" | jq '.' 2>/dev/null || echo "$response"
  echo ""
}

# ============================================
# TEST 1: Health Check
# ============================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}1. HEALTH CHECK${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

test_endpoint "Health Check" "GET" "/api/v1/health"

# ============================================
# TEST 2: Login Tests
# ============================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}2. AUTENTICACIÓN - LOGIN${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

# Login Admin
echo -e "${YELLOW}Intentando login como ADMIN...${NC}"
admin_response=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$ADMIN_EMAIL\",\"password\":\"$ADMIN_PASSWORD\"}")

echo -e "${BLUE}Respuesta:${NC}"
echo "$admin_response" | jq '.' 2>/dev/null || echo "$admin_response"
echo ""

# Extraer token admin
ADMIN_TOKEN=$(echo "$admin_response" | jq -r '.token' 2>/dev/null || echo "")
if [ -n "$ADMIN_TOKEN" ] && [ "$ADMIN_TOKEN" != "null" ]; then
  echo -e "${GREEN}✓ Token Admin obtenido${NC}"
  echo "  Token: ${ADMIN_TOKEN:0:50}..."
  echo ""
else
  echo -e "${RED}✗ Error al obtener token admin${NC}"
  echo ""
  ADMIN_TOKEN=""
fi

# Login Cliente Autorizado
echo -e "${YELLOW}Intentando login como CLIENTE (autorizado)...${NC}"
client_response=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$CLIENT_EMAIL\",\"password\":\"$CLIENT_PASSWORD\"}")

echo -e "${BLUE}Respuesta:${NC}"
echo "$client_response" | jq '.' 2>/dev/null || echo "$client_response"
echo ""

# Extraer token cliente
CLIENT_TOKEN=$(echo "$client_response" | jq -r '.token' 2>/dev/null || echo "")
if [ -n "$CLIENT_TOKEN" ] && [ "$CLIENT_TOKEN" != "null" ]; then
  echo -e "${GREEN}✓ Token Cliente obtenido${NC}"
  echo "  Token: ${CLIENT_TOKEN:0:50}..."
  echo ""
else
  echo -e "${RED}✗ Error al obtener token cliente${NC}"
  echo ""
  CLIENT_TOKEN=""
fi

# Login Cliente No Autorizado
echo -e "${YELLOW}Intentando login como CLIENTE (NO autorizado)...${NC}"
unauthorized_response=$(curl -s -X POST "$API_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$UNAUTHORIZED_EMAIL\",\"password\":\"$UNAUTHORIZED_PASSWORD\"}")

echo -e "${BLUE}Respuesta esperada: Error 401${NC}"
echo "$unauthorized_response" | jq '.' 2>/dev/null || echo "$unauthorized_response"
echo ""

# ============================================
# TEST 3: Fondos
# ============================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}3. FONDOS - Listar fondos${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ -n "$CLIENT_TOKEN" ]; then
  test_endpoint "GET /api/v1/fondos" "GET" "/api/v1/fondos" "" "$CLIENT_TOKEN"
else
  echo -e "${RED}No hay token disponible para este test${NC}"
  echo ""
fi

# ============================================
# TEST 4: Usuarios
# ============================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}4. USUARIOS - Listar todos (ADMIN)${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ -n "$ADMIN_TOKEN" ]; then
  test_endpoint "GET /api/v1/usuarios" "GET" "/api/v1/usuarios" "" "$ADMIN_TOKEN"
else
  echo -e "${RED}No hay token admin disponible${NC}"
  echo ""
fi

# ============================================
# TEST 5: Inversiones
# ============================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}5. INVERSIONES - Inversiones del usuario${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

if [ -n "$CLIENT_TOKEN" ]; then
  # Asumir usuario_id = 3 (cliente2)
  test_endpoint "GET /api/v1/inversiones/usuario/3" "GET" "/api/v1/inversiones/usuario/3" "" "$CLIENT_TOKEN"
else
  echo -e "${RED}No hay token disponible${NC}"
  echo ""
fi

# ============================================
# Resumen
# ============================================
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo -e "${BLUE}TESTS COMPLETADOS${NC}"
echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
echo ""

echo -e "${YELLOW}Notas:${NC}"
echo "  • Asegúrate que todos los servicios están corriendo"
echo "  • El puerto por defecto es 8090 (BFF-Service)"
echo "  • Puedes cambiar API_URL: export API_URL=http://localhost:8090"
echo ""

echo -e "${YELLOW}Para más pruebas, edita este script o usa Postman${NC}"
echo ""
