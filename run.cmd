@echo off
setlocal enabledelayedexpansion

REM HireSync Application Manager
REM A unified interface for all operations related to the HireSync application

REM Get script directory for reliable sourcing
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR:~0,-1%"

REM Color definitions
set "RED=\033[0;31m"
set "GREEN=\033[0;32m"
set "YELLOW=\033[0;33m"
set "BLUE=\033[0;34m"
set "NC=\033[0m"

REM Print header
echo ========================================
echo HireSync Application Manager
echo ========================================

REM Check if Docker is installed and running
:check_docker
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: Docker is not running or not installed.
    exit /b 1
)
exit /b 0

REM Load environment variables
:load_env
if exist "%PROJECT_ROOT%\.env" (
    echo Loading environment variables from .env file...
    for /f "tokens=*" %%a in (%PROJECT_ROOT%\.env) do (
        set "%%a"
    )
) else (
    echo Warning: .env file not found, using default values.
    echo Consider creating an .env file from .env.example
)
exit /b 0

REM Command handlers
:handle_build
call :load_env
set "args=%*"
set "args=!args:*build =!"

if "!args!" == "!args:--docker=!" (
    REM No docker flag
    set USE_DOCKER=false
) else (
    call :check_docker
    if !ERRORLEVEL! NEQ 0 (
        echo Warning: Docker is required for Docker builds.
        echo Proceeding with standard build...
    )
    set USE_DOCKER=true
)

REM Pass the Docker directory to the build script
set "DOCKER_DIR=%PROJECT_ROOT%\docker"
bash "%SCRIPT_DIR%scripts\build\docker-build.sh" %args%
exit /b %ERRORLEVEL%

:handle_clean
set "args=%*"
set "args=!args:*clean =!"
echo Cleaning build artifacts...
call .\mvnw clean %args%

if not "!args!" == "!args:--docker=!" (
    call :check_docker
    if !ERRORLEVEL! EQU 0 (
        echo Cleaning Docker artifacts...
        docker system prune -f
    ) else (
        echo Warning: Docker is not available, skipping Docker cleanup.
    )
)

if exist "%SCRIPT_DIR%logs" (
    echo Cleaning logs directory...
    del /Q "%SCRIPT_DIR%logs\*"
)

echo Clean completed successfully.
exit /b 0

:handle_deploy
call :load_env
set "args=%*"
set "args=!args:*deploy =!"

if not "!args!" == "!args:--docker=!" (
    call :check_docker
    if !ERRORLEVEL! NEQ 0 (
        echo Error: Docker is required for Docker deployments.
        exit /b 1
    )
)

REM Pass the Docker directory to the deploy script
set "DOCKER_DIR=%PROJECT_ROOT%\docker"
bash "%SCRIPT_DIR%scripts\deploy\prod-deploy.sh" %args%
exit /b %ERRORLEVEL%

:handle_dev
call :load_env
set "args=%*"
set "args=!args:*dev =!"

REM Pass the Docker directory to the dev script
set "DOCKER_DIR=%PROJECT_ROOT%\docker"
bash "%SCRIPT_DIR%scripts\dev\dev-environment.sh" --mode dev %args%
exit /b %ERRORLEVEL%

:handle_local
call :load_env
set "args=%*"
set "args=!args:*local =!"

call :check_docker
if !ERRORLEVEL! NEQ 0 (
    echo Error: Docker is required for local development.
    exit /b 1
)

REM Pass the Docker directory to the dev environment script
set "DOCKER_DIR=%PROJECT_ROOT%\docker"
bash "%SCRIPT_DIR%scripts\dev\dev-environment.sh" --mode local %args%
exit /b %ERRORLEVEL%

:handle_quality
set "args=%*"
set "args=!args:*quality =!"
bash "%SCRIPT_DIR%scripts\quality\quality-check.sh" %args%
exit /b %ERRORLEVEL%

:handle_verify
set "args=%*"
set "args=!args:*verify =!"
bash "%SCRIPT_DIR%scripts\build\verify.sh" %args%
exit /b %ERRORLEVEL%

:handle_test
set "args=%*"
set "args=!args:*test =!"
echo Running tests with test profile...
call .\mvnw test -Dspring.profiles.active=test %args%
exit /b %ERRORLEVEL%

:handle_test_env
call :load_env
set "args=%*"
set "args=!args:*test-env =!"

REM Pass the Docker directory to the dev environment script
set "DOCKER_DIR=%PROJECT_ROOT%\docker"
bash "%SCRIPT_DIR%scripts\dev\dev-environment.sh" --mode test %args%
exit /b %ERRORLEVEL%

:handle_health
set "args=%*"
set "args=!args:*health =!"
echo Running health check...
bash "%SCRIPT_DIR%scripts\utils\health-check.sh" %args%
exit /b %ERRORLEVEL%

:show_help
echo Usage: run.cmd [command] [options]
echo.
echo Commands:
echo   build     Build the application
echo   clean     Clean build artifacts
echo   deploy    Deploy the application
echo   dev       Start development environment
echo   local     Start local development
echo   quality   Run quality checks
echo   verify    Verify code and build
echo   test      Run tests with test profile
echo   test-env  Start application with test environment
echo   health    Check application health status
echo.
echo Options:
echo   --help       Show this help message
echo   --verbose    Enable verbose output
echo   --debug      Enable debug mode
echo   --docker     Use Docker for the operation
echo   --version=X  Specify version for builds
echo.
echo Examples:
echo   run.cmd build --version=1.0.0
echo   run.cmd deploy --docker
echo   run.cmd dev
echo   run.cmd local
echo   run.cmd quality
echo   run.cmd verify
exit /b 0

REM Main command handler
if "%1" == "" goto :show_help
if "%1" == "--help" goto :show_help
if "%1" == "-h" goto :show_help
if "%1" == "help" goto :show_help

if "%1" == "build" goto :handle_build
if "%1" == "clean" goto :handle_clean
if "%1" == "deploy" goto :handle_deploy
if "%1" == "dev" goto :handle_dev
if "%1" == "local" goto :handle_local
if "%1" == "quality" goto :handle_quality
if "%1" == "verify" goto :handle_verify
if "%1" == "test" goto :handle_test
if "%1" == "test-env" goto :handle_test_env
if "%1" == "health" goto :handle_health

echo Error: Unknown command '%1'
echo Run 'run.cmd --help' for usage information
exit /b 1 