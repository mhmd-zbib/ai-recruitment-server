@echo off
echo Running Spotless to fix all formatting issues...
call mvn spotless:apply
if %ERRORLEVEL% EQU 0 (
    echo All formatting issues have been fixed successfully!
) else (
    echo An error occurred while running Spotless.
    echo Please check the Maven output for more details.
)
pause 