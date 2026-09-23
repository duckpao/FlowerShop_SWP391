
@echo off
title FlowerShop Backend

cd /d "%~dp0"

echo Starting MySQL...
docker compose -f ..\docker-compose.yml up -d

if errorlevel 1 (
    echo Docker failed to start.
    pause
    exit /b 1
)

echo Starting Spring Boot in VS Code Terminal...

start "" /b cmd /c "timeout /t 10 /nobreak >nul & start http://localhost:8080/swagger-ui/index.html"

call gradlew.bat bootRun