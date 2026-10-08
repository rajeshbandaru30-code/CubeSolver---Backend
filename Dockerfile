# ============================================================
# CubeSolve Backend - Multi-Stage Production Dockerfile
# ============================================================
# Stage 1: Build the Spring Boot fat JAR with Maven + JDK 21
# Stage 2: Run the JAR on a minimal JRE 21 image
#
# IMPORTANT: No secrets, passwords, JWT keys, or .env files
# are embedded. All sensitive config must be passed via
# environment variables at runtime (Render dashboard).
# ============================================================

# ----------------------------------------------------------
# Stage 1 – Build
# ----------------------------------------------------------
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copy Maven wrapper and POM first to leverage Docker layer caching
COPY pom.xml ./

# Copy the full source tree
COPY src ./src

# Build the fat JAR, skipping tests
RUN apt-get update && \
    apt-get install -y --no-install-recommends maven && \
    mvn clean package -DskipTests -B && \
    rm -rf /root/.m2 && \
    apt-get purge -y --auto-remove maven && \
    rm -rf /var/lib/apt/lists/*

# ----------------------------------------------------------
# Stage 2 – Runtime
# ----------------------------------------------------------
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the built JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Render injects PORT as an env var; Spring Boot reads SERVER_PORT
ENV SERVER_PORT=10000

# Expose the port for documentation (Render uses PORT env var)
EXPOSE 10000

# Run as a non-root user for security
RUN groupadd --system appgroup && \
    useradd --system --gid appgroup --no-create-home appuser
USER appuser

# Start the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
