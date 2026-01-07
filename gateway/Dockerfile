# Argument for JAR file version (defaults to snapshot)
ARG JAR_FILENAME=gateway-0.0.1-SNAPSHOT.jar

# === STAGE 1: BUILD (Builds from source) ===
FROM eclipse-temurin:25-jdk-noble AS builder

# Set the work directory to the final application root
WORKDIR /app

# Copy Maven wrapper and configuration
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# Copy source and build (skipping tests for speed)
COPY src ./src
RUN ./mvnw package -DskipTests

# === STAGE 2: CI RUNTIME (Optimized for CI/CD) ===
# Expects a pre-built JAR from CI artifacts (matches backend-ci pattern)
# Usage: docker build --target ci ...
FROM gcr.io/distroless/java25-debian13:latest AS ci
WORKDIR /app
ENV TZ=UTC LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxDirectMemorySize=128m" \
    SPRING_PROFILES_ACTIVE=default
EXPOSE 8080

# Copy pre-built JAR (expects context to have target/)
COPY target/*.jar /app/app.jar

USER nonroot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

# === STAGE 3: PRODUCTION RUNTIME (Default) ===
# Default stage for "docker compose up --build"
FROM gcr.io/distroless/java25-debian13:latest AS production
WORKDIR /app
ENV TZ=UTC LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxDirectMemorySize=128m" \
    SPRING_PROFILES_ACTIVE=default
EXPOSE 8080
# Copy JAR from builder stage
COPY --from=builder /app/target/*.jar /app/app.jar
USER nonroot
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
