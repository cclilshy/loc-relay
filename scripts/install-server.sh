#!/bin/sh
set -eu

REPO_URL=${REPO_URL:-https://github.com/cclilshy/loc-relay.git}
RAW_BASE_URL=${RAW_BASE_URL:-https://raw.githubusercontent.com/cclilshy/loc-relay/main/scripts}
if [ -z "${INSTALL_DIR:-}" ] && [ -z "${SERVER_INSTALL_DIR:-}" ]; then
    if [ -d "$HOME/.tayd-server" ]; then
        INSTALL_DIR="$HOME/.tayd-server"
    elif [ -d "$HOME/.loc-relay-server" ]; then
        INSTALL_DIR="$HOME/.loc-relay-server"
    else
        INSTALL_DIR="$HOME/.tayd-server"
    fi
else
    INSTALL_DIR=${INSTALL_DIR:-$SERVER_INSTALL_DIR}
fi
SERVER_PORT_EXPLICIT=0
[ -n "${SERVER_PORT:-}" ] && SERVER_PORT_EXPLICIT=1
SERVER_PORT=${SERVER_PORT:-7000}
HTTP_PORT=${HTTP_PORT:-}
HTTPS_PORT=${HTTPS_PORT:-}
SKIP_START=${SKIP_START:-0}
SERVER_ADDR=${SERVER_ADDR:-}
TOKEN=${TOKEN:-}
SERVER_ADDR_EXPLICIT=0
HTTP_PORT_EXPLICIT=0
HTTPS_PORT_EXPLICIT=0
[ -n "$SERVER_ADDR" ] && SERVER_ADDR_EXPLICIT=1
[ -n "$HTTP_PORT" ] && HTTP_PORT_EXPLICIT=1
[ -n "$HTTPS_PORT" ] && HTTPS_PORT_EXPLICIT=1

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
                SERVER_ADDR_EXPLICIT=1
                shift 2
                ;;
            --port)
                [ "$#" -ge 2 ] || die "missing value for $1"
                SERVER_PORT=$2
                SERVER_PORT_EXPLICIT=1
                shift 2
                ;;
            --http-port)
                [ "$#" -ge 2 ] || die "missing value for $1"
                HTTP_PORT=$2
                HTTP_PORT_EXPLICIT=1
                shift 2
                ;;
            --https-port)
                [ "$#" -ge 2 ] || die "missing value for $1"
                HTTPS_PORT=$2
                HTTPS_PORT_EXPLICIT=1
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
                SERVER_ADDR_EXPLICIT=1
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

json_string() {
    key=$1
    file=$2
    [ -f "$file" ] || return 0
    sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\"\\([^\"]*\\)\".*/\\1/p" "$file" | sed -n '1p'
}

json_number() {
    key=$1
    file=$2
    [ -f "$file" ] || return 0
    sed -n "s/.*\"$key\"[[:space:]]*:[[:space:]]*\\([0-9][0-9]*\\).*/\\1/p" "$file" | sed -n '1p'
}

frps_token() {
    file="$INSTALL_DIR/frps.toml"
    [ -f "$file" ] || return 0
    sed -n 's/^[[:space:]]*auth\.token[[:space:]]*=[[:space:]]*"\([^"]*\)".*/\1/p' "$file" | sed -n '1p'
}

frps_bind_port() {
    file="$INSTALL_DIR/frps.toml"
    [ -f "$file" ] || return 0
    sed -n 's/^[[:space:]]*bindPort[[:space:]]*=[[:space:]]*\([0-9][0-9]*\).*/\1/p' "$file" | sed -n '1p'
}

existing_server_info() {
    info_file="$INSTALL_DIR/server-install.json"
    EXISTING_TOKEN=$(json_string token "$info_file")
    if [ -z "$EXISTING_TOKEN" ]; then
        EXISTING_TOKEN=$(frps_token)
    fi
    EXISTING_ADDR=$(json_string addr "$info_file")
    EXISTING_SERVER_PORT=$(json_number port "$info_file")
    if [ -z "$EXISTING_SERVER_PORT" ]; then
        EXISTING_SERVER_PORT=$(frps_bind_port)
    fi
    EXISTING_HTTP_PORT=$(json_number http_port "$info_file")
    EXISTING_HTTPS_PORT=$(json_number https_port "$info_file")
}

tayd_target() {
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

select_tayd_binary() {
    target=$(tayd_target)
    for candidate in \
        "$INSTALL_DIR/bin/tayd-$target" \
        "$INSTALL_DIR/bin/loc-relay-$target" \
        "$INSTALL_DIR/bin/tayd" \
        "$INSTALL_DIR/bin/loc-relay"; do
        if [ -x "$candidate" ]; then
            printf '%s\n' "$candidate"
            return
        fi
    done

    die "no prebuilt tayd binary for $target; run ./build.sh before publishing"
}

parse_args "$@"
existing_server_info
clone_or_update

if [ -z "$TOKEN" ]; then
    TOKEN=${EXISTING_TOKEN:-}
fi
if [ -z "$TOKEN" ]; then
    TOKEN=$(generate_token)
fi
if [ "$SERVER_ADDR_EXPLICIT" != "1" ] && [ -n "${EXISTING_ADDR:-}" ]; then
    addr=$EXISTING_ADDR
else
    addr=$(server_addr)
fi
if [ "$SERVER_PORT_EXPLICIT" != "1" ] && [ -n "${EXISTING_SERVER_PORT:-}" ]; then
    SERVER_PORT=$EXISTING_SERVER_PORT
fi
if [ "$HTTP_PORT_EXPLICIT" != "1" ] && [ -n "${EXISTING_HTTP_PORT:-}" ]; then
    HTTP_PORT=$EXISTING_HTTP_PORT
fi
if [ "$HTTPS_PORT_EXPLICIT" != "1" ] && [ -n "${EXISTING_HTTPS_PORT:-}" ]; then
    HTTPS_PORT=$EXISTING_HTTPS_PORT
fi

TAYD_BIN=$(select_tayd_binary)
INSTALL_DIR="$INSTALL_DIR" "$INSTALL_DIR/scripts/install-frp.sh"
set -- init-server --token "$TOKEN" --port "$SERVER_PORT" --addr "$addr" --raw-base-url "$RAW_BASE_URL"
if [ -n "$HTTP_PORT" ]; then
	set -- "$@" --http-port "$HTTP_PORT"
fi
if [ -n "$HTTPS_PORT" ]; then
	set -- "$@" --https-port "$HTTPS_PORT"
fi
"$TAYD_BIN" "$@"
touch "$INSTALL_DIR/.tayd-server"

if [ "$SKIP_START" != "1" ]; then
	"$TAYD_BIN" server restart
fi

"$TAYD_BIN" info
