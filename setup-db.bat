@echo off
cd /d "%~dp0"
echo Setting up database...

"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p"Gane2006@12" < sql\setup.sql
if errorlevel 1 (
    echo Database setup failed! Check MySQL is running.
    pause
    exit /b 1
)

"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" -u root -p"Gane2006@12" < sql\seed_data.sql
echo.
echo Database ready! Run run.bat to start the app.
pause
