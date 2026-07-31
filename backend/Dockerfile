# ETAPA 1: Construcción (Build)
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Aprovechar la caché de dependencias de Maven
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# ==========================================
# ETAPA 2: Producción (Ejecución)
# ==========================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 1. Instalar dependencias de zona horaria y configurar la hora local
# Esto evita que los tickets y resguardos se guarden con hora UTC (desfasada)
RUN apk add --no-cache tzdata
ENV TZ="America/Mexico_City"

# 2. Crear un usuario y grupo sin privilegios de root por seguridad
RUN addgroup -S spring && adduser -S spring -G spring

# 3. Cambiar al usuario seguro que acabamos de crear
USER spring:spring

# Copiar el .jar compilado desde la Etapa 1
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# Comando de arranque (usamos variables de entorno para mayor flexibilidad)
ENTRYPOINT ["java", "-jar", "app.jar"]