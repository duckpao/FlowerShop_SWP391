
@echo off
title FlowerShop Backend

cd /d "%~dp0"

echo Starting Spring Boot in VS Code Terminal...

start "" /b cmd /c "timeout /t 10 /nobreak >nul & start http://localhost:8080/swagger-ui/index.html"

call gradlew.bat bootRun