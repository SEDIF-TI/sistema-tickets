# ETAPA 1: Construcción (Build)
FROM node:20-alpine AS builder

WORKDIR /app

# Copiamos solo los archivos de dependencias primero para optimizar la caché de Docker
COPY package.json package-lock.json ./
RUN npm install

# Copiamos el resto del código del frontend y construimos el proyecto
COPY . .
RUN npm run build

# ETAPA 2: Producción (Servidor Nginx)
FROM nginx:alpine

# Copiamos nuestra configuración personalizada de Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Copiamos los archivos compilados de la Etapa 1 a Nginx
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]