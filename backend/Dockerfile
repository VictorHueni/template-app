# syntax=docker/dockerfile:1.7

######## Build ########
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

######## Runtime ########
FROM eclipse-temurin:25-jre-alpine

ENV TZ=UTC LANG=C.UTF-8 \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:InitialRAMPercentage=50 -XX:+UseG1GC -XX:MaxDirectMemorySize=128m" \
    SPRING_PROFILES_ACTIVE=default

WORKDIR /app

# Tools for healthcheck (install before dropping root)
RUN apk add --no-cache wget ca-certificates

# Non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy fat jar (consider setting <finalName>app</finalName> in pom.xml)
COPY --from=build /workspace/target/*.jar /app/app.jar

# Healthcheck (requires spring-boot-starter-actuator)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
