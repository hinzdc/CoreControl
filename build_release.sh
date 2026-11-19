#!/data/data/com.termux/files/usr/bin/bash
# ==============================================================================
# SARGAVERSE Core Control - Automated Release & Distribution Build Script
# Package: com.sargaverse.corecontrol
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

AAPT2_BIN="/data/data/com.termux/files/usr/bin/aapt2"
APKSIGNER_BIN="/data/data/com.termux/files/home/android-sdk/build-tools/34.0.0/apksigner"

echo "=============================================================================="
echo " CORE CONTROL — SARGAVERSE RELEASE PIPELINE"
echo " Date: $(date '+%Y-%m-%d %H:%M:%S %Z')"
echo " Working Directory: $PWD"
echo "=============================================================================="

# 1. Clean
echo ""
echo "[Step 1/6] Cleaning project build directories..."
./gradlew clean --no-daemon

# 2. Run Unit Tests (Strict Gate: must pass)
echo ""
echo "[Step 2/6] Executing unit tests (Safety & Isolation test suite)..."
if ! ./gradlew test --no-daemon; then
    echo "ERROR: Unit tests FAILED! Aborting release pipeline." >&2
    exit 1
fi
echo "SUCCESS: All unit tests passed (100%)."

# 3. Build Debug APK
echo ""
echo "[Step 3/6] Building Debug APK..."
./gradlew assembleDebug --no-daemon

# 4. Build Release APK (Signed if configured, otherwise fallback to unsigned)
echo ""
echo "[Step 4/6] Building Release APK..."
./gradlew assembleRelease --no-daemon

# 5. Distribute APKs with version-date formatted names
echo ""
echo "[Step 5/6] Distributing versioned APKs into dist/..."
./gradlew distributeApks --no-daemon

# 6. Verification & Inspection
echo ""
echo "[Step 6/6] Verifying output artifacts..."
echo "------------------------------------------------------------------------------"

DIST_DIR="$SCRIPT_DIR/dist"
DATE_STR="$(date '+%Y-%m-%d')"
VERSION_NAME="1.0.0"

DEBUG_APK="$DIST_DIR/CoreControl-v${VERSION_NAME}-debug-${DATE_STR}.apk"
RELEASE_APK="$DIST_DIR/CoreControl-v${VERSION_NAME}-release-${DATE_STR}.apk"

# Check Debug APK
if [ -f "$DEBUG_APK" ]; then
    DEBUG_SIZE="$(ls -lh "$DEBUG_APK" | awk '{print $5}')"
    echo "Found Debug Artifact: $DEBUG_APK ($DEBUG_SIZE)"
    if [ -x "$AAPT2_BIN" ]; then
        PKG="$("$AAPT2_BIN" dump badging "$DEBUG_APK" | grep "package: name=" | head -n1)"
        echo "  AAPT2 Package: $PKG"
    fi
    if [ -f "$APKSIGNER_BIN" ]; then
        echo "  Signing Verification (Debug):"
        bash "$APKSIGNER_BIN" verify --verbose "$DEBUG_APK" 2>&1 | head -n 4 | sed 's/^/    /' || true
    fi
else
    echo "WARNING: Debug APK not found in dist/"
fi

echo ""
# Check Release APK
if [ -f "$RELEASE_APK" ]; then
    RELEASE_SIZE="$(ls -lh "$RELEASE_APK" | awk '{print $5}')"
    echo "Found Release Artifact: $RELEASE_APK ($RELEASE_SIZE)"
    if [ -x "$AAPT2_BIN" ]; then
        PKG="$("$AAPT2_BIN" dump badging "$RELEASE_APK" | grep "package: name=" | head -n1)"
        echo "  AAPT2 Package: $PKG"
    fi
    if [ -f "$APKSIGNER_BIN" ]; then
        echo "  Signing Verification (Release):"
        bash "$APKSIGNER_BIN" verify --verbose "$RELEASE_APK" 2>&1 | head -n 5 | sed 's/^/    /' || true
    fi
else
    echo "WARNING: Release APK not found in dist/"
fi

echo "=============================================================================="
echo " RELEASE BUILD COMPLETE"
echo " Output directory: $DIST_DIR"
echo "=============================================================================="
