#!/bin/sh
# Arranque del contenedor n8n de NutriGuide:
#   1. Genera las credenciales a partir de las variables de entorno y las importa.
#   2. Importa el flujo NutriGuide IA (se actualiza en cada arranque desde el repositorio).
#   3. Lo publica para que sus webhooks de producción queden activos.
#   4. Arranca n8n.
set -e

CREDENCIALES=/tmp/nutriguide-credenciales.json
FLUJO=/opt/nutriguide/nutriguide-ia.json
ID_FLUJO=nutriguideIA0001

echo "[NutriGuide] Generando credenciales..."
node /opt/nutriguide/setup/credenciales.js > "$CREDENCIALES"
n8n import:credentials --input="$CREDENCIALES"
rm -f "$CREDENCIALES"

echo "[NutriGuide] Importando el flujo $FLUJO..."
n8n import:workflow --input="$FLUJO"

echo "[NutriGuide] Publicando el flujo $ID_FLUJO..."
n8n publish:workflow --id="$ID_FLUJO"

if [ -z "$GEMINI_API_KEY" ]; then
  echo "[NutriGuide] AVISO: GEMINI_API_KEY está vacía; el chat y la indexación fallarán hasta configurarla."
fi

exec n8n start
