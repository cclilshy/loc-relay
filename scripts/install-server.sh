#!/bin/sh
set -eu

REPO_URL=${REPO_URL:-https://github.com/cclilshy/loc-relay.git}
RAW_BASE_URL=${RAW_BASE_URL:-https://raw.githubusercontent.com/cclilshy/loc-relay/main/scripts}
INSTALL_DIR=${INSTALL_DIR:-${SERVER_INSTALL_DIR:-"$HOME/.loc-relay-server"}}
SERVER_PORT=${SERVER_PORT:-7000}
HTTP_PORT=${HTTP_PORT:-}
HTTPS_PORT=${HTTPS_PORT:-}
SKIP_START=${SKIP_START:-0}
SERVER_ADDR=${SERVER_ADDR:-}

die() {
    echo "ERROR: $*" >&2
    exit 1
}

usage() {
    echo "usage: install-server.sh [server_addr] [--port 7000] [--http-port 80] [--https-port 443]" >&2
}

parse_args() {
    while [ "$#" -gt 0 ]; do
        case "$1" in
            --server|--server-addr|--addr)
                [ "$#" -ge 2 ] || die "missing value for $1"
                SERVER_ADDR=$2
                shift 2
                ;;
            --port)
                [ "$#" -ge 2 ] || die "missing value for $1"
                SERVER_PORT=$2
                shift 2
                ;;
            --http-port)
                [ "$#" -ge 2 ] || die "missing value for $1"
                HTTP_PORT=$2
                shift 2
                ;;
            --https-port)
                [ "$#" -ge 2 ] || die "missing value for $1"
                HTTPS_PORT=$2
                shift 2
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            --*)
                usage
                die "unknown option: $1"
                ;;
            *)
                if [ -n "$SERVER_ADDR" ]; then
                    usage
                    die "unexpected argument: $1"
                fi
                SERVER_ADDR=$1
                shift
                ;;
        esac
    done
}

generate_token() {
    if command -v openssl >/dev/null 2>&1; then
        openssl rand -hex 24
        return
    fi

    if command -v od >/dev/null 2>&1; then
        od -An -N24 -tx1 /dev/urandom | tr -d ' \n'
        printf '\n'
        return
    fi

    die "openssl or od is required to generate a token"
}

server_addr() {
    if [ -n "${SERVER_ADDR:-}" ]; then
        printf '%s\n' "$SERVER_ADDR"
        return
    fi

    if command -v curl >/dev/null 2>&1; then
        addr=$(curl -fsS --max-time 5 https://api.ipify.org 2>/dev/null || true)
        if [ -n "$addr" ]; then
            printf '%s\n' "$addr"
            return
        fi
    fi

    if command -v hostname >/dev/null 2>&1; then
        ips=$(hostname -I 2>/dev/null || true)
        set -- $ips
        if [ "$#" -gt 0 ]; then
            printf '%s\n' "$1"
            return
        fi

        hostname 2>/dev/null && return
    fi

    die "could not detect server address; set SERVER_ADDR"
}

clone_or_update() {
    command -v git >/dev/null 2>&1 || die "git is required"

    if [ -d "$INSTALL_DIR/.git" ]; then
        git -C "$INSTALL_DIR" pull --ff-only
    elif [ -e "$INSTALL_DIR" ]; then
        die "$INSTALL_DIR exists but is not a git repo"
    else
        git clone "$REPO_URL" "$INSTALL_DIR"
    fi
}

loc_relay_target() {
    os=$(uname -s | tr '[:upper:]' '[:lower:]')
    arch=$(uname -m)
    case "$arch" in
        x86_64|amd64)
            arch=amd64
            ;;
        arm64|aarch64)
            arch=arm64
            ;;
        *)
            die "unsupported architecture: $arch"
            ;;
    esac
    printf '%s-%s\n' "$os" "$arch"
}

select_loc_relay_binary() {
    target=$(loc_relay_target)
    candidate="$INSTALL_DIR/bin/loc-relay-$target"
    if [ -x "$candidate" ]; then
        printf '%s\n' "$candidate"
        return
    fi

    candidate="$INSTALL_DIR/bin/loc-relay"
    if [ -x "$candidate" ]; then
        printf '%s\n' "$candidate"
        return
    fi

    die "no prebuilt loc-relay binary for $target; run ./build.sh before publishing"
}

parse_args "$@"
clone_or_update

token=$(generate_token)
addr=$(server_addr)
LOC_RELAY_BIN=$(select_loc_relay_binary)
INSTALL_DIR="$INSTALL_DIR" "$INSTALL_DIR/scripts/install-frp.sh"
set -- init-server --token "$token" --port "$SERVER_PORT" --addr "$addr" --raw-base-url "$RAW_BASE_URL"
if [ -n "$HTTP_PORT" ]; then
	set -- "$@" --http-port "$HTTP_PORT"
fi
if [ -n "$HTTPS_PORT" ]; then
	set -- "$@" --https-port "$HTTPS_PORT"
fi
"$LOC_RELAY_BIN" "$@"
touch "$INSTALL_DIR/.loc-relay-server"

if [ "$SKIP_START" != "1" ]; then
	"$LOC_RELAY_BIN" server install
fi

"$LOC_RELAY_BIN" server info
