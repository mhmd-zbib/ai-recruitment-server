@echo off
setlocal enabledelayedexpansion

:: Streamlined Git hooks installer for modern Git workflows (Windows version)
:: Uses core.hooksPath for zero-overhead installation

echo.
echo === Git Hooks Setup - Senior Engineering Edition ===
echo.

:: Get the directory of this script and repository root
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "REPO_ROOT=%SCRIPT_DIR%\.."
set "HOOKS_DIR=%SCRIPT_DIR%"

:: Check if hooks directory exists
if not exist "%HOOKS_DIR%\pre-commit" (
    echo Error: Git hooks directory not found or incomplete at:
    echo %HOOKS_DIR%
    exit /b 1
)

:: Check git version to ensure core.hooksPath is supported
for /f "tokens=3" %%v in ('git --version') do set GIT_VERSION=%%v

:: Compare versions
call :compare_versions "%GIT_VERSION%" "2.9.0" result

if %result% GEQ 0 (
    :: Modern Git with hooksPath support
    echo [92m✓ Git version %GIT_VERSION% supports modern hooks installation[0m
    
    :: Configure git to use the hooks directory directly
    git config core.hooksPath "%HOOKS_DIR%"
    
    echo [92m✓ Hooks installed via core.hooksPath (zero performance overhead)[0m
    
    :: Also update config to avoid issues with line endings
    git config core.autocrlf false
    git config core.eol native
    
    :: Add git aliases for bypassing hooks when needed
    git config --local alias.pushf "push --no-verify"
    git config --local alias.commitf "commit --no-verify"
    git config --local alias.bypass-hooks "!set GIT_BYPASS_HOOKS=true&& git"
    
    echo [92m✓ Added helpful git aliases:[0m
    echo   [93mgit pushf[0m - Push without hook verification
    echo   [93mgit commitf[0m - Commit without hook verification
    echo   [93mgit bypass-hooks[0m - Run any git command bypassing hooks
) else (
    :: Fall back to traditional copy method for older Git versions
    echo [93m⚠ Git version %GIT_VERSION% is older than 2.9.0[0m
    echo [93mUsing compatibility mode (file copies) for hook installation[0m
    
    :: Get the Git hooks directory
    for /f "tokens=*" %%a in ('git rev-parse --git-dir 2^>nul') do set "GIT_DIR=%%a"
    if "!GIT_DIR!"=="" set "GIT_DIR=%REPO_ROOT%\.git"
    
    set "GIT_HOOKS_DIR=!GIT_DIR!\hooks"
    if not exist "!GIT_HOOKS_DIR!" mkdir "!GIT_HOOKS_DIR!"
    
    :: List of hooks to install
    set HOOKS=pre-commit commit-msg prepare-commit-msg pre-push post-checkout pre-rebase
    
    :: Install each hook
    for %%h in (%HOOKS%) do (
        if exist "%HOOKS_DIR%\%%h" (
            copy /Y "%HOOKS_DIR%\%%h" "!GIT_HOOKS_DIR!\%%h" >nul
            echo [92m✓ Installed %%h hook[0m
        )
    )
)

:: Normalize line endings immediately using Git
echo [94mNormalizing line endings...[0m
git add --renormalize . >nul

:: Make bat/cmd files executable on Windows
attrib +x "%HOOKS_DIR%\*.bat" >nul 2>&1
attrib +x "%HOOKS_DIR%\*.cmd" >nul 2>&1

echo.
echo [92m✅ Git hooks successfully installed![0m
echo.
echo [93mAvailable environment variables to control hooks:[0m
echo   [94mset GIT_BYPASS_HOOKS=true[0m - Bypass all hooks
echo   [94mset SKIP_PUSH_HOOKS=true[0m - Skip pre-push hooks only
echo   [94mset FORCE_CHECKS=true[0m - Force full checks even if unchanged
echo   [94mset BYPASS_COMMIT_MSG_HOOK=true[0m - Skip commit message validation
echo.

:: Provide quick usage guide
echo [94mQuick Usage Guide:[0m
echo   • Normal workflow: Just commit and push as usual
echo   • Bypass hooks: [93mset GIT_BYPASS_HOOKS=true && git commit -m "message"[0m
echo   • Skip for a single repo: [93mgit config --local hooks.enabled false[0m
echo.

goto :eof

:compare_versions
setlocal
set "ver1=%~1"
set "ver2=%~2"

for /f "tokens=1,2,3 delims=." %%a in ("%ver1%") do (
    set "v1_1=%%a"
    set "v1_2=%%b"
    set "v1_3=%%c"
)

for /f "tokens=1,2,3 delims=." %%a in ("%ver2%") do (
    set "v2_1=%%a"
    set "v2_2=%%b" 
    set "v2_3=%%c"
)

if %v1_1% GTR %v2_1% (
    endlocal & set "%~3=1" & goto :eof
) else if %v1_1% LSS %v2_1% (
    endlocal & set "%~3=-1" & goto :eof
)

if %v1_2% GTR %v2_2% (
    endlocal & set "%~3=1" & goto :eof
) else if %v1_2% LSS %v2_2% (
    endlocal & set "%~3=-1" & goto :eof
)

if %v1_3% GTR %v2_3% (
    endlocal & set "%~3=1" & goto :eof
) else if %v1_3% LSS %v2_3% (
    endlocal & set "%~3=-1" & goto :eof
) else (
    endlocal & set "%~3=0" & goto :eof
)

goto :eof 