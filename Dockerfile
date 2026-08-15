FROM eclipse-temurin:21-jdk-jammy

# Install Python 3, pip, and supervisor
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    python3-pip \
    supervisor \
    curl \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Install Python dependencies
COPY requirements.txt ./
RUN pip3 install --no-cache-dir -r requirements.txt

# Copy project files
COPY . .

# Grant execute permission to Gradle wrapper and build the project
RUN chmod +x ./gradlew && ./gradlew build -x test --no-daemon

# Copy supervisor config
COPY supervisord.conf /etc/supervisor/conf.d/supervisord.conf

EXPOSE 8085 10000

# Start both Java Bot and Python Humo Listener 24/7
CMD ["/usr/bin/supervisord", "-c", "/etc/supervisor/conf.d/supervisord.conf"]
