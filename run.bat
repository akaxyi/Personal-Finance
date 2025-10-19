@echo off
setlocal ENABLEDELAYEDEXPANSION

REM Compile and run the GUI Personal Finance app (Swing) without console
if not exist target mkdir target

REM Copy resources (icons, etc.) into target so they are on the classpath
if exist src\main\resources (
  xcopy /E /I /Y src\main\resources target >nul 2>nul
)

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

REM Include both compiled classes in target and raw resources on the classpath
start "" javaw -cp target;src\main\resources com.jetbrains.ui.FinanceApp
endlocal
