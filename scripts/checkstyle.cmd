@echo off
REM Script to run Checkstyle checks on the HireSync codebase

setlocal enabledelayedexpansion

REM Colors for Windows console
set RED=[91m
set GREEN=[92m
set YELLOW=[93m
set BLUE=[94m
set BOLD=[1m
set NC=[0m

REM Set script directory and project root
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
cd /d "%PROJECT_ROOT%"

REM Default parameters
set CHECK_MODE=full
set INCLUDE_TESTS=false

REM Parse command line arguments
:parse_args
if "%~1"=="" goto :end_parse_args
if "%~1"=="-q" (
    set CHECK_MODE=quick
    shift
    goto :parse_args
)
if "%~1"=="--quick" (
    set CHECK_MODE=quick
    shift
    goto :parse_args
)
if "%~1"=="-f" (
    set CHECK_MODE=full
    shift
    goto :parse_args
)
if "%~1"=="--full" (
    set CHECK_MODE=full
    shift
    goto :parse_args
)
if "%~1"=="-t" (
    set INCLUDE_TESTS=true
    shift
    goto :parse_args
)
if "%~1"=="--test" (
    set INCLUDE_TESTS=true
    shift
    goto :parse_args
)
if "%~1"=="-h" (
    goto :show_usage
)
if "%~1"=="--help" (
    goto :show_usage
)
echo %RED%Unknown option: %~1%NC%
goto :show_usage

:end_parse_args

REM Find Maven executable
if exist "%PROJECT_ROOT%\mvnw.cmd" (
    set MVN_CMD="%PROJECT_ROOT%\mvnw.cmd"
) else (
    set MVN_CMD=mvn
)

echo %BLUE%%BOLD%========== HireSync Checkstyle Runner ==========%NC%

REM Build command based on mode
set CHECKSTYLE_CMD=%MVN_CMD% checkstyle:check -Dcheckstyle.skip=false

REM Quick mode: only modified files
if "%CHECK_MODE%"=="quick" (
    echo %BLUE%Running in quick mode (modified files only)%NC%
    
    REM Create a temporary file to store modified files
    set "TEMP_FILE=%TEMP%\modified_files_%RANDOM%.txt"
    
    REM Get list of modified Java files
    git ls-files --modified --others --exclude-standard | findstr /i "\.java$" > "%TEMP_FILE%"
    
    REM Check if we have any modified files
    for %%F in ("%TEMP_FILE%") do if %%~zF==0 (
        echo %YELLOW%No modified Java files found%NC%
        del "%TEMP_FILE%" 2>nul
        goto :eof
    )
    
    set CHECKSTYLE_CMD=%CHECKSTYLE_CMD% -DcheckstyleFiles="%TEMP_FILE%"
) else (
    echo %BLUE%Running in full mode (all files)%NC%
)

REM Include test files if requested
if "%INCLUDE_TESTS%"=="true" (
    echo %BLUE%Including test files in checks%NC%
    set CHECKSTYLE_CMD=%CHECKSTYLE_CMD% -Dcheckstyle.includeTestSourceDirectory=true
) else (
    set CHECKSTYLE_CMD=%CHECKSTYLE_CMD% -Dcheckstyle.includeTestSourceDirectory=false
)

REM Run Checkstyle
echo %BLUE%Executing Checkstyle checks...%NC%
%CHECKSTYLE_CMD%

if %ERRORLEVEL% EQU 0 (
    echo %GREEN%Checkstyle checks passed!%NC%
) else (
    echo %RED%Checkstyle checks failed. Please fix the issues.%NC%
)

REM Clean up temporary file
if "%CHECK_MODE%"=="quick" (
    del "%TEMP_FILE%" 2>nul
)

goto :eof

:show_usage
echo %BOLD%Usage:%NC% %~nx0 [OPTIONS]
echo Run Checkstyle checks on the project codebase
echo.
echo %BOLD%Options:%NC%
echo   -q, --quick        Quick mode: check only modified files
echo   -f, --full         Full mode: check all files (default)
echo   -t, --test         Include test code in checks
echo   -h, --help         Show this help message
echo.
echo %BOLD%Examples:%NC%
echo   %~nx0 --quick         # Check only modified files
echo   %~nx0 --full --test   # Check all files including tests

endlocal
exit /b 0 