@echo off
REM HireSync Development Environment CLI for Windows
REM This batch file provides easy access to HireSync commands on Windows

setlocal enabledelayedexpansion

REM Get the directory where the batch file is located
set "SCRIPT_DIR=%~dp0"
set "DOCKER_COMPOSE_FILE=%SCRIPT_DIR%docker\docker-compose.local.yaml"

if "%1"=="" (
    call :show_help
    goto :end
)

if "%1"=="help" (
    call :show_help
    goto :end
)

if "%1"=="start" (
    echo Starting HireSync local development environment...
    call :ensure_bash_available
    bash "%SCRIPT_DIR%scripts\run-local.sh"
    goto :end
)

if "%1"=="stop" (
    echo Stopping HireSync local development environment...
    call :ensure_bash_available
    bash "%SCRIPT_DIR%scripts\stop-local.sh"
    goto :end
)

if "%1"=="restart" (
    echo Restarting HireSync local development environment...
    call :ensure_bash_available
    bash "%SCRIPT_DIR%scripts\stop-local.sh"
    timeout /t 3 /nobreak > nul
    bash "%SCRIPT_DIR%scripts\run-local.sh"
    goto :end
)

if "%1"=="status" (
    echo Checking HireSync status...
    docker ps --filter "name=hiresync"
    
    REM Check if the application port is in use
    set "APP_PORT=8080"
    if defined APP_PORT (
        echo.
        echo Application Port Status:
        netstat -an | findstr /C:":%APP_PORT% "
        if errorlevel 1 (
            echo No process is using port %APP_PORT%
        )
    )
    
    echo.
    echo Docker Volumes:
    docker volume ls --filter "name=hiresync"
    goto :end
)

if "%1"=="logs" (
    echo Viewing PostgreSQL logs...
    docker logs -f hiresync-postgres
    goto :end
)

if "%1"=="devlogs" (
    echo Viewing Development Container logs...
    docker logs -f hiresync-devtools
    goto :end
)

if "%1"=="psql" (
    echo Opening PostgreSQL terminal...
    
    REM Check if PostgreSQL container is running
    docker ps --filter "name=hiresync-postgres" | findstr "hiresync-postgres" > nul
    if errorlevel 1 (
        echo Error: PostgreSQL container is not running.
        goto :end
    )
    
    REM Get database credentials from .env file
    set "DB_USER=hiresync"
    set "DB_NAME=hiresync"
    if exist "%SCRIPT_DIR%.env" (
        for /f "tokens=1,2 delims==" %%a in ('type "%SCRIPT_DIR%.env" ^| findstr /v "^#" ^| findstr /R "^DB_USER= ^DB_NAME="') do (
            set "%%a=%%b"
        )
    )
    
    docker exec -it hiresync-postgres psql -U "%DB_USER%" -d "%DB_NAME%"
    goto :end
)

if "%1"=="clean" (
    echo WARNING: This will remove all data from your development environment!
    echo All database data will be lost!
    
    set /p confirm="Are you sure you want to continue? (y/N) "
    if /i "%confirm%"=="y" (
        echo Stopping containers...
        call :ensure_bash_available
        bash "%SCRIPT_DIR%scripts\stop-local.sh"
        
        echo Removing PostgreSQL data volume...
        docker volume rm hiresync-postgres-data
        
        echo Environment cleaned successfully
    ) else (
        echo Operation cancelled
    )
    goto :end
)

echo Unknown command: %1
call :show_help
exit /b 1

:show_help
echo HireSync Development Environment CLI
echo Usage: hiresync.cmd ^<command^>
echo.
echo Available commands:
echo   start       Start local development environment
echo   stop        Stop local development environment
echo   restart     Restart local development environment
echo   status      Check status of components
echo   logs        View PostgreSQL logs
echo   devlogs     View Development Tools logs
echo   psql        Open PostgreSQL terminal
echo   clean       Remove all data (WARNING: destructive)
echo   help        Show this help message
echo.
echo Examples:
echo   hiresync.cmd start        Start the development environment
echo   hiresync.cmd stop         Stop the development environment
exit /b 0

:ensure_bash_available
where bash >nul 2>&1
if errorlevel 1 (
    echo Error: bash is not available in your PATH.
    echo Please install Git for Windows or WSL to get bash.
    exit /b 1
)
exit /b 0

:end
exit /b %ERRORLEVEL% 