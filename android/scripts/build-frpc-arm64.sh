#!/bin/sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ABIS=${ABIS:-arm64-v8a}
export ABIS
exec "$SCRIPT_DIR/build-frpc.sh" "$@"
