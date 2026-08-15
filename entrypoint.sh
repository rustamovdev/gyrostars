#!/bin/bash
set -e

echo "=========================================================="
echo "🚀 [1/2] Humo Payment Listener ishga tushirilmoqda..."
echo "=========================================================="
python3 /app/payment_listener.py &

echo "=========================================================="
echo "🚀 [2/2] Spring Boot Java Bot ishga tushirilmoqda..."
PORT="${PORT:-10000}"
export SERVER_PORT="$PORT"
export PORT="$PORT"

JAR_PATH=$(find /app/build/libs/ -name "*.jar" ! -name "*-plain.jar" | head -n 1)
echo "🌐 Server PORT: $PORT"
echo "📦 Executable JAR: $JAR_PATH"
echo "=========================================================="

exec java -Xmx400m -Dserver.port="$PORT" -Dserver.address="0.0.0.0" -jar "$JAR_PATH"
