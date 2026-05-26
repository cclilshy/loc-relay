#!/bin/sh
set -eu

ANDROID_NDK_VERSION=${ANDROID_NDK_VERSION:-29.0.14206865}

find_sdkmanager() {
    if command -v sdkmanager >/dev/null 2>&1; then
        command -v sdkmanager
        return
    fi

    for sdk_root in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "/usr/local/lib/android/sdk"; do
        if [ -n "$sdk_root" ] && [ -x "$sdk_root/cmdline-tools/latest/bin/sdkmanager" ]; then
            printf '%s\n' "$sdk_root/cmdline-tools/latest/bin/sdkmanager"
            return
        fi
    done

    echo "ERROR: sdkmanager not found. Set ANDROID_HOME or ANDROID_SDK_ROOT." >&2
    exit 1
}

sdkmanager_bin=$(find_sdkmanager)

yes | "$sdkmanager_bin" --licenses >/dev/null
"$sdkmanager_bin" \
    "platforms;android-36" \
    "build-tools;36.0.0" \
    "platform-tools" \
    "ndk;$ANDROID_NDK_VERSION"
