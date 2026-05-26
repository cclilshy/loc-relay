#!/bin/sh
set -eu

required_vars="GH_TOKEN TAG ASSET ASSET_NAME GITHUB_SHA"

for name in $required_vars; do
    eval "value=\${$name:-}"
    if [ -z "$value" ]; then
        echo "::error::Missing required environment variable: $name" >&2
        exit 1
    fi
done

if gh release view "$TAG" >/dev/null 2>&1; then
    gh release upload "$TAG" "$ASSET#$ASSET_NAME" --clobber
else
    gh release create "$TAG" "$ASSET#$ASSET_NAME" \
        --title "loc-relay $TAG" \
        --generate-notes \
        --target "$GITHUB_SHA"
fi
