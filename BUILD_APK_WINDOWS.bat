@echo off
setlocal
cd /d "%~dp0"
echo =====================================================
echo TV Engineering Remote - Android APK Builder
echo =====================================================
echo.
where java >nul 2>nul || (
  echo ERROR: Java/JDK is not installed or not in PATH.
  echo Install Android Studio, then run this file again.
  pause
  exit /b 1
)
if not defined ANDROID_HOME if not defined ANDROID_SDK_ROOT (
  echo Android SDK environment variable is not set.
  echo If Android Studio is installed, open the project once and let it sync.
  echo Then run: gradlew.bat assembleDebug
  echo.
)
call gradlew.bat --no-daemon assembleDebug
if errorlevel 1 (
  echo.
  echo BUILD FAILED.
  pause
  exit /b 1
)
copy /Y "app\build\outputs\apk\debug\app-debug.apk" "TV-Engineering-Remote-debug.apk" >nul
echo.
echo SUCCESS:
echo %CD%\TV-Engineering-Remote-debug.apk
pause
