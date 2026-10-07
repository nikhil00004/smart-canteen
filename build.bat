@echo off
setlocal EnableDelayedExpansion

if not exist "lib\mysql-connector-j-26.7.0.jar" (
    echo ERROR: MySQL Connector/J JAR not found.
    echo Expected:
    echo lib\mysql-connector-j-26.7.0.jar
    echo.
    pause
    exit /b 1
)

if not exist "out" mkdir out

echo.
echo Compiling...
echo.

set "SOURCE_FILES="

for /R "src" %%F in (*.java) do (
    set "SOURCE_FILES=!SOURCE_FILES! "%%F""
)

javac -cp "lib\mysql-connector-j-26.7.0.jar" -d "out" !SOURCE_FILES!

if errorlevel 1 (
    echo.
    echo Build FAILED. See the errors above.
    pause
    exit /b 1
)

echo.
echo Build succeeded!
echo Run start.bat to launch the server.
echo.

pause