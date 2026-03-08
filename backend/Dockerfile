# ─────────────────────────────────────────────
# Etapa 1: Build con Maven + JDK 21
# ─────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copiar pom y descargar dependencias primero (aprovecha cache de Docker)
COPY pom.xml .
RUN mvn dependency:go-offline -q

# Copiar fuentes y compilar (skip tests para evitar levantar BD en build)
COPY src ./src
RUN mvn package -DskipTests -q

# ─────────────────────────────────────────────
# Etapa 2: Runtime con JRE 21 slim
# ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear directorio para certificados generados
RUN mkdir -p /app/certs

# Copiar el JAR desde la etapa de build
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
