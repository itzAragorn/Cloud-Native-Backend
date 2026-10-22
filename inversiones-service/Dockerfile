# Multi-stage build for inversiones-service
# Stage 1: Build with Maven and Java 17
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /build

# Copy pom.xml and download dependencies first (layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -DskipTests

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime with JRE 17 - Multi-architecture (ARM64 + AMD64)
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install curl for healthchecks
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Copy only the JAR from builder stage
COPY --from=builder /build/target/inversiones-service-*.jar inversiones-service.jar

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "inversiones-service.jar"]
