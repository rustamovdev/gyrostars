# -------------------------------------------------------------
# Stage 1: Build Java application with official Gradle image
# -------------------------------------------------------------
FROM gradle:8.12-jdk21 AS builder
WORKDIR /app

# Copy gradle config files and dependencies
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY src ./src

# Build production jar - capture errors explicitly
RUN gradle compileJava --no-daemon 2>&1 | tee /tmp/build_out.txt; \
    if grep -q "error:" /tmp/build_out.txt; then \
        echo "=== COMPILATION ERRORS ==="; \
        grep -A3 "error:" /tmp/build_out.txt; \
        exit 1; \
    fi
RUN gradle bootJar -x test --no-daemon

# -------------------------------------------------------------
# Stage 2: Production Runtime image (Ultra Fast & Lightweight)
# -------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install Python 3, pip, curl, dos2unix
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    curl \
    dos2unix \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY requirements.txt ./
RUN pip3 install --no-cache-dir -r requirements.txt --break-system-packages 2>/dev/null || pip3 install --no-cache-dir -r requirements.txt

# Copy built production JAR from builder stage
COPY --from=builder /app/build/libs/LeykaBot-1.0-SNAPSHOT.jar /app/app.jar

# Copy runtime scripts, data directory, and session
COPY payment_listener.py entrypoint.sh ./
COPY humo_payment_session.session* ./
COPY data ./data

# Ensure data directory has full permissions and convert line endings
RUN mkdir -p /app/data && chmod -R 777 /app/data \
    && dos2unix /app/entrypoint.sh \
    && chmod +x /app/entrypoint.sh

EXPOSE 8085 10000

ENV PORT=10000
ENV SERVER_PORT=10000

CMD ["/bin/bash", "/app/entrypoint.sh"]
