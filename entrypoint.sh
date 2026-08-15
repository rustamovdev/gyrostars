#!/bin/bash
set -e

echo "=========================================================="
echo "🚀 [1/2] Humo Payment Listener ishga tushirilmoqda..."
echo "=========================================================="
python3 /app/payment_listener.py &

echo "=========================================================="
echo "🚀 [2/2] Spring Boot Java Bot ishga tushirilmoqda..."
echo "PORT: ${PORT:-8085}"
echo "=========================================================="
PORT=${PORT:-8085}
exec java -Xmx400m -Dserver.port=$PORT -Dserver.address=0.0.0.0 -jar /app/build/libs/LeykaBot-1.0-SNAPSHOT.jar
