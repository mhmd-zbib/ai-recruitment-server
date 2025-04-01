@echo off
REM HireSync Application Manager for Windows
REM A batch file wrapper for the Bash run script

SETLOCAL EnableDelayedExpansion

REM Set project root directory
SET "PROJECT_ROOT=%~dp0"
CD /D "%PROJECT_ROOT%"

REM Check for Bash (Git Bash, WSL, Cygwin, etc.)
WHERE bash >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (
  ECHO.
  ECHO ERROR: Bash not found. Please install Git for Windows, WSL, or Cygwin.
  ECHO.
  ECHO You can download Git for Windows from: https://git-scm.com/download/win
  ECHO.
  EXIT /B 1
)

REM Pass all arguments to the bash script
IF "%1"=="" (
  bash ./run help
) ELSE (
  bash ./run %*
)

REM Exit with the same status code as the bash script
EXIT /B %ERRORLEVEL% 