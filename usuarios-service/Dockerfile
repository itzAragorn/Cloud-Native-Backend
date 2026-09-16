# Build stage
FROM maven:3.8.8-eclipse-temurin-17 as builder

WORKDIR /build

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copiar JAR compilado del stage anterior
COPY --from=builder /build/target/*.jar app.jar

# Exponer puerto
EXPOSE 8083

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# Comando de inicio
ENTRYPOINT ["java", "-Xmx512m", "-jar", "app.jar"]
