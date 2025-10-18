@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Compile and run the GUI Personal Finance app (Swing) without console
if not exist target mkdir target

javac -encoding UTF-8 -d target ^
  src\main\java\com\jetbrains\finance\model\*.java ^
  src\main\java\com\jetbrains\finance\service\*.java ^
  src\main\java\com\jetbrains\finance\store\*.java ^
  src\main\java\com\jetbrains\ui\*.java ^
  src\main\java\com\jetbrains\Main.java

if errorlevel 1 (
  echo.
  echo Compile failed. Make sure you have a JDK installed and JAVA_HOME is set.
  echo You can check with:  javac -version
  pause
  exit /b 1
)

start "" javaw -cp target com.jetbrains.ui.FinanceApp
endlocal
