#!/bin/bash
set -e

mkdir -p /app/data ./data
chmod -R 777 /app/data ./data 2>/dev/null || true

PORT="${PORT:-10000}"
export SERVER_PORT="$PORT"
export PORT="$PORT"

echo "=========================================================="
echo "🚀 [1/2] Humo Payment Listener ishga tushirilmoqda..."
echo "=========================================================="
if [ -f "/app/payment_listener.py" ]; then
    python3 /app/payment_listener.py &
elif [ -f "./payment_listener.py" ]; then
    python3 ./payment_listener.py &
fi

echo "=========================================================="
echo "🚀 [2/2] Spring Boot Java Bot & WebApp ishga tushirilmoqda..."
echo "🌐 Server PORT: $PORT"
echo "=========================================================="

if [ -f "/app/app.jar" ]; then
    JAR_PATH="/app/app.jar"
elif [ -f "/app/build/libs/LeykaBot-1.0-SNAPSHOT.jar" ]; then
    JAR_PATH="/app/build/libs/LeykaBot-1.0-SNAPSHOT.jar"
else
    JAR_PATH=$(find /app -name "*.jar" ! -name "*-plain.jar" 2>/dev/null | head -n 1)
fi

echo "📦 Executable JAR: $JAR_PATH"
echo "=========================================================="

exec java -Xmx350m -Xms128m -XX:+UseG1GC -Dserver.port="$PORT" -Dserver.address="0.0.0.0" -jar "$JAR_PATH"
