@echo off
setlocal enabledelayedexpansion

::==============================================================================
:: HireSync Application Manager
:: Version: 1.0.0
::
:: Description:
::   A unified interface for all operations related to the HireSync application.
::   Manages build, deployment, local development, testing, and utilities.
::
:: Author: HireSync Team
:: License: Proprietary
::==============================================================================

:: Get script directory for reliable sourcing
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR:~0,-1%"

:: Print header
echo ========================================
echo HireSync Application Manager
echo ========================================

goto :main

::==============================================================================
:: Function definitions
::==============================================================================

:: Find bash executable for script running
:find_bash
where bash >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    set "BASH_CMD=bash"
    exit /b 0
)

if exist "C:\Program Files\Git\bin\bash.exe" (
    set "BASH_CMD=C:\Program Files\Git\bin\bash.exe"
    exit /b 0
) else if exist "C:\Program Files (x86)\Git\bin\bash.exe" (
    set "BASH_CMD=C:\Program Files (x86)\Git\bin\bash.exe"
    exit /b 0
) else (
    where wsl >nul 2>&1
    if %ERRORLEVEL% EQU 0 (
        set "BASH_CMD=wsl bash"
        exit /b 0
    )
)

echo Warning: Bash is not available. Some commands may not work.
echo Consider installing Git Bash, WSL, or Cygwin.
exit /b 1

:: Run a bash script with the best available bash
:run_bash_script
set "script_path=%~1"
set "script_args=%~2"

call :find_bash
if %ERRORLEVEL% EQU 0 (
    %BASH_CMD% "%script_path%" %script_args%
    exit /b %ERRORLEVEL%
) else (
    echo Error: Cannot execute bash script. Bash is not available.
    exit /b 1
)

:: Display help information
:show_help
echo Usage: run.cmd [command] [options]
echo.
echo Commands:
echo   build     Build the application
echo   clean     Clean build artifacts
echo   db        Manage the database container (start^|stop^|restart^|status)
echo   deploy    Deploy the application
echo   dev       Start development environment
echo   local     Start local development
echo   quality   Run quality checks
echo   lint      Run auto-fix linting (corrects issues automatically)
echo   verify    Verify code and build
echo   test      Run tests with test profile
echo   test-env  Start application with test environment
echo   health    Check application health status
echo.
echo Options:
echo   --help                 Show this help message
echo   --verbose              Enable verbose output
echo   --debug                Enable debug mode
echo   --docker               Use Docker for the operation
echo   --no-docker            Skip using Docker (for dev/local/test-env)
echo   --use-existing-db      Use existing PostgreSQL database
echo   --skip-db-wait         Skip waiting for PostgreSQL to be ready
echo   --version=X            Specify version for builds
echo.
echo Examples:
echo   run.cmd build --version=1.0.0
echo   run.cmd deploy --docker
echo   run.cmd dev
echo   run.cmd local
echo   run.cmd db start
echo   run.cmd quality
echo   run.cmd lint
echo   run.cmd verify
exit /b

:: Check if Docker is installed and running
:check_docker
docker info >nul 2>&1
if %ERRORLEVEL% NEQ 0 (
    echo Error: Docker is not running or not installed.
    exit /b 1
)
exit /b 0

:: Check if PostgreSQL container is running
:is_postgres_running
docker ps --format "{{.Names}}" | findstr /C:"hiresync-postgres" >nul 2>&1
if %ERRORLEVEL% EQU 0 (
    echo PostgreSQL container is already running.
    exit /b 0
) else (
    exit /b 1
)

:: Load environment variables
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

::==============================================================================
:: Command handlers
::==============================================================================

:: Handle build command
:handle_build
call :load_env
set "args=%*"
set "args=!args:*build =!"

if "!args!" == "!args:--docker=!" (
    set USE_DOCKER=false
) else (
    call :check_docker
    if !ERRORLEVEL! NEQ 0 (
        echo Warning: Docker is required for Docker builds.
        echo Proceeding with standard build...
    )
    set USE_DOCKER=true
)

set "DOCKER_DIR=%PROJECT_ROOT%\docker"
call :run_bash_script "%SCRIPT_DIR%scripts\build\docker-build.sh" "%args%"
exit /b %ERRORLEVEL%

:: Handle clean command - removes build artifacts
:handle_clean
set "args=%*"
set "args=!args:*clean =!"
echo Cleaning build artifacts...

call %mvn_command% clean %args%

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

:: Handle deploy command
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

set "DOCKER_DIR=%PROJECT_ROOT%\docker"
call :run_bash_script "%SCRIPT_DIR%scripts\deploy\prod-deploy.sh" "%args%"
exit /b %ERRORLEVEL%

:: Handle development environment command
:handle_dev
call :load_env
set "args=%*"
set "args=!args:*dev =!"

set "DOCKER_DIR=%PROJECT_ROOT%\docker"
call :is_postgres_running
if !ERRORLEVEL! EQU 0 (
    echo Detected PostgreSQL container is already running.
    echo Auto-adding --use-existing-db flag.
    call :run_bash_script "%SCRIPT_DIR%scripts\dev\dev-environment.sh" "--mode dev --use-existing-db %args%"
) else (
    call :run_bash_script "%SCRIPT_DIR%scripts\dev\dev-environment.sh" "--mode dev %args%"
)
exit /b %ERRORLEVEL%

:: Handle local development command
:handle_local
call :load_env
set "args=%*"
set "args=!args:*local =!"

call :check_docker
if !ERRORLEVEL! NEQ 0 (
    echo Error: Docker is required for local development.
    exit /b 1
)

set "DOCKER_DIR=%PROJECT_ROOT%\docker"
call :is_postgres_running
if !ERRORLEVEL! EQU 0 (
    echo Detected PostgreSQL container is already running.
    echo Auto-adding --use-existing-db flag.
    call :run_bash_script "%SCRIPT_DIR%scripts\dev\dev-environment.sh" "--mode local --use-existing-db %args%"
) else (
    call :run_bash_script "%SCRIPT_DIR%scripts\dev\dev-environment.sh" "--mode local %args%"
)
exit /b %ERRORLEVEL%

:: Handle quality check command
:handle_quality
set "args=%*"
set "args=!args:*quality =!"
call :run_bash_script "%SCRIPT_DIR%scripts\quality\quality-check.sh" "%args%"
exit /b %ERRORLEVEL%

:: Handle linting command - autofix code style issues
:handle_lint
set "args=%*"
set "args=!args:*lint =!"
echo Running auto-fix linting...

call %mvn_command% spotless:apply -q
call :run_bash_script "%SCRIPT_DIR%scripts\quality\quality-check.sh" "--quick --auto-fix %args%"

echo Linting and auto-fixing completed!
exit /b 0

:: Handle verification command
:handle_verify
set "args=%*"
set "args=!args:*verify =!"
call :run_bash_script "%SCRIPT_DIR%scripts\build\verify.sh" "%args%"
exit /b %ERRORLEVEL%

:: Handle test command
:handle_test
set "args=%*"
set "args=!args:*test =!"
echo Running tests with test profile...

call %mvn_command% test -Dspring.profiles.active=test %args%
exit /b %ERRORLEVEL%

:: Handle test environment command
:handle_test_env
call :load_env
set "args=%*"
set "args=!args:*test-env =!"

set "DOCKER_DIR=%PROJECT_ROOT%\docker"

call :is_postgres_running
if !ERRORLEVEL! EQU 0 (
    echo %args% | findstr /C:"--no-docker" >nul 2>&1
    if !ERRORLEVEL! NEQ 0 (
        echo Detected PostgreSQL container is already running.
        echo Auto-adding --use-existing-db flag.
        call :run_bash_script "%SCRIPT_DIR%scripts\dev\dev-environment.sh" "--mode test --use-existing-db %args%"
    ) else (
        call :run_bash_script "%SCRIPT_DIR%scripts\dev\dev-environment.sh" "--mode test %args%"
    )
) else (
    call :run_bash_script "%SCRIPT_DIR%scripts\dev\dev-environment.sh" "--mode test %args%"
)
exit /b %ERRORLEVEL%

:: Handle health check command
:handle_health
set "args=%*"
set "args=!args:*health =!"
echo Running health check...
call :run_bash_script "%SCRIPT_DIR%scripts\utils\health-check.sh" "%args%"
exit /b %ERRORLEVEL%

:: Handle database management command
:handle_db
set "subcommand=%2"
set "args=%*"
set "args=!args:*db =!"
set "args=!args:*%subcommand% =!"

if "%subcommand%"=="start" (
    echo Starting PostgreSQL database...
    set "DOCKER_DIR=%PROJECT_ROOT%\docker"
    call :run_bash_script "-c" "source '%SCRIPT_DIR%scripts\utils\db-utils.sh' && start_postgres"
    exit /b %ERRORLEVEL%
) else if "%subcommand%"=="stop" (
    echo Stopping PostgreSQL database...
    call :check_docker
    if !ERRORLEVEL! EQU 0 (
        docker ps --format "{{.Names}}" | findstr /C:"hiresync-postgres" >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            docker stop hiresync-postgres
            echo PostgreSQL database stopped.
        ) else (
            echo No running PostgreSQL container found.
        )
    )
    exit /b 0
) else if "%subcommand%"=="restart" (
    echo Restarting PostgreSQL database...
    call :check_docker
    if !ERRORLEVEL! EQU 0 (
        docker ps -a --format "{{.Names}}" | findstr /C:"hiresync-postgres" >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            docker restart hiresync-postgres
            echo PostgreSQL database restarted.
        ) else (
            echo No PostgreSQL container found to restart.
            echo Use 'run.cmd db start' to create and start a new container.
        )
    )
    exit /b 0
) else if "%subcommand%"=="status" (
    call :check_docker
    if !ERRORLEVEL! EQU 0 (
        docker ps --format "{{.Names}}" | findstr /C:"hiresync-postgres" >nul 2>&1
        if !ERRORLEVEL! EQU 0 (
            echo PostgreSQL database is running.
            docker ps --filter "name=hiresync-postgres" --format "table {{.ID}}  {{.Names}}  {{.Status}}  {{.Ports}}"
        ) else (
            docker ps -a --format "{{.Names}}" | findstr /C:"hiresync-postgres" >nul 2>&1
            if !ERRORLEVEL! EQU 0 (
                echo PostgreSQL container exists but is not running.
                docker ps -a --filter "name=hiresync-postgres" --format "table {{.ID}}  {{.Names}}  {{.Status}}"
                echo Use 'run.cmd db start' to start the container.
            ) else (
                echo No PostgreSQL container found.
                echo Use 'run.cmd db start' to create and start a new container.
            )
        )
    )
    exit /b 0
) else (
    echo Usage: run.cmd db [start^|stop^|restart^|status]
    echo   start    - Start the PostgreSQL database container
    echo   stop     - Stop the PostgreSQL database container
    echo   restart  - Restart the PostgreSQL database container
    echo   status   - Check if the PostgreSQL database is running
    exit /b 1
)

::==============================================================================
:: Main Entry Point
::==============================================================================
:main
:: Check for Maven/Maven wrapper
set "mvn_command=mvn"

if exist "%PROJECT_ROOT%\mvnw.cmd" (
    set "mvn_command=%PROJECT_ROOT%\mvnw.cmd"
) else if exist "%PROJECT_ROOT%\mvnw" (
    set "mvn_command=%PROJECT_ROOT%\mvnw"
) else if exist ".\mvnw.cmd" (
    set "mvn_command=.\mvnw.cmd"
) else if exist ".\mvnw" (
    set "mvn_command=.\mvnw"
) else (
    where mvn >nul 2>&1
    if !ERRORLEVEL! NEQ 0 (
        echo Error: Neither Maven wrapper nor 'mvn' command is available.
        echo Please install Maven or generate a Maven wrapper using:
        echo mvn -N io.takari:maven:wrapper
        exit /b 1
    )
    echo Warning: Maven wrapper not found. Using 'mvn' command instead...
)

:: Process command
if "%1"=="" (
    call :show_help
    exit /b 0
)
if "%1"=="--help" (
    call :show_help
    exit /b 0
)
if "%1"=="-h" (
    call :show_help
    exit /b 0
)
if "%1"=="help" (
    call :show_help
    exit /b 0
)

if "%1"=="build" (
    call :handle_build %*
    exit /b %ERRORLEVEL%
)
if "%1"=="clean" (
    call :handle_clean %*
    exit /b %ERRORLEVEL%
)
if "%1"=="db" (
    call :handle_db %*
    exit /b %ERRORLEVEL%
)
if "%1"=="deploy" (
    call :handle_deploy %*
    exit /b %ERRORLEVEL%
)
if "%1"=="dev" (
    call :handle_dev %*
    exit /b %ERRORLEVEL%
)
if "%1"=="local" (
    call :handle_local %*
    exit /b %ERRORLEVEL%
)
if "%1"=="quality" (
    call :handle_quality %*
    exit /b %ERRORLEVEL%
)
if "%1"=="lint" (
    call :handle_lint %*
    exit /b %ERRORLEVEL%
)
if "%1"=="verify" (
    call :handle_verify %*
    exit /b %ERRORLEVEL%
)
if "%1"=="test" (
    call :handle_test %*
    exit /b %ERRORLEVEL%
)
if "%1"=="test-env" (
    call :handle_test_env %*
    exit /b %ERRORLEVEL%
)
if "%1"=="health" (
    call :handle_health %*
    exit /b %ERRORLEVEL%
)

echo Error: Unknown command '%1'
echo Run 'run.cmd --help' for usage information
exit /b 1 