# Assuming the final built JAR will be named: demo-0.0.1-SNAPSHOT.jar
ARG JAR_FILENAME=demo-0.0.1-SNAPSHOT.jar

# === STAGE 1: BUILD (Same JDK is needed for compilation) ===
FROM eclipse-temurin:21-jdk-jammy AS builder

# Set the work directory to the final application root
WORKDIR /app

# 1. Copy POM files and wrappers (context-relative to backend/)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Fetch dependencies (layer cache optimization)
RUN ./mvnw dependency:go-offline

# 2. Copy the source code
COPY src src/

# 3. Build the application (creates target/demo-0.0.1-SNAPSHOT.jar)
RUN ./mvnw package -DskipTests -DskipUTs=true -DskipITs=true


# === STAGE 2: RUNTIME (Distroless Production Image) ===
# Using the Distroless image ensures only the JRE and minimal libraries are included.
# This drastically reduces CVE count and eliminates the shell/package manager.
FROM gcr.io/distroless/java21-debian12:latest AS final

# --- SECURITY & UTILITIES (AS NON-ROOT BY DEFAULT) ---
# NOTE: No need to install wget or create a user. Distroless runs as 'nonroot'.
# The only files it includes are the minimum necessary JRE components.

# Set WORKDIR to the application root
WORKDIR /app

# Environment variables for performance, memory, and Spring profile
ENV TZ=UTC LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxDirectMemorySize=128m" \
    SPRING_PROFILES_ACTIVE=default

EXPOSE 8080

# Copy the built JAR from the builder stage
# The JAR is now located at /app/target/ in the builder stage.
COPY --from=builder /app/target/*.jar /app/app.jar

# Entrypoint: Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]