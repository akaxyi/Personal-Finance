@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Package the Personal Finance GUI app into a runnable JAR under dist/
set DIST=dist
set TARGET=target
set MAIN_CLASS=com.jetbrains.ui.FinanceApp

if not exist %TARGET% mkdir %TARGET%
if not exist %DIST% mkdir %DIST%

REM Copy resources (icons, fonts, etc.) into target so they end up inside the jar
if exist src\main\resources (
  xcopy /E /I /Y src\main\resources %TARGET% >nul 2>nul
)

REM Resolve Java tools
set JAVAC=javac
set JAR=
where javac >NUL 2>&1 && set JAVAC=javac
where jar >NUL 2>&1 && set JAR=jar
if not defined JAR (
  if exist "%JAVA_HOME%\bin\jar.exe" set "JAR=%JAVA_HOME%\bin\jar.exe"
)
if exist "%JAVA_HOME%\bin\javac.exe" set "JAVAC=%JAVA_HOME%\bin\javac.exe"

echo [1/3] Compiling sources to %TARGET% using: %JAVAC%
set SRCLIST=%TARGET%\sources.txt
if exist "%SRCLIST%" del /Q "%SRCLIST%" >nul 2>nul
cmd /c dir /S /B src\main\java\*.java > "%SRCLIST%"
"%JAVAC%" -encoding UTF-8 -d %TARGET% @"%SRCLIST%"
if errorlevel 1 (
  echo Compile failed. Ensure JDK is installed and in PATH (or JAVA_HOME set).
  echo Detected JAVA_HOME=%JAVA_HOME%
  pause
  exit /b 1
)

for /f %%A in ('dir /s /b %TARGET% ^| findstr /r ".*\.class$"') do set HAS_CLASSES=1
if not defined HAS_CLASSES (
  echo No compiled classes found in %TARGET%. Packaging aborted.
  pause
  exit /b 1
)

if not defined JAR (
  echo Could not find the 'jar' tool. Ensure JAVA_HOME is set correctly or add JDK\bin to PATH.
  echo JAVA_HOME=%JAVA_HOME%
  pause
  exit /b 1
)

echo [2/3] Creating manifest ...
set MANIFEST=%TARGET%\MANIFEST.MF
echo Manifest-Version: 1.0> "%MANIFEST%"
echo Main-Class: %MAIN_CLASS%>> "%MANIFEST%"

set JAR_FILE=%DIST%\personal-finance-app.jar
if exist "%JAR_FILE%" del /Q "%JAR_FILE%" > NUL 2>&1

echo [3/3] Building JAR %JAR_FILE% using: %JAR%
"%JAR%" cfm "%JAR_FILE%" "%MANIFEST%" -C %TARGET% .
if errorlevel 1 (
  echo JAR creation failed. Ensure the 'jar' tool is available (JAVA_HOME/bin/jar.exe) and not blocked by antivirus.
  pause
  exit /b 1
)

echo.

echo Built JAR: %JAR_FILE%
echo Run it with:
echo   javaw -jar "%JAR_FILE%"
endlocal
