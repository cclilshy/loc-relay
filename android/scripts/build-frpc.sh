#!/bin/sh
set -eu

FRP_VERSION=${FRP_VERSION:-0.69.0}
REPO_URL=${REPO_URL:-https://github.com/fatedier/frp.git}
ABIS=${ABIS:-"arm64-v8a armeabi-v7a x86_64 x86"}
ANDROID_API=${ANDROID_API:-26}
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ANDROID_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
OUT_ROOT="$ANDROID_DIR/app/src/main/jniLibs"

go_target_for_abi() {
    case "$1" in
        arm64-v8a)
            printf '%s\n' "arm64 "
            ;;
        armeabi-v7a)
            printf '%s\n' "arm 7"
            ;;
        x86_64)
            printf '%s\n' "amd64 "
            ;;
        x86)
            printf '%s\n' "386 "
            ;;
        *)
            echo "ERROR: unsupported Android ABI: $1" >&2
            exit 1
            ;;
    esac
}

linker_strategy_for_abi() {
    case "$1" in
        arm64-v8a)
            printf '%s\n' "internal"
            ;;
        *)
            printf '%s\n' "ndk"
            ;;
    esac
}

print_plan() {
    for abi in $ABIS; do
        set -- $(go_target_for_abi "$abi")
        goarch=$1
        printf '%s android/%s %s\n' "$abi" "$goarch" "$(linker_strategy_for_abi "$abi")"
    done
}

sdk_root() {
    if [ -n "${ANDROID_HOME:-}" ]; then
        printf '%s\n' "$ANDROID_HOME"
    elif [ -n "${ANDROID_SDK_ROOT:-}" ]; then
        printf '%s\n' "$ANDROID_SDK_ROOT"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        printf '%s\n' "$HOME/Library/Android/sdk"
    fi
}

find_ndk_home() {
    if [ -n "${ANDROID_NDK_HOME:-}" ]; then
        printf '%s\n' "$ANDROID_NDK_HOME"
        return 0
    fi
    if [ -n "${NDK_HOME:-}" ]; then
        printf '%s\n' "$NDK_HOME"
        return 0
    fi

    sdk=$(sdk_root)
    if [ -n "$sdk" ] && [ -d "$sdk/ndk" ]; then
        find "$sdk/ndk" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1
    fi
}

ndk_toolchain_bin() {
    ndk_home=$1
    host_prebuilt=$(find "$ndk_home/toolchains/llvm/prebuilt" -mindepth 1 -maxdepth 1 -type d | sort | tail -n 1)
    printf '%s\n' "$host_prebuilt/bin"
}

clang_for_abi() {
    abi=$1
    toolchain_bin=$2
    case "$abi" in
        arm64-v8a)
            printf '%s\n' "$toolchain_bin/aarch64-linux-android${ANDROID_API}-clang"
            ;;
        armeabi-v7a)
            printf '%s\n' "$toolchain_bin/armv7a-linux-androideabi${ANDROID_API}-clang"
            ;;
        x86_64)
            printf '%s\n' "$toolchain_bin/x86_64-linux-android${ANDROID_API}-clang"
            ;;
        x86)
            printf '%s\n' "$toolchain_bin/i686-linux-android${ANDROID_API}-clang"
            ;;
    esac
}

if [ "${DRY_RUN:-0}" = "1" ]; then
    print_plan
    exit 0
fi

command -v git >/dev/null 2>&1 || {
    echo "ERROR: git is required" >&2
    exit 1
}
command -v go >/dev/null 2>&1 || {
    echo "ERROR: go is required" >&2
    exit 1
}

tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT INT TERM

git clone --depth 1 --branch "v$FRP_VERSION" "$REPO_URL" "$tmp_dir/frp"

for abi in $ABIS; do
    set -- $(go_target_for_abi "$abi")
    goarch=$1
    goarm=${2:-}
    out_dir="$OUT_ROOT/$abi"
    out="$out_dir/libfrpc.so"
    mkdir -p "$out_dir"

    (
        cd "$tmp_dir/frp"
        if [ "$(linker_strategy_for_abi "$abi")" = "ndk" ]; then
            ndk_home=$(find_ndk_home || true)
            if [ -z "$ndk_home" ]; then
                echo "ERROR: $abi requires Android NDK external linking. Install an NDK with sdkmanager or set ANDROID_NDK_HOME." >&2
                exit 1
            fi
            toolchain_bin=$(ndk_toolchain_bin "$ndk_home")
            cc=$(clang_for_abi "$abi" "$toolchain_bin")
            if [ ! -x "$cc" ]; then
                echo "ERROR: missing Android clang for $abi: $cc" >&2
                exit 1
            fi
            if [ -n "$goarm" ]; then
                GOOS=android GOARCH="$goarch" GOARM="$goarm" CGO_ENABLED=1 CC="$cc" \
                    go build -tags noweb -trimpath -buildmode=pie -ldflags="-checklinkname=0 -s -w" -o "$out" ./cmd/frpc
            else
                GOOS=android GOARCH="$goarch" CGO_ENABLED=1 CC="$cc" \
                    go build -tags noweb -trimpath -buildmode=pie -ldflags="-checklinkname=0 -s -w" -o "$out" ./cmd/frpc
            fi
        elif [ -n "$goarm" ]; then
            GOOS=android GOARCH="$goarch" GOARM="$goarm" CGO_ENABLED=0 \
                go build -tags noweb -trimpath -buildmode=pie -ldflags="-checklinkname=0 -s -w" -o "$out" ./cmd/frpc
        else
            GOOS=android GOARCH="$goarch" CGO_ENABLED=0 \
                go build -tags noweb -trimpath -buildmode=pie -ldflags="-checklinkname=0 -s -w" -o "$out" ./cmd/frpc
        fi
    )

    chmod 755 "$out"
    echo "wrote $out"
done
