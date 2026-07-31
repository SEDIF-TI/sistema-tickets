# ETAPA 1: Construcción (Build)
# Usamos una imagen oficial de Maven con Java 17 (ajusta la versión si usas Java 21)
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# Copiamos el archivo pom.xml primero
# Esto le permite a Docker guardar las dependencias en caché y no volver a descargarlas cada vez que cambies tu código
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiamos el resto del código fuente (la carpeta src)
COPY src ./src

# Compilamos el proyecto generando el archivo .jar (saltamos los tests para acelerar el despliegue)
RUN mvn clean package -DskipTests

# ETAPA 2: Producción (Ejecución)
# Usamos una imagen mucho más ligera de Java (solo el JRE, sin el compilador)
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiamos el archivo .jar generado en la Etapa 1 hacia nuestro contenedor final
COPY --from=builder /app/target/*.jar app.jar

# Exponemos el puerto que usa Spring Boot por defecto
EXPOSE 8080

# Comando que encenderá el servidor backend
ENTRYPOINT ["java", "-jar", "app.jar"]