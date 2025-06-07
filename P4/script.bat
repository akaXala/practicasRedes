@echo off
TITLE Test de Carga para Servidor Java

ECHO.
ECHO =================================================================
ECHO   Script para probar la concurrencia del servidor Java
ECHO =================================================================
ECHO.
ECHO Este script lanzara 4 peticiones simultaneas a http://localhost:8080
ECHO Asegurate de que tu servidor Java este corriendo en otra ventana.
ECHO.
PAUSE
ECHO.
ECHO Lanzando peticiones...
ECHO.

:: El comando START /B ejecuta los procesos en segundo plano de forma concurrente
START /B curl "http://localhost:8080/?file_path=archivos&file_name=prueba"
START /B curl "http://localhost:8080/?file_path=archivos&file_name=prueba"
START /B curl "http://localhost:8080/?file_path=archivos&file_name=prueba"
START /B curl "http://localhost:8080/?file_path=archivos&file_name=prueba"

ECHO.
ECHO Listo! Las 4 peticiones fueron enviadas casi al mismo tiempo.
ECHO.
PAUSE