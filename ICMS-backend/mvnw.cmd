@REM ----------------------------------------------------------------------------
@REM ICMS Maven Bootstrapper for Windows
@REM ----------------------------------------------------------------------------

@echo off
setlocal envvar

set "DIR=%~dp0"
set "MAVEN_VER=3.9.6"
set "MAVEN_HOME=%DIR%.mvn\apache-maven-%MAVEN_VER%"
set "MAVEN_ZIP=%DIR%.mvn\maven.zip"

if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    goto run
)

echo [ICMS Backend] Local Maven not found. Downloading Maven %MAVEN_VER%...
if not exist "%DIR%.mvn" mkdir "%DIR%.mvn"

powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; (New-Object System.Net.WebClient).DownloadFile('https://archive.apache.org/dist/maven/maven-3/%MAVEN_VER%/binaries/apache-maven-%MAVEN_VER%-bin.zip', '%MAVEN_ZIP%')"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Failed to download Maven. Please check your network connection.
    exit /b 1
)

echo [ICMS Backend] Extracting Maven...
powershell -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%DIR%.mvn' -Force"
if %ERRORLEVEL% neq 0 (
    echo [ERROR] Extraction failed.
    exit /b 1
)

del "%MAVEN_ZIP%"
echo [ICMS Backend] Maven bootstrap completed successfully.

:run
set "JAVA_HOME="
"%MAVEN_HOME%\bin\mvn.cmd" %*
