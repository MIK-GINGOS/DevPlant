@echo off
rem DevPlant for iPhone: double-click this file on a Windows computer.
rem It installs Node.js if needed, installs the app's packages, and starts Expo.
rem Then scan the QR code with the iPhone camera; the app opens in Expo Go.
setlocal
cd /d "%~dp0"
title DevPlant - Expo

echo.
echo  ==============================================
echo    DevPlant  -  start the app for your iPhone
echo  ==============================================
echo.

rem Opened straight from inside the zip, Windows copies only this file out, without the app.
if exist package.json goto unzipped
echo  Unzip first: right-click the downloaded zip, choose "Extract All...",
echo  then open the extracted folder and double-click START-WINDOWS.bat there.
pause
exit /b 1
:unzipped

rem ---- 1. Node.js (or Bun) ----
where node >nul 2>nul
if %errorlevel%==0 goto have_node

where bun >nul 2>nul
if %errorlevel%==0 (
  echo  Node.js not found, but Bun is installed. Using Bun.
  set RUNNER=bun
  goto install
)

echo  Node.js is not installed. Installing Node.js LTS now...
where winget >nul 2>nul
if not %errorlevel%==0 goto node_msi
winget install --id OpenJS.NodeJS.LTS -e --silent --accept-package-agreements --accept-source-agreements
goto node_installed

:node_msi
echo  Downloading the Node.js installer from nodejs.org...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $base='https://nodejs.org/dist/latest-v24.x/'; $name=((Invoke-WebRequest $base -UseBasicParsing).Links.href | Where-Object { $_ -match 'node-v[0-9.]+-x64\.msi$' } | Select-Object -First 1).Split('/')[-1]; $msi=Join-Path $env:TEMP $name; Invoke-WebRequest ($base+$name) -OutFile $msi -UseBasicParsing; Start-Process msiexec.exe -ArgumentList '/i',$msi,'/passive' -Wait"

:node_installed
set "PATH=%PATH%;%ProgramFiles%\nodejs"
where node >nul 2>nul
if not %errorlevel%==0 (
  echo.
  echo  Node.js could not be installed automatically.
  echo  Install it from https://nodejs.org  ^(the LTS button^), then run this file again.
  pause
  exit /b 1
)

:have_node
set RUNNER=npm
for /f "delims=" %%v in ('node -v') do echo  Node.js %%v found.

rem ---- 2. Packages (first run only) ----
:install
if exist node_modules\expo\package.json goto start
echo.
echo  Installing the app's packages. The first time takes a few minutes...
if "%RUNNER%"=="bun" (
  call bun install
) else (
  call npm install
)
if not %errorlevel%==0 (
  echo  Installing packages failed. Check the internet connection and run this file again.
  pause
  exit /b 1
)

rem ---- 3. Start Expo ----
:start
echo.
echo  On the iPhone: install "Expo Go" from the App Store.
echo.
echo  Is the iPhone on the SAME Wi-Fi as this computer?
echo    [Y] Yes  - fastest
echo    [N] No   - use a tunnel over the internet ^(works anywhere, a bit slower^)
choice /c YN /n /m "  Press Y or N: "
set MODE=
if errorlevel 2 set MODE=--tunnel

echo.
echo  Starting... When the QR code appears, open the iPhone Camera and point it at the code,
echo  then tap the banner to open DevPlant in Expo Go.
echo  If Windows asks to allow Node.js on the network, click Allow.
echo  Close this window to stop the app.
echo.
if "%RUNNER%"=="bun" (
  call bunx expo start %MODE%
) else (
  call npx expo start %MODE%
)
pause
