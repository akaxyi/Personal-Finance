@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Build a self-contained Windows installer (.exe) with a bundled JRE using jpackage
REM Requirements for building: JDK 17+ (with jpackage)

set APP_NAME=PersonalFinance
set VENDOR=JetBrains Sample
set VERSION=1.0.0
set DIST=dist

if not exist %DIST% mkdir %DIST% >nul 2>nul

where jpackage >NUL 2>&1
if errorlevel 1 (
  echo jpackage not found. Install a JDK 17+ that includes jpackage and ensure it's first in PATH.
  echo For example: https://adoptium.net/ (Temurin 17+)
  pause
  exit /b 1
)

REM Try Maven first; if not present, build the jar manually with javac/jar
where mvn >NUL 2>&1
if errorlevel 1 (
  echo Maven not found. Building JAR with JDK tools...
  set TARGET=target
  if not exist %TARGET% mkdir %TARGET% >nul 2>nul
  set SRCLIST=%TARGET%\sources.txt
  if exist "%SRCLIST%" del /Q "%SRCLIST%" >nul 2>nul
  cmd /c dir /S /B src\main\java\*.java > "%SRCLIST%"
  where javac >NUL 2>&1
  if errorlevel 1 (
    echo javac not found in PATH. Ensure a JDK 17+ is installed and active.
    pause
    exit /b 1
  )
  echo Compiling sources...
  javac -encoding UTF-8 -d %TARGET% @"%SRCLIST%"
  if errorlevel 1 (
    echo Compile failed.
    pause
    exit /b 1
  )
  if exist src\main\resources xcopy /E /I /Y src\main\resources %TARGET% >nul 2>nul
  echo Manifest-Version: 1.0> %TARGET%\MANIFEST.MF
  echo Main-Class: com.jetbrains.ui.FinanceApp>> %TARGET%\MANIFEST.MF
  where jar >NUL 2>&1
  if errorlevel 1 (
    if exist "%JAVA_HOME%\bin\jar.exe" (
      set JAR="%JAVA_HOME%\bin\jar.exe"
    ) else (
      echo jar tool not found. Ensure JDK bin is in PATH.
      pause
      exit /b 1
    )
  ) else (
    set JAR=jar
  )
  set INPUT_DIR=%TARGET%
  set MAIN_JAR=app-manual.jar
  if exist "%INPUT_DIR%\%MAIN_JAR%" del /Q "%INPUT_DIR%\%MAIN_JAR%" >nul 2>nul
  %JAR% cfm "%INPUT_DIR%\%MAIN_JAR%" "%INPUT_DIR%\MANIFEST.MF" -C %TARGET% .
  if errorlevel 1 (
    echo JAR creation failed.
    pause
    exit /b 1
  )
) else (
  echo Building with Maven...
  call mvn -q -DskipTests package
  if errorlevel 1 (
    echo Maven build failed.
    pause
    exit /b 1
  )
  set INPUT_DIR=target
  set MAIN_JAR=
  for %%F in (target\app-*.jar) do (
    if not defined MAIN_JAR set MAIN_JAR=%%~nxF
  )
  if not defined MAIN_JAR (
    echo Could not find built jar under target\app-*.jar
    pause
    exit /b 1
  )
)

echo Using input dir: %INPUT_DIR%
echo Using main jar: %MAIN_JAR%

REM Create installer (.exe) into dist\
jpackage ^
  --name "%APP_NAME%" ^
  --vendor "%VENDOR%" ^
  --app-version %VERSION% ^
  --type exe ^
  --input "%INPUT_DIR%" ^
  --main-jar "%MAIN_JAR%" ^
  --dest "%DIST%" ^
  --win-dir-chooser ^
  --win-menu ^
  --win-shortcut

if errorlevel 1 (
  echo jpackage failed to create the installer.
  pause
  exit /b 1
)

echo.
echo Installer created under %DIST%\
dir /b %DIST%\*.exe 2>nul
echo Done.
endlocal
