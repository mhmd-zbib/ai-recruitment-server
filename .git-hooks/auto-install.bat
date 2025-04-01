@echo off
setlocal enabledelayedexpansion

:: Auto-installation script for Git hooks (Windows version)
:: This script will:
:: 1. Install the hooks immediately
:: 2. Set up auto-installation on clone/pull for future uses
:: 3. Configure Git to ensure hooks are maintained across the team

echo.
echo === Automatic Git Hook Setup ===
echo.

:: Get the directory of this script and the repository root
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "REPO_ROOT=%SCRIPT_DIR%\.."

:: First, install the hooks immediately
echo Installing Git hooks immediately...

:: Get the Git directory
for /f "tokens=*" %%a in ('git rev-parse --git-dir 2^>nul') do set "GIT_DIR=%%a"

:: Fallback if git command fails
if "!GIT_DIR!"=="" (
    set "GIT_DIR=%REPO_ROOT%\.git"
)

:: Ensure the destination directory exists
set "HOOKS_DIR=!GIT_DIR!\hooks"
if not exist "!HOOKS_DIR!" (
    mkdir "!HOOKS_DIR!" 2>nul
    if errorlevel 1 (
        echo Error: Failed to create hooks directory at !HOOKS_DIR!
        exit /b 1
    )
)

:: List of hooks to install
set HOOKS=pre-commit commit-msg prepare-commit-msg pre-push post-checkout pre-rebase

:: Install each hook
for %%h in (%HOOKS%) do (
    set "SOURCE=%SCRIPT_DIR%\%%h"
    set "TARGET=!HOOKS_DIR!\%%h"
    
    if not exist "!SOURCE!" (
        echo Warning: Hook %%h not found in %SCRIPT_DIR%
        goto :continue
    )
    
    echo Installing %%h hook...
    
    :: Copy the hook
    copy /Y "!SOURCE!" "!TARGET!" >nul
    if errorlevel 1 (
        echo Error: Failed to copy %%h hook to !TARGET!
        exit /b 1
    )
    
    echo ✓ %%h installed successfully
    
    :continue
)

:: Set up post-merge hook to auto-update hooks on pull
set "AUTO_UPDATE_HOOK=%REPO_ROOT%\.git\hooks\post-merge"
echo Setting up auto-update on git pull...

:: Create post-merge hook for auto-update on pull
(
    echo @echo off
    echo :: Auto-update hooks when pulling from remote
    echo.
    echo :: Check if .git-hooks directory changed
    echo for /f "tokens=*" %%%%a in ^('git diff-tree -r --name-only --no-commit-id ORIG_HEAD HEAD ^| findstr ".git-hooks/"'^) do ^(
    echo     set "HOOK_CHANGES=true"
    echo ^)
    echo.
    echo if defined HOOK_CHANGES ^(
    echo     echo.
    echo     echo Git hooks have been updated in the repository.
    echo     echo Auto-updating your local hooks...
    echo.
    echo     :: Get the directory of the repository
    echo     for /f "tokens=*" %%%%a in ^('git rev-parse --show-toplevel'^) do set "REPO_ROOT=%%%%a"
    echo.
    echo     :: Run the hooks installation script
    echo     if exist "!REPO_ROOT!\.git-hooks\auto-install.bat" ^(
    echo         call "!REPO_ROOT!\.git-hooks\auto-install.bat"
    echo     ^)
    echo ^)
) > "%AUTO_UPDATE_HOOK%"

:: Create a post-checkout hook for initial clone
set "POST_CHECKOUT_AUTO=%REPO_ROOT%\.git\hooks\post-checkout"
if not exist "%POST_CHECKOUT_AUTO%" (
    echo Setting up auto-install on first clone...
    
    :: Create post-checkout script
    (
        echo @echo off
        echo setlocal enabledelayedexpansion
        echo :: Auto-install hooks on initial clone
        echo.
        echo :: Arguments passed to hook by Git
        echo set PREV_HEAD=%%1
        echo.
        echo :: Check if this is the initial clone ^(previous HEAD is all zeros^)
        echo if "%%PREV_HEAD%%"=="0000000000000000000000000000000000000000" ^(
        echo     echo.
        echo     echo First checkout detected. Installing Git hooks...
        echo.
        echo     :: Get the directory of the repository
        echo     for /f "tokens=*" %%%%a in ^('git rev-parse --show-toplevel'^) do set "REPO_ROOT=%%%%a"
        echo.
        echo     :: Run the hooks installation script
        echo     if exist "!REPO_ROOT!\.git-hooks\auto-install.bat" ^(
        echo         call "!REPO_ROOT!\.git-hooks\auto-install.bat"
        echo     ^)
        echo ^)
        echo.
        echo :: Execute the actual post-checkout hook if it exists separately
        echo if exist "!REPO_ROOT!\.git\hooks\post-checkout.actual" ^(
        echo     call "!REPO_ROOT!\.git\hooks\post-checkout.actual" %%*
        echo ^)
        echo.
        echo endlocal
    ) > "%POST_CHECKOUT_AUTO%"
    
    :: If a post-checkout hook already exists, rename it
    if exist "%REPO_ROOT%\.git-hooks\post-checkout" (
        copy "%REPO_ROOT%\.git-hooks\post-checkout" "%REPO_ROOT%\.git\hooks\post-checkout.actual" > nul
    )
)

:: Set up template directory for automatic hook installation
echo Setting up hook templates for future clones...
set "GIT_TEMPLATE_DIR=%REPO_ROOT%\.git-template"
if not exist "%GIT_TEMPLATE_DIR%\hooks" mkdir "%GIT_TEMPLATE_DIR%\hooks"

:: Create template post-checkout hook that auto-installs
(
    echo @echo off
    echo setlocal enabledelayedexpansion
    echo :: Template hook to install project hooks on clone
    echo.
    echo :: Get the repository root
    echo for /f "tokens=*" %%%%a in ^('git rev-parse --show-toplevel'^) do set "REPO_ROOT=%%%%a"
    echo.
    echo :: Check if this is the initial clone
    echo if "%%1"=="0000000000000000000000000000000000000000" ^(
    echo     echo First checkout detected. Installing project-specific hooks...
    echo.
    echo     :: Run the hooks installation script if it exists
    echo     if exist "!REPO_ROOT!\.git-hooks\auto-install.bat" ^(
    echo         call "!REPO_ROOT!\.git-hooks\auto-install.bat"
    echo     ^)
    echo ^)
    echo.
    echo :: Exit with success
    echo exit /b 0
    echo endlocal
) > "%GIT_TEMPLATE_DIR%\hooks\post-checkout"

:: Configure Git to use the template directory
echo Configuring Git template directory...
git config --local init.templateDir "%GIT_TEMPLATE_DIR%"

:: Add reminder to README if it doesn't already exist
echo Updating README.md to include auto-install instructions...
if exist "%REPO_ROOT%\README.md" (
    findstr /C:"Setup Git Hooks" "%REPO_ROOT%\README.md" > nul
    if errorlevel 1 (
        (
            echo.
            echo ## Setup Git Hooks
            echo.
            echo This repository uses Git hooks to ensure code quality and consistent commit messages.
            echo Run the following command to automatically set up the hooks:
            echo.
            echo ```bash
            echo # For Unix/Linux/macOS
            echo bash .git-hooks/auto-install.sh
            echo.
            echo # For Windows
            echo .\.git-hooks\auto-install.bat
            echo ```
            echo.
        ) >> "%REPO_ROOT%\README.md"
    )
)

:: Create a helper batch file to run bash scripts properly
set "HELPER_DIR=%GIT_DIR%\hooks\helpers"
if not exist "!HELPER_DIR!" mkdir "!HELPER_DIR!" 2>nul

:: Create run-bash-script.bat helper
set "HELPER_BAT=!HELPER_DIR!\run-bash-script.bat"
(
    echo @echo off
    echo setlocal
    echo.
    echo :: Helper script to run bash scripts using Git Bash
    echo.
    echo :: Find bash from Git for Windows
    echo for %%P in ^(bash.exe^) do set "BASH_PATH=%%~$PATH:P"
    echo.
    echo if "!BASH_PATH!"=="" ^(
    echo     :: Try common Git installation paths
    echo     if exist "C:\Program Files\Git\bin\bash.exe" ^(
    echo         set "BASH_PATH=C:\Program Files\Git\bin\bash.exe"
    echo     ^) else if exist "C:\Program Files ^(x86^)\Git\bin\bash.exe" ^(
    echo         set "BASH_PATH=C:\Program Files ^(x86^)\Git\bin\bash.exe"
    echo     ^)
    echo ^)
    echo.
    echo if "!BASH_PATH!"=="" ^(
    echo     echo Error: Git Bash not found. Please make sure Git is installed.
    echo     exit /b 1
    echo ^)
    echo.
    echo :: Run the script using bash
    echo "!BASH_PATH!" %%*
) > "!HELPER_BAT!"

echo.
echo ✅ Git hooks auto-installation has been set up successfully!
echo Hooks will automatically install for new clones and stay updated on pulls.
echo Note: Ask all team members to run this script once to enable these features.
echo.

endlocal
exit /b 0 