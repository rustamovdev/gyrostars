FROM eclipse-temurin:21-jdk-jammy

# Install Python 3, pip, dos2unix and curl
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    curl \
    dos2unix \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies
COPY requirements.txt ./
RUN pip3 install --no-cache-dir -r requirements.txt

# Copy project files
COPY . .

# Convert line endings to LF, ensure data folder exists, grant execute permissions and build
RUN mkdir -p /app/data \
    && chmod -R 777 /app/data \
    && dos2unix /app/entrypoint.sh /app/gradlew \
    && chmod +x /app/gradlew /app/entrypoint.sh \
    && ./gradlew build -x test --no-daemon

EXPOSE 8085 10000

# Start both Java Bot and Python Humo Listener directly
CMD ["/bin/bash", "/app/entrypoint.sh"]
