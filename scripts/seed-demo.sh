#!/usr/bin/env bash
#
# Carga datos de demostración en Mercado Páramo a través de la API.
#
# Crea: 4 productores de Guasca (3 verificados y 1 pendiente, para mostrar la verificación),
# 12 productos en las 5 categorías, 1 compradora y 5 pedidos en estados distintos.
#
# Es seguro ejecutarlo varias veces: no duplica usuarios, productos ni pedidos.
#
# Uso:
#   read -s "ADMIN_PASSWORD?Contraseña del ADMIN: "; echo; export ADMIN_PASSWORD
#   ./scripts/seed-demo.sh                                    # backend local
#   API_URL=https://api.mercadoparamo.nick-bern.com DEMO_PASSWORD='otra-clave' ./scripts/seed-demo.sh
#
# Requisitos: curl y jq.

set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
ADMIN_EMAIL="${ADMIN_EMAIL:-admin@mercado-paramo.local}"
: "${ADMIN_PASSWORD:?Define la variable ADMIN_PASSWORD (ver instrucciones al inicio del script)}"
DEMO_PASSWORD="${DEMO_PASSWORD:-clave-segura-123}"

command -v jq >/dev/null || { echo "Falta jq: brew install jq"; exit 1; }

# ---------- Utilidades HTTP ----------

api() {  # api MÉTODO RUTA [TOKEN] [CUERPO_JSON]
  local metodo=$1 ruta=$2 token=${3:-} cuerpo=${4:-}
  local args=(-sS --fail-with-body -X "$metodo" "$API_URL$ruta" -H "Content-Type: application/json")
  [[ -n $token ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n $cuerpo ]] && args+=(-d "$cuerpo")
  curl "${args[@]}"
}

login() {  # login EMAIL CONTRASEÑA → token
  api POST /api/auth/login "" "$(jq -n --arg e "$1" --arg p "$2" '{email: $e, password: $p}')" | jq -er .token
}

registrar() {  # registrar NOMBRE EMAIL ROL TELÉFONO [VEREDA] [DESCRIPCIÓN]
  local cuerpo codigo
  cuerpo=$(jq -n --arg n "$1" --arg e "$2" --arg r "$3" --arg t "$4" --arg v "${5:-}" --arg d "${6:-}" \
    --arg p "$DEMO_PASSWORD" \
    '{nombre: $n, email: $e, password: $p, telefono: $t, rol: $r}
     + (if $v != "" then {vereda: $v} else {} end)
     + (if $d != "" then {descripcion: $d} else {} end)')
  codigo=$(curl -sS -o /dev/null -w "%{http_code}" -X POST "$API_URL/api/auth/registro" \
    -H "Content-Type: application/json" -d "$cuerpo")
  case $codigo in
    201) echo "  ✓ $2 registrado" ;;
    409) echo "  · $2 ya existía" ;;
    *)   echo "  ✗ $2 → HTTP $codigo"; exit 1 ;;
  esac
}

# ---------- 1. Usuarios ----------

echo "▶ Usuarios en $API_URL"
registrar "Pedro Rodríguez" "pedro@ejemplo.com" PRODUCTOR "3104567890" "La Concepción" \
  "Quesos y lácteos de la finca El Roble, con leche de nuestras vacas."
registrar "Ana Gómez" "ana@ejemplo.com" PRODUCTOR "3157894561" "Santa Ana" \
  "Fresas, moras y uchuvas cultivadas sin químicos a 2.800 metros."
registrar "Luis Martínez" "luis@ejemplo.com" PRODUCTOR "3201239876" "La Trinidad" \
  "Papa, cubios y hortalizas de páramo, cosechados cada semana."
registrar "Rosa Castillo" "rosa@ejemplo.com" PRODUCTOR "3112223344" "Santa Isabel" \
  "Aromáticas y plantas medicinales de huerta familiar."
registrar "María López" "maria@ejemplo.com" COMPRADOR "3009876543"

# ---------- 2. Verificación (Rosa queda PENDIENTE a propósito) ----------

echo "▶ Verificación de productores"
TOKEN_ADMIN=$(login "$ADMIN_EMAIL" "$ADMIN_PASSWORD")
PENDIENTES=$(api GET "/api/admin/productores?estado=PENDIENTE" "$TOKEN_ADMIN")

for email in pedro@ejemplo.com ana@ejemplo.com luis@ejemplo.com; do
  id=$(echo "$PENDIENTES" | jq -r --arg e "$email" '.[] | select(.email == $e) | .id')
  if [[ -n $id ]]; then
    api PATCH "/api/admin/productores/$id/verificacion" "$TOKEN_ADMIN" '{"estado":"VERIFICADO"}' >/dev/null
    echo "  ✓ $email verificado"
  else
    echo "  · $email ya estaba revisado"
  fi
done
echo "  · rosa@ejemplo.com queda PENDIENTE (para mostrar la verificación en la demo)"

# ---------- 3. Productos ----------

echo "▶ Productos"
CATEGORIAS=$(api GET /api/categorias)
categoria_id() { echo "$CATEGORIAS" | jq -er --arg n "$1" '.[] | select(.nombre == $n) | .id'; }

crear_producto() {  # crear_producto TOKEN NOMBRE UNIDAD PRECIO STOCK CATEGORÍA
  local token=$1 nombre=$2
  if api GET /api/productor/productos "$token" | jq -e --arg n "$nombre" 'any(.[]; .nombre == $n)' >/dev/null; then
    echo "  · $nombre ya existía"
    return
  fi
  api POST /api/productor/productos "$token" "$(jq -n --arg n "$nombre" --arg u "$3" \
    --argjson p "$4" --argjson s "$5" --argjson c "$(categoria_id "$6")" \
    '{nombre: $n, unidad: $u, precio: $p, stock: $s, categoriaId: $c}')" >/dev/null
  echo "  ✓ $nombre"
}

TOKEN_PEDRO=$(login pedro@ejemplo.com "$DEMO_PASSWORD")
crear_producto "$TOKEN_PEDRO" "Queso campesino"  libra 12000 20 "Lácteos"
crear_producto "$TOKEN_PEDRO" "Cuajada fresca"   libra  9000 15 "Lácteos"
crear_producto "$TOKEN_PEDRO" "Leche entera"     litro  3500 30 "Lácteos"

TOKEN_ANA=$(login ana@ejemplo.com "$DEMO_PASSWORD")
crear_producto "$TOKEN_ANA" "Fresas"   libra 6000 25 "Frutas"
crear_producto "$TOKEN_ANA" "Mora"     libra 5000 18 "Frutas"
crear_producto "$TOKEN_ANA" "Uchuvas"  libra 7000  4 "Frutas"      # stock bajo: "¡Quedan 4!"

TOKEN_LUIS=$(login luis@ejemplo.com "$DEMO_PASSWORD")
crear_producto "$TOKEN_LUIS" "Papa pastusa"  kilo   2500 50 "Tubérculos"
crear_producto "$TOKEN_LUIS" "Papa criolla"  kilo   3500 30 "Tubérculos"
crear_producto "$TOKEN_LUIS" "Cubios"        kilo   4000  3 "Tubérculos"  # stock bajo
crear_producto "$TOKEN_LUIS" "Lechuga crespa" unidad 2000 20 "Hortalizas"
crear_producto "$TOKEN_LUIS" "Zanahoria"     kilo   2800 25 "Hortalizas"
crear_producto "$TOKEN_LUIS" "Cilantro"      atado  1000 15 "Hierbas y aromáticas"

# ---------- 4. Pedidos (solo si María aún no tiene ninguno) ----------

echo "▶ Pedidos"
TOKEN_MARIA=$(login maria@ejemplo.com "$DEMO_PASSWORD")

if [[ $(api GET /api/pedidos "$TOKEN_MARIA" | jq length) -gt 0 ]]; then
  echo "  · María ya tiene pedidos; no se crean más"
else
  PRODUCTOS=$(api GET /api/productos)
  producto_id() { echo "$PRODUCTOS" | jq -er --arg n "$1" '.[] | select(.nombre == $n) | .id'; }

  crear_pedido() {  # crear_pedido "Nombre:cantidad" ... → id del pedido
    local items="[]" par
    for par in "$@"; do
      items=$(echo "$items" | jq --argjson id "$(producto_id "${par%%:*}")" --argjson c "${par##*:}" \
        '. + [{productoId: $id, cantidad: $c}]')
    done
    api POST /api/pedidos "$TOKEN_MARIA" "$(jq -n --argjson i "$items" \
      '{direccionEntrega: "Calle 5 # 3-20, Guasca centro", items: $i}')" | jq -er .id
  }

  estado() {  # estado TOKEN_PRODUCTOR ID_PEDIDO NUEVO_ESTADO
    api PATCH "/api/productor/pedidos/$2/estado" "$1" "{\"estado\":\"$3\"}" >/dev/null
  }

  p1=$(crear_pedido "Queso campesino:2" "Cuajada fresca:1")
  estado "$TOKEN_PEDRO" "$p1" ACEPTADO
  estado "$TOKEN_PEDRO" "$p1" ENTREGADO
  echo "  ✓ #$p1 a Pedro → ENTREGADO (cuenta en el reporte y el impacto)"

  p2=$(crear_pedido "Fresas:3" "Mora:2")
  estado "$TOKEN_ANA" "$p2" ACEPTADO
  estado "$TOKEN_ANA" "$p2" ENTREGADO
  echo "  ✓ #$p2 a Ana → ENTREGADO"

  p3=$(crear_pedido "Leche entera:4")
  echo "  ✓ #$p3 a Pedro → PENDIENTE (para aceptarlo en vivo)"

  p4=$(crear_pedido "Papa criolla:2" "Zanahoria:1")
  estado "$TOKEN_LUIS" "$p4" ACEPTADO
  echo "  ✓ #$p4 a Luis → ACEPTADO (para marcarlo entregado en vivo)"

  p5=$(crear_pedido "Uchuvas:1")
  estado "$TOKEN_ANA" "$p5" RECHAZADO
  echo "  ✓ #$p5 a Ana → RECHAZADO (devuelve el stock)"
fi

echo
echo "✅ Listo. Usuarios de demo (contraseña: la de DEMO_PASSWORD):"
echo "   Productores: pedro@ / ana@ / luis@ejemplo.com (verificados) · rosa@ejemplo.com (pendiente)"
echo "   Compradora:  maria@ejemplo.com"
