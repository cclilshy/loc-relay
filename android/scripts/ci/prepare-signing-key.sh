#!/bin/sh
set -eu

required_vars="ANDROID_KEYSTORE_BASE64 ANDROID_KEYSTORE_PASSWORD ANDROID_KEY_ALIAS ANDROID_KEY_PASSWORD GITHUB_ENV RUNNER_TEMP"

for name in $required_vars; do
    eval "value=\${$name:-}"
    if [ -z "$value" ]; then
        echo "::error::Missing required environment variable: $name" >&2
        exit 1
    fi
done

keystore="$RUNNER_TEMP/loc-relay-release.jks"
printf '%s' "$ANDROID_KEYSTORE_BASE64" | base64 --decode > "$keystore"

{
    echo "LOC_RELAY_KEYSTORE_FILE=$keystore"
    echo "LOC_RELAY_KEYSTORE_PASSWORD=$ANDROID_KEYSTORE_PASSWORD"
    echo "LOC_RELAY_KEY_ALIAS=$ANDROID_KEY_ALIAS"
    echo "LOC_RELAY_KEY_PASSWORD=$ANDROID_KEY_PASSWORD"
} >> "$GITHUB_ENV"
