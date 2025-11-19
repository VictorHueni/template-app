# Assuming the final built JAR will be named: demo-0.0.1-SNAPSHOT.jar
ARG JAR_FILENAME=demo-0.0.1-SNAPSHOT.jar

# === STAGE 1: BUILD ===
FROM eclipse-temurin:25-jdk-jammy AS builder

# Set the context root for Maven
WORKDIR /app

# Copy wrapper files from project root
COPY mvnw .
COPY .mvn .mvn

# Copy backend pom and source code to their correct relative locations
COPY backend/pom.xml backend/
COPY backend/src backend/src/

# Fetch all dependencies
RUN ./mvnw dependency:go-offline -f backend/pom.xml

# Build the application
# Use -f to specify the pom location and skip tests
RUN ./mvnw package -DskipTests -f backend/pom.xml


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

# Environment variables for performance and timezone
ENV TZ=UTC LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxDirectMemorySize=128m" \
    SPRING_PROFILES_ACTIVE=default

EXPOSE 8080

# Copy the built JAR from the builder stage
# Uses the standard path resulting from the Maven build
COPY --from=builder /app/backend/target/${JAR_FILENAME} /app/app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

# Entrypoint: Run the application
ENTRYPOINT ["java","-jar","/app/app.jar"]