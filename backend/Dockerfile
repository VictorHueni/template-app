# Assuming the final built JAR will be named: demo-0.0.1-SNAPSHOT.jar
ARG JAR_FILENAME=demo-0.0.1-SNAPSHOT.jar

# === STAGE 1: BUILD (Same JDK is needed for compilation) ===
FROM eclipse-temurin:25-jdk-noble AS builder

# Set the work directory to the final application root
WORKDIR /app

# 1. Copy the API definition (This is what was missing!)
# This puts it at /app/api, so it sits "next" to backend
COPY api api

# 2. Copy the Maven Wrapper and POM (from the backend folder context)
# We place them into /app/backend/
COPY backend/pom.xml backend/pom.xml
COPY backend/mvnw backend/mvnw
COPY backend/.mvn backend/.mvn

# 3. Set WORKDIR to backend so Maven runs from the correct place
WORKDIR /app/backend

# 4. Resolve dependencies
# (We made sure mvnw is executable just in case permissions got lost)
RUN chmod +x mvnw && ./mvnw dependency:go-offline

# 5. Copy the source code
COPY backend/src src

# 6. Build the application
# Skip test compilation and execution for faster Docker builds (tests run in CI)
RUN ./mvnw package -DskipTests -DskipUTs=true -DskipITs=true -Djacoco.skip=true


# === STAGE 2: CI RUNTIME (Optimized for CI/CD pipelines) ===
# This stage uses a pre-built JAR artifact from the CI pipeline (backend_pipeline job)
# Skips compilation entirely, making Docker builds ~3-6 minutes faster
# Usage in CI: docker build --target ci -t demo-backend:ci .
FROM gcr.io/distroless/java25-debian13:latest AS ci

WORKDIR /app

ENV TZ=UTC LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxDirectMemorySize=128m" \
    SPRING_PROFILES_ACTIVE=default

EXPOSE 8080 8081

# Copy pre-built JAR from CI artifact (expects backend/target/*.jar to exist)
COPY backend/target/*.jar /app/app.jar

ENTRYPOINT ["java","-jar","/app/app.jar"]


# === STAGE 3: PRODUCTION RUNTIME (Full build for local development) ===
# Using the Distroless image ensures only the JRE and minimal libraries are included.
# This drastically reduces CVE count and eliminates the shell/package manager.
# This is the default stage when building without --target flag
# Usage locally: docker build -t demo-backend:latest .
FROM gcr.io/distroless/java25-debian13:latest AS production

# --- SECURITY & UTILITIES (AS NON-ROOT BY DEFAULT) ---
# NOTE: No need to install wget or create a user. Distroless runs as 'nonroot'.
# The only files it includes are the minimum necessary JRE components.

# Set WORKDIR to the application root
WORKDIR /app

# Environment variables for performance, memory, and Spring profile
ENV TZ=UTC LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxDirectMemorySize=128m" \
    SPRING_PROFILES_ACTIVE=default

# Expose both application port (8080) and management/actuator port (8081)
EXPOSE 8080 8081

# Copy the built JAR from the builder stage
# Note: The path is now /app/backend/target because of the structure change in Stage 1
COPY --from=builder /app/backend/target/*.jar /app/app.jar

# Entrypoint: Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]