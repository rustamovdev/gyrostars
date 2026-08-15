FROM eclipse-temurin:21-jdk-jammy

# Install Python 3 and pip
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies
COPY requirements.txt ./
RUN pip3 install --no-cache-dir -r requirements.txt

# Copy project files
COPY . .

# Grant execute permissions and build the project
RUN chmod +x ./gradlew entrypoint.sh && ./gradlew build -x test --no-daemon

EXPOSE 8085 10000

# Start both Java Bot and Python Humo Listener directly
CMD ["/bin/bash", "/app/entrypoint.sh"]
