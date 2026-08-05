#!/usr/bin/env bash
#
# Arranca el frontend en modo desarrollo.
#
#   ./scripts/dev-frontend.sh
#
# Existe por simetria con dev-backend.sh y para avisar de las dos cosas que se
# olvidan: instalar las dependencias la primera vez, y que el backend tiene que
# estar arriba o la pantalla se queda sin datos.

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$RAIZ/frontend"

if [ ! -d node_modules ]; then
    echo "Instalando dependencias (solo la primera vez)…"
    npm install
fi

if ! curl -s -o /dev/null --max-time 2 -I http://localhost:8080/api/salud; then
    echo "Aviso: el backend no responde en el puerto 8080." >&2
    echo "       Levantalo con ./scripts/dev-backend.sh o la interfaz saldra sin datos." >&2
    echo
fi

exec npm run dev
