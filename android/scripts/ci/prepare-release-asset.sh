#!/bin/sh
set -eu

tag=${1:-}
apk=${2:-android/app/build/outputs/apk/release/app-release.apk}

if [ -z "$tag" ]; then
    echo "::error::Release tag is empty" >&2
    exit 1
fi

if [ -z "${GITHUB_OUTPUT:-}" ]; then
    echo "::error::GITHUB_OUTPUT is not set" >&2
    exit 1
fi

if [ ! -f "$apk" ]; then
    echo "::error::Release APK not found at $apk" >&2
    find android/app/build/outputs/apk -type f -name '*.apk' -print 2>/dev/null || true
    exit 1
fi

safe_tag=$(printf '%s' "$tag" | tr '/' '-')
mkdir -p dist
asset="dist/loc-relay-$safe_tag.apk"
asset_name="loc-relay-$safe_tag.apk"

cp "$apk" "$asset"

{
    echo "tag=$tag"
    echo "asset=$asset"
    echo "asset_name=$asset_name"
} >> "$GITHUB_OUTPUT"
