#!/bin/bash
set -e

mkdir -p /app/data ./data
chmod -R 777 /app/data ./data 2>/dev/null || true

echo "=========================================================="
echo "🚀 [1/2] Humo Payment Listener ishga tushirilmoqda..."
echo "=========================================================="
if [ -f "/app/payment_listener.py" ]; then
    python3 /app/payment_listener.py &
elif [ -f "./payment_listener.py" ]; then
    python3 ./payment_listener.py &
fi

echo "=========================================================="
echo "🚀 [2/2] Spring Boot Java Bot ishga tushirilmoqda..."
PORT="${PORT:-10000}"
export SERVER_PORT="$PORT"
export PORT="$PORT"

if [ -f "/app/build/libs/LeykaBot-1.0-SNAPSHOT.jar" ]; then
    JAR_PATH="/app/build/libs/LeykaBot-1.0-SNAPSHOT.jar"
elif [ -f "./build/libs/LeykaBot-1.0-SNAPSHOT.jar" ]; then
    JAR_PATH="./build/libs/LeykaBot-1.0-SNAPSHOT.jar"
else
    JAR_PATH=$(find /app/build/libs ./build/libs -name "*.jar" ! -name "*-plain.jar" 2>/dev/null | head -n 1)
fi

echo "🌐 Server PORT: $PORT"
echo "📦 Executable JAR: $JAR_PATH"
echo "=========================================================="

exec java -Xmx320m -Xms128m -XX:+UseSerialGC -Dserver.port="$PORT" -Dserver.address="0.0.0.0" -jar "$JAR_PATH"
