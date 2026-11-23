#!/usr/bin/env bash
#
# Birthday Reminder - Local APK Build Script
#

set -e

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo "=========================================="
echo "🎂 Building Birthday Reminder Android APK"
echo "=========================================="

# 1. Detect Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Library/Android/sdk" ]; then
        export ANDROID_HOME="$HOME/Library/Android/sdk"
        echo "Found Android SDK at: $ANDROID_HOME"
    else
        echo "⚠️ ANDROID_HOME environment variable is not set."
        echo "If you have Android Studio installed, make sure the SDK is at ~/Library/Android/sdk"
        echo "Or install via Homebrew: brew install --cask android-commandlinetools"
    fi
fi

# 2. Check Java
if ! command -v java &> /dev/null; then
    echo "❌ Error: Java is required. Please install OpenJDK 17 or higher."
    exit 1
fi

echo "Using Java: $(java -version 2>&1 | head -n 1)"

# 3. Ensure gradlew is executable
chmod +x ./gradlew

# 4. Build Debug APK
echo ""
echo "🚀 Compiling APK with Gradle..."
./gradlew assembleDebug --stacktrace

APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo ""
    echo "=========================================="
    echo "🎉 APK BUILT SUCCESSFULLY!"
    echo "File location: $APK_PATH"
    echo "=========================================="
    echo ""
    echo "To install on your connected Android device via ADB:"
    echo "  adb install -r \"$APK_PATH\""
else
    echo "⚠️ Build finished. Look in app/build/outputs/apk/ for output."
fi
