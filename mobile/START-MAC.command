#!/bin/sh
# DevPlant for iPhone: double-click this file on a Mac (or run `sh START-MAC.command` on Linux).
# It installs Node.js if needed, installs the app's packages, and starts Expo.
# Then scan the QR code with the iPhone camera; the app opens in Expo Go.
cd "$(dirname "$0")" || exit 1

echo
echo " =============================================="
echo "   DevPlant  -  start the app for your iPhone"
echo " =============================================="
echo

# ---- 1. Node.js (or Bun) ----
RUNNER=npm
if ! command -v node >/dev/null 2>&1; then
  if command -v bun >/dev/null 2>&1; then
    echo " Node.js not found, but Bun is installed. Using Bun."
    RUNNER=bun
  elif command -v brew >/dev/null 2>&1; then
    echo " Node.js is not installed. Installing it with Homebrew..."
    brew install node
  elif [ "$(uname)" = "Darwin" ]; then
    echo " Node.js is not installed. Downloading the installer from nodejs.org..."
    BASE="https://nodejs.org/dist/latest-v24.x/"
    PKG=$(curl -fsSL "$BASE" | grep -o 'node-v[0-9.]*\.pkg' | head -n 1)
    curl -fL "$BASE$PKG" -o "/tmp/$PKG" || exit 1
    echo " Your Mac password is needed to install Node.js:"
    sudo installer -pkg "/tmp/$PKG" -target / || exit 1
    export PATH="/usr/local/bin:$PATH"
  else
    echo " Please install Node.js LTS from https://nodejs.org (or your package manager), then run this again."
    exit 1
  fi
fi
if [ "$RUNNER" = npm ]; then
  if ! command -v node >/dev/null 2>&1; then
    echo " Node.js could not be installed. Install it from https://nodejs.org, then run this again."
    exit 1
  fi
  echo " Node.js $(node -v) found."
fi

# ---- 2. Packages (first run only) ----
if [ ! -f node_modules/expo/package.json ]; then
  echo
  echo " Installing the app's packages. The first time takes a few minutes..."
  if [ "$RUNNER" = bun ]; then bun install; else npm install; fi || {
    echo " Installing packages failed. Check the internet connection and run this again."
    exit 1
  }
fi

# ---- 3. Start Expo ----
echo
echo " On the iPhone: install \"Expo Go\" from the App Store."
echo
echo " Is the iPhone on the SAME Wi-Fi as this computer?"
echo "   y = yes (fastest)"
echo "   n = no, use a tunnel over the internet (works anywhere, a bit slower)"
printf " Type y or n and press Enter: "
read -r answer
MODE=""
case "$answer" in
  n|N|no|No) MODE="--tunnel" ;;
esac

echo
echo " Starting... When the QR code appears, open the iPhone Camera and point it at the code,"
echo " then tap the banner to open DevPlant in Expo Go."
echo " Press Ctrl+C (or close this window) to stop the app."
echo
if [ "$RUNNER" = bun ]; then
  exec bunx expo start $MODE
else
  exec npx expo start $MODE
fi
