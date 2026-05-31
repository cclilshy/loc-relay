#!/bin/sh
set -eu

REPO_URL=${REPO_URL:-https://github.com/cclilshy/loc-relay.git}
if [ -z "${INSTALL_DIR:-}" ] && [ -z "${CLIENT_INSTALL_DIR:-}" ]; then
	if [ -d "$HOME/.tayd-client" ]; then
		INSTALL_DIR="$HOME/.tayd-client"
	elif [ -d "$HOME/.loc-relay-client" ]; then
		INSTALL_DIR="$HOME/.loc-relay-client"
	else
		INSTALL_DIR="$HOME/.tayd-client"
	fi
else
	INSTALL_DIR=${INSTALL_DIR:-$CLIENT_INSTALL_DIR}
fi
BIN_DIR=${BIN_DIR:-"$HOME/.local/bin"}
SERVER_ADDR=${1:-${SERVER_ADDR:-}}
TOKEN=${2:-${TOKEN:-}}
SERVER_PORT=${SERVER_PORT:-7000}
PROXY_NAME=${PROXY_NAME:-}
PROXY_TYPE=${PROXY_TYPE:-}
LOCAL_IP=${LOCAL_IP:-127.0.0.1}
LOCAL_PORT=${LOCAL_PORT:-}
LOCAL_ENDPOINT=${LOCAL_ENDPOINT:-}
REMOTE_PORT=${REMOTE_PORT:-}
SKIP_START=${SKIP_START:-0}

die() {
	echo "ERROR: $*" >&2
	exit 1
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

tayd_target() {
	os=$(uname -s | tr '[:upper:]' '[:lower:]')
	arch=$(uname -m)
	case "$arch" in
	x86_64 | amd64)
		arch=amd64
		;;
	arm64 | aarch64)
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

install_tayd_command() {
	tayd_bin=$1
	mkdir -p "$BIN_DIR"
	ln -sf "$tayd_bin" "$BIN_DIR/tayd"
	if [ -L "$BIN_DIR/loc-relay" ]; then
		rm -f "$BIN_DIR/loc-relay"
	fi

	case ":$PATH:" in
	*":$BIN_DIR:"*)
		return
		;;
	esac

	echo "Use $BIN_DIR/tayd when tayd is not in PATH."
}

[ -n "$SERVER_ADDR" ] || die "usage: install-client.sh <server_addr> <token>"
[ -n "$TOKEN" ] || die "usage: install-client.sh <server_addr> <token>"

clone_or_update

TAYD_BIN=$(select_tayd_binary)
INSTALL_DIR="$INSTALL_DIR" "$INSTALL_DIR/scripts/install-frp.sh"

"$TAYD_BIN" init \
	--server "$SERVER_ADDR" \
	--token "$TOKEN" \
	--server-port "$SERVER_PORT"

proxy_configured=0
if [ -n "$PROXY_NAME" ] || [ -n "$LOCAL_ENDPOINT" ] || [ -n "$LOCAL_PORT" ] || [ -n "$REMOTE_PORT" ]; then
	[ -n "$PROXY_NAME" ] || die "PROXY_NAME is required when configuring an initial mapping"
	[ -n "$REMOTE_PORT" ] || die "REMOTE_PORT is required when configuring an initial mapping"
	if [ -z "$LOCAL_ENDPOINT" ]; then
		[ -n "$LOCAL_PORT" ] || die "LOCAL_PORT or LOCAL_ENDPOINT is required when configuring an initial mapping"
		if [ "$LOCAL_IP" = "127.0.0.1" ]; then
			LOCAL_ENDPOINT=$LOCAL_PORT
		else
			LOCAL_ENDPOINT=$LOCAL_IP:$LOCAL_PORT
		fi
	fi

	"$TAYD_BIN" remove "$PROXY_NAME" --no-restart >/dev/null 2>&1 || true
	if [ -n "$PROXY_TYPE" ]; then
		"$TAYD_BIN" add "$PROXY_NAME" "$LOCAL_ENDPOINT" "$REMOTE_PORT" --type "$PROXY_TYPE" --no-restart
	else
		"$TAYD_BIN" add "$PROXY_NAME" "$LOCAL_ENDPOINT" "$REMOTE_PORT" --no-restart
	fi
	proxy_configured=1
fi

touch "$INSTALL_DIR/.tayd-client"
install_tayd_command "$TAYD_BIN"

if [ "$SKIP_START" != "1" ]; then
	"$TAYD_BIN" restart
	echo "[restarted] client gateway"
else
	echo "[saved] start skipped"
fi

echo "[installed] tayd client -> $INSTALL_DIR"
echo "[server] $SERVER_ADDR:$SERVER_PORT"
echo "[command] $BIN_DIR/tayd"
if [ "$proxy_configured" = "1" ]; then
	echo "[proxy] $PROXY_NAME $LOCAL_ENDPOINT -> $REMOTE_PORT"
fi
echo "[uninstall] $BIN_DIR/tayd uninstall"
