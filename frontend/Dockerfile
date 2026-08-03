# =========================================================================
# Frontend — React + Vite servido por Nginx
# Build multi-etapa: Node compila los estaticos y la imagen final solo
# lleva Nginx con los archivos ya generados. Node no viaja a produccion.
# =========================================================================

# ---------- Etapa 1: build ----------
FROM node:20-alpine AS build

WORKDIR /build

# package.json y el lock primero: mientras las dependencias no cambien,
# Docker reutiliza esta capa y no reinstala en cada build.
COPY package.json package-lock.json ./

# 'npm ci' instala exactamente lo fijado en el lock, a diferencia de
# 'npm install', que puede resolver versiones distintas.
RUN npm ci

COPY . .

# VITE_API_URL se resuelve en tiempo de BUILD, no de ejecucion: Vite la
# incrusta en el bundle. Por eso llega como argumento de build y cada
# entorno necesita su propia imagen.
# Recordatorio: todo lo que empiece por VITE_ queda visible en el navegador.
# Nunca pases secretos por aqui.
ARG VITE_API_URL=/api
ENV VITE_API_URL=$VITE_API_URL

RUN npm run build

# ---------- Etapa 2: servidor ----------
FROM nginx:1.27-alpine

# Configuracion propia: enrutado SPA y cabeceras de seguridad.
COPY nginx.conf /etc/nginx/conf.d/default.conf

COPY --from=build /build/dist /usr/share/nginx/html

# Nginx corre como usuario sin privilegios. Por eso escucha en el 8080 y
# no en el 80: los puertos por debajo de 1024 exigen root.
RUN touch /var/run/nginx.pid \
    && chown -R nginx:nginx /var/run/nginx.pid /var/cache/nginx /usr/share/nginx/html

USER nginx

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
