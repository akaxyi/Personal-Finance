@echo off
setlocal

REM Create a ZIP of the portable app image at dist\PersonalFinance\ into dist\PersonalFinance.zip
set DIST=dist
set APP_NAME=PersonalFinance
set APP_DIR=%DIST%\%APP_NAME%
set ZIP_PATH=%DIST%\%APP_NAME%.zip

if not exist "%DIST%" (
  echo Dist folder not found. Run build-portable.bat first.
  exit /b 1
)

if not exist "%APP_DIR%" (
  echo Portable app image not found at %APP_DIR%.
  echo Attempting to build it now...
  call build-portable.bat || exit /b 1
)

if not exist "%APP_DIR%" (
  echo Still cannot find %APP_DIR%. Aborting.
  exit /b 1
)

where powershell >NUL 2>&1
if errorlevel 1 (
  echo PowerShell is required to create the ZIP. Please ensure PowerShell is available.
  exit /b 1
)

echo Creating ZIP: %ZIP_PATH%
powershell -NoLogo -NoProfile -Command "Compress-Archive -Path '%APP_DIR%\*' -DestinationPath '%ZIP_PATH%' -Force" || (
  echo Failed to create ZIP.
  exit /b 1
)

echo Done: %ZIP_PATH%
endlocal

