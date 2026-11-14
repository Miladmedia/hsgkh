@echo off
setlocal
set "APP_HOME=%~dp0"
if "%GRADLE_USER_HOME%"=="" set "GRADLE_USER_HOME=%USERPROFILE%\.gradle"
set "DIST=%GRADLE_USER_HOME%\wrapper\dists\gradle-8.4-bin\local\gradle-8.4"
if not exist "%DIST%\bin\gradle.bat" (
  set "BASE=%GRADLE_USER_HOME%\wrapper\dists\gradle-8.4-bin\local"
  if not exist "%BASE%" mkdir "%BASE%"
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-8.4-bin.zip' -OutFile '%BASE%\gradle-8.4-bin.zip'; Expand-Archive -Force '%BASE%\gradle-8.4-bin.zip' '%BASE%' ; Remove-Item '%BASE%\gradle-8.4-bin.zip'"
  if errorlevel 1 exit /b %ERRORLEVEL%
)
call "%DIST%\bin\gradle.bat" -p "%APP_HOME%" %*
exit /b %ERRORLEVEL%
