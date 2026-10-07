@echo off
REM =========================================================
REM Starts the Smart Canteen server.
REM Run build.bat first (or whenever you change a .java file).
REM =========================================================

if not exist out\com\smartcanteen\Main.class (
    echo ERROR: Project is not built yet. Run build.bat first.
    pause
    exit /b 1
)

java -cp "out;lib\mysql-connector-j-26.7.0.jar" com.smartcanteen.Main
pause
