@echo off
title GyroStars Runner
echo =========================================================
echo   Starting GyroStars Bot and Payment Listener...
echo =========================================================

start "GyroStars Bot" cmd /k "set JAVA_HOME=C:\Users\User\.jdks\jdk-21&& set PATH=C:\Users\User\.jdks\jdk-21\bin;%PATH%&& gradlew.bat bootRun"
timeout /t 5
start "Humo Payment Listener" cmd /k "python payment_listener.py"
