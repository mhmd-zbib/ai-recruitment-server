@echo off
:: Install Git hooks for HireSync project
:: This script installs the hooks in .git-hooks to the .git/hooks directory

setlocal enabledelayedexpansion

:: Colors for output
set "RED=[91m"
set "GREEN=[92m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "NC=[0m"

echo %BLUE%========== HireSync Git Hooks Installer ==========%NC%

:: Get script and project directories
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%\.."
cd /d "%PROJECT_ROOT%"

:: Get Git directory
for /f "tokens=*" %%a in ('git rev-parse --git-dir') do set "GIT_DIR=%%a"
if not defined GIT_DIR (
    echo %RED%Error: Not in a Git repository%NC%
    exit /b 1
)

set "HOOKS_DIR=%GIT_DIR%\hooks"

echo %BLUE%Installing Git hooks from: %SCRIPT_DIR%%NC%
echo %BLUE%To Git hooks directory: %HOOKS_DIR%%NC%

:: Create hooks directory if it doesn't exist
if not exist "%HOOKS_DIR%" mkdir "%HOOKS_DIR%"

:: Install each hook
for %%f in ("%SCRIPT_DIR%\*") do (
    set "FILENAME=%%~nxf"
    if not "!FILENAME!"=="install.cmd" if not "!FILENAME!"=="install.sh" if not "!FILENAME!"=="README.md" (
        echo %BLUE%Installing hook: !FILENAME!%NC%
        copy /Y "%%f" "%HOOKS_DIR%\!FILENAME!" > nul
        echo %GREEN%Successfully installed: !FILENAME!%NC%
    )
)

:: Configure Git to use core.hooksPath
git config core.hooksPath "%HOOKS_DIR%"

echo %GREEN%Git hooks installation completed!%NC%
echo %YELLOW%Note: To bypass hooks temporarily, use: set "GIT_BYPASS_HOOKS=1" before git commit/push%NC%

endlocal
exit /b 0 