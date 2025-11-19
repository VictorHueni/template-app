# Assuming the final built JAR will be named: demo-0.0.1-SNAPSHOT.jar
ARG JAR_FILENAME=demo-0.0.1-SNAPSHOT.jar

# === STAGE 1: BUILD ===
FROM eclipse-temurin:25-jdk-jammy AS builder

# Set the work directory to the final application root
WORKDIR /app

# 1. Copy POM files and wrappers for dependency caching
# SOURCE PATHS ARE NOW RELATIVE TO THE BACKEND DIRECTORY (THE BUILD CONTEXT)
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

# Fetch dependencies (layer cache optimization)
# The POM is now directly in the WORKDIR, so no need for -f
RUN ./mvnw dependency:go-offline

# 2. Copy the source code
# Copy contents of src/ to src/ in the WORKDIR
COPY src src/

# 3. Build the application
# Run package from the WORKDIR, finds pom.xml and src/ correctly
RUN ./mvnw package -DskipTests


# === STAGE 2: RUNTIME (Production Image) ===
FROM eclipse-temurin:25-jre-jammy AS final

# --- SECURITY & UTILITIES (AS ROOT) ---
# 1. Install utilities (wget) needed for the healthcheck, using apt (Debian/Ubuntu)
RUN apt-get update \
    && apt-get install -y wget ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# 2. Create and switch to a dedicated non-root user
RUN groupadd --system appuser && useradd --system --gid appuser appuser
USER appuser

# --- APPLICATION SETUP (AS NON-ROOT) ---
WORKDIR /app

# Environment variables
ENV TZ=UTC LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxDirectMemorySize=128m" \
    SPRING_PROFILES_ACTIVE=default

EXPOSE 8080

# Copy the built JAR from the builder stage
# The JAR is now located at /app/target/ in the builder stage.
COPY --from=builder /app/target/${JAR_FILENAME} /app/app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

# Entrypoint: Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]