@echo off
cd /d "%~dp0"

echo Compiling...
javac -encoding UTF-8 -cp "lib\mysql-connector-j-8.3.0.jar" -d out ^
  src\bank\config\*.java ^
  src\bank\model\*.java ^
  src\bank\dao\*.java ^
  src\bank\service\*.java ^
  src\bank\security\*.java ^
  src\bank\exception\*.java ^
  src\bank\util\*.java ^
  src\bank\ui\*.java ^
  src\bank\Main.java ^
  src\bank\LoginTestRunner.java

if errorlevel 1 (
    echo Compile failed!
    pause
    exit /b 1
)

echo Starting Bank Management System...
java -cp "out;lib\mysql-connector-j-8.3.0.jar" bank.Main
pause
