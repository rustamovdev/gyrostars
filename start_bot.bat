@echo off
title GyroStars Telegram Bot & WebApp
echo =========================================================
echo   GyroStars Spring Boot Bot & WebApp
echo   Port: 8085
echo =========================================================
set "JAVA_HOME=C:\Users\User\.jdks\jdk-21"
set "PATH=%JAVA_HOME%\bin;%PATH%"

call .\gradlew.bat bootRun
pause
