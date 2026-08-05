#!/usr/bin/env bash
#
# Arranca el backend en modo desarrollo, contra la base de datos en Docker.
#
#   ./scripts/dev-backend.sh
#
# Existe porque arrancar a mano exige tres cosas que es facil olvidar:
#
#   1. JAVA_HOME apuntando a un JDK 17 real. El enlace `current` de SDKMAN
#      puede quedar apuntando a una version desinstalada, y entonces el
#      wrapper de Maven falla con "JAVA_HOME is not defined correctly" sin
#      decir por que.
#   2. Las variables del archivo .env. Sin ellas el arranque muere con
#      "Could not resolve placeholder 'APP_JWT_SECRET'", que tampoco explica
#      que falta cargar el archivo.
#   3. El host y el puerto de la base: dentro de Docker es `postgres-db:5432`,
#      pero desde el equipo se llega por `localhost:5433`.

set -euo pipefail

RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

# --- 1. JDK ---------------------------------------------------------------
# Se busca un JDK 17; si `current` esta roto, se ignora.
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
    JAVA_HOME="$(find "$HOME/.sdkman/candidates/java" -maxdepth 1 -name '17*' -type d 2>/dev/null | sort | tail -1)"
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME}/bin/java" ]; then
    echo "No se encontro un JDK 17. Instalalo con:  sdk install java 17.0.10-tem" >&2
    exit 1
fi
export JAVA_HOME

# --- 2. Variables de entorno ----------------------------------------------
if [ ! -f "$RAIZ/.env" ]; then
    echo "Falta el archivo .env en $RAIZ" >&2
    echo "Copialo del ejemplo:  cp .env.example .env" >&2
    exit 1
fi

# `set -a` exporta todo lo que se defina a continuacion. Se desactiva justo
# despues para no exportar el resto de variables del script.
set -a
# shellcheck disable=SC1091
. "$RAIZ/.env"
set +a

# --- 3. Conexion a la base ------------------------------------------------
# El .env trae la configuracion de Docker, donde el backend y la base comparten
# red. Al ejecutar el backend fuera del contenedor hay que apuntar al puerto
# publicado en el equipo.
export DB_HOST=localhost
export DB_PORT=5433

# --- Comprobacion previa --------------------------------------------------
if ! docker ps --filter name=sistema_tickets_db --filter status=running --format '{{.Names}}' | grep -q .; then
    echo "La base de datos no esta corriendo. Levantala con:" >&2
    echo "  docker compose -f docker-compose.yml -f docker-compose.local.yml up -d postgres-db" >&2
    exit 1
fi

echo "JDK      : $JAVA_HOME"
echo "Base     : $DB_HOST:$DB_PORT/$POSTGRES_DB"
echo "Telegram : ${TELEGRAM_BOT_ENABLED:-false}"
echo

cd "$RAIZ/backend"
exec sh mvnw spring-boot:run
