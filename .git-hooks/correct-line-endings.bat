@echo off
setlocal enabledelayedexpansion

:: Script to correct line endings for all files in the repository
:: This ensures that the correct line endings are used based on file type
:: and platform requirements as defined in .gitattributes

echo.
echo === Correcting Line Endings ===
echo.

:: Get the directory of this script and the repository root
set "SCRIPT_DIR=%~dp0"
set "SCRIPT_DIR=%SCRIPT_DIR:~0,-1%"
set "REPO_ROOT=%SCRIPT_DIR%\.."

echo Working in repository: %REPO_ROOT%
echo.

:: Make Git detect and convert CRLF/LF according to .gitattributes
echo Applying .gitattributes line ending rules...
git add --renormalize .

:: Check if there are any files with changed line endings
git diff --name-only --exit-code
if %ERRORLEVEL% EQU 0 (
    echo All files already have correct line endings.
) else (
    echo The following files had their line endings corrected:
    git diff --name-only
    echo.
    echo Please commit these changes.
)

:: Create helpers directory if it doesn't exist
if not exist "%REPO_ROOT%\.git-hooks\helpers" mkdir "%REPO_ROOT%\.git-hooks\helpers"

:: Create a helper for running bash commands on Windows
set "BASH_HELPER=%REPO_ROOT%\.git-hooks\helpers\run-bash.bat"

:: Create the bash helper script if it doesn't exist
if not exist "%BASH_HELPER%" (
    (
        echo @echo off
        echo setlocal enabledelayedexpansion
        echo.
        echo :: Helper script to run bash commands
        echo :: Finds bash.exe from Git for Windows installation
        echo.
        echo :: Find bash.exe in PATH
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
        echo     echo Error: Git Bash not found. Please make sure Git for Windows is installed.
        echo     exit /b 1
        echo ^)
        echo.
        echo :: Run the command with bash
        echo "!BASH_PATH!" %%*
        echo.
        echo endlocal
    ) > "%BASH_HELPER%"
)

:: Set executable permissions using bash
echo.
echo Making shell scripts executable using Git Bash...
call "%BASH_HELPER%" -c "find \"%REPO_ROOT%\" -name \"*.sh\" -type f -exec chmod +x {} \;"
call "%BASH_HELPER%" -c "find \"%REPO_ROOT%/.git-hooks\" -type f -not -name \"*.bat\" -not -name \"*.md\" -exec chmod +x {} \;"
call "%BASH_HELPER%" -c "find \"%REPO_ROOT%\" -name \"mvnw\" -type f -exec chmod +x {} \;"

echo.
echo Line ending correction completed successfully.
echo All shell scripts are now executable.
echo.

endlocal 