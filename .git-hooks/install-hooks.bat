@echo off
echo Installing Git hooks...

:: Get the directory of this script
SET SCRIPT_DIR=%~dp0

:: Copy pre-commit hook to .git/hooks directory
echo Copying pre-commit hook...
copy /Y "%SCRIPT_DIR%pre-commit" "%SCRIPT_DIR%..\.git\hooks\"

:: Copy commit-msg hook to .git/hooks directory
echo Copying commit-msg hook...
copy /Y "%SCRIPT_DIR%commit-msg" "%SCRIPT_DIR%..\.git\hooks\"

:: Ensure the hooks are executable
echo Making hooks executable...
:: This doesn't do anything on Windows but is kept for compatibility with bash script

echo Git hooks installed successfully! 