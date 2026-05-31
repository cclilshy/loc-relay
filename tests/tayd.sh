#!/bin/sh
set -eu

repo_root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)

if [ "${TAYD_TEST_IN_DOCKER:-0}" != "1" ]; then
	command -v docker >/dev/null 2>&1 || {
		echo "FAIL: docker is required" >&2
		exit 1
	}
	docker run --rm \
		-v "$repo_root":/work \
		-w /work \
		-e TAYD_TEST_IN_DOCKER=1 \
		"${RUST_IMAGE:-rust:1.85-bookworm}" \
		sh tests/tayd.sh
	exit $?
fi

tmp_dir=$(mktemp -d)
trap 'rm -rf "$tmp_dir"' EXIT INT TERM

fail() {
	echo "FAIL: $*" >&2
	exit 1
}

assert_contains() {
	file=$1
	text=$2

	grep -F "$text" "$file" >/dev/null 2>&1 || fail "$file does not contain: $text"
}

if grep -R "python3" "$repo_root/scripts" >/dev/null 2>&1; then
	fail "runtime scripts must not require python3"
fi

if grep -R "docker compose" "$repo_root/scripts" "$repo_root/src" >/dev/null 2>&1; then
	fail "runtime must not depend on docker compose"
fi

if [ -e "$repo_root/go.mod" ] || [ -d "$repo_root/cmd/tayd" ]; then
	fail "project must not keep Go tayd sources"
fi

[ -x "$repo_root/build.sh" ] || fail "build.sh must exist and be executable"
[ -x "$repo_root/bin/tayd-darwin-arm64" ] || fail "darwin arm64 binary must be published"
[ -x "$repo_root/bin/tayd-darwin-amd64" ] || fail "darwin amd64 binary must be published"
[ -f "$repo_root/scripts/install-client.ps1" ] || fail "Windows client installer must exist"
[ -f "$repo_root/android/app/src/main/res/drawable/ic_qr_scan_24.xml" ] || fail "Android scan icon must exist"
if find "$repo_root/android/app/src/main/java/com/cclilshy/locrelay" -maxdepth 1 -type f -name '*.java' | grep . >/dev/null 2>&1; then
	fail "Android Java sources must live in feature packages"
fi
[ -f "$repo_root/android/app/src/main/java/com/cclilshy/locrelay/qr/ui/QrScanActivity.java" ] || fail "Android QR scan activity must exist"
if grep -R "无窗口模式" "$repo_root/android/app/src/main/java" "$repo_root/android/app/src/main/res" >/dev/null 2>&1; then
	fail "Android UI still contains Chinese no-window label"
fi
[ -x "$repo_root/android/scripts/build-frpc.sh" ] || fail "Android frpc builder must exist"
android_frpc_plan=$(DRY_RUN=1 "$repo_root/android/scripts/build-frpc.sh")
printf '%s\n' "$android_frpc_plan" | grep -F 'arm64-v8a android/arm64' >/dev/null 2>&1 || fail "Android frpc plan must include arm64-v8a"
printf '%s\n' "$android_frpc_plan" | grep -F 'armeabi-v7a android/arm' >/dev/null 2>&1 || fail "Android frpc plan must include armeabi-v7a"
printf '%s\n' "$android_frpc_plan" | grep -F 'x86_64 android/amd64' >/dev/null 2>&1 || fail "Android frpc plan must include x86_64"
printf '%s\n' "$android_frpc_plan" | grep -F 'x86 android/386' >/dev/null 2>&1 || fail "Android frpc plan must include x86"
printf '%s\n' "$android_frpc_plan" | grep -F 'armeabi-v7a android/arm ndk' >/dev/null 2>&1 || fail "Android frpc plan must use NDK for armeabi-v7a"
printf '%s\n' "$android_frpc_plan" | grep -F 'x86_64 android/amd64 ndk' >/dev/null 2>&1 || fail "Android frpc plan must use NDK for x86_64"
printf '%s\n' "$android_frpc_plan" | grep -F 'x86 android/386 ndk' >/dev/null 2>&1 || fail "Android frpc plan must use NDK for x86"

case "$(uname -m)" in
	x86_64|amd64)
		published_linux_bin="$repo_root/bin/tayd-linux-amd64"
		;;
	aarch64|arm64)
		published_linux_bin="$repo_root/bin/tayd-linux-arm64"
		;;
	*)
		published_linux_bin=
		;;
esac
if [ -n "$published_linux_bin" ]; then
	TAYD_HOME="$tmp_dir/published-frps-info" "$published_linux_bin" init-server \
		--token server-token \
		--addr frp.example.com \
		--raw-base-url https://example.com/scripts
	assert_contains "$tmp_dir/published-frps-info/server-install.json" '"addr": "frp.example.com"'
	assert_contains "$tmp_dir/published-frps-info/server-install.json" '"raw_base_url": "https://example.com/scripts"'
fi

test_bin="$tmp_dir/tayd-bin"
cargo build --manifest-path "$repo_root/Cargo.toml" --target-dir "$tmp_dir/target" >/tmp/tayd-cargo-build.log
cp "$tmp_dir/target/debug/tayd" "$test_bin"

help_output=$("$test_bin" help)
printf '%s\n' "$help_output" | grep -F 'Usage:' >/dev/null 2>&1 || fail "help did not include Usage section"
printf '%s\n' "$help_output" | grep -F 'Commands:' >/dev/null 2>&1 || fail "help did not include Commands section"
printf '%s\n' "$help_output" | grep -F 'Endpoint forms:' >/dev/null 2>&1 || fail "help did not include Endpoint forms section"
printf '%s\n' "$help_output" | grep -F 'Protocol selection:' >/dev/null 2>&1 || fail "help did not include Protocol selection section"
printf '%s\n' "$help_output" | grep -F 'Options:' >/dev/null 2>&1 || fail "help did not include Options section"
printf '%s\n' "$help_output" | grep -F 'Examples:' >/dev/null 2>&1 || fail "help did not include Examples section"
printf '%s\n' "$help_output" | grep -F 'tayd add <name> <local> <remote>' >/dev/null 2>&1 || fail "help did not include add synopsis"
printf '%s\n' "$help_output" | grep -F 'local:  [tcp://|udp://|http://|https://][host:]port' >/dev/null 2>&1 || fail "help did not explain local endpoint forms"
printf '%s\n' "$help_output" | grep -F 'remote: port | tcp://host:port | udp://host:port | http://domain[:port] | https://domain[:port]' >/dev/null 2>&1 || fail "help did not explain remote endpoint forms"
printf '%s\n' "$help_output" | grep -F 'remote scheme selects protocol when present' >/dev/null 2>&1 || fail "help did not explain remote scheme priority"
printf '%s\n' "$help_output" | grep -F 'local tcp:// or udp:// selects protocol when remote is a bare port' >/dev/null 2>&1 || fail "help did not explain local scheme inference"
printf '%s\n' "$help_output" | grep -F 'no scheme defaults to tcp' >/dev/null 2>&1 || fail "help did not explain default protocol"
printf '%s\n' "$help_output" | grep -F 'use --type only for bare-port udp or tcp+udp mappings' >/dev/null 2>&1 || fail "help did not explain when type is required"
printf '%s\n' "$help_output" | grep -F 'add options:' >/dev/null 2>&1 || fail "help did not group add options"
printf '%s\n' "$help_output" | grep -F 'tayd install' >/dev/null 2>&1 || fail "help did not include install"
printf '%s\n' "$help_output" | grep -F 'tayd uninstall' >/dev/null 2>&1 || fail "help did not include uninstall"
printf '%s\n' "$help_output" | grep -F 'tayd server install' >/dev/null 2>&1 || fail "help did not include server install"
printf '%s\n' "$help_output" | grep -F 'tayd server uninstall' >/dev/null 2>&1 || fail "help did not include server uninstall"
printf '%s\n' "$help_output" | grep -F 'tayd server restart' >/dev/null 2>&1 || fail "help did not include server restart"
printf '%s\n' "$help_output" | grep -F 'tayd server info' >/dev/null 2>&1 || fail "help did not include server info"
printf '%s\n' "$help_output" | grep -F 'tayd server log' >/dev/null 2>&1 || fail "help did not include server log"
printf '%s\n' "$help_output" | grep -F -- '--group name --group-key key' >/dev/null 2>&1 || fail "help did not include group options"
if printf '%s\n' "$help_output" | grep -F 'tayd up' >/dev/null 2>&1; then
	fail "help still included up"
fi
if printf '%s\n' "$help_output" | grep -F 'tayd down' >/dev/null 2>&1; then
	fail "help still included down"
fi
if printf '%s\n' "$help_output" | grep -F 'server-up' >/dev/null 2>&1; then
	fail "help still included server-up"
fi
if printf '%s\n' "$help_output" | grep -F 'server-down' >/dev/null 2>&1; then
	fail "help still included server-down"
fi
add_line=$(printf '%s\n' "$help_output" | awk '/tayd add / { print NR; exit }')
init_line=$(printf '%s\n' "$help_output" | awk '/tayd init --server/ { print NR; exit }')
[ -n "$add_line" ] || fail "help did not include add"
[ -n "$init_line" ] || fail "help did not include init"
[ "$init_line" -gt "$add_line" ] || fail "init should be lower than add in help"

TAYD_HOME="$tmp_dir/tayd" "$test_bin" init \
	--server frp.example.com \
	--token test-token

assert_contains "$tmp_dir/tayd/frpc.toml" 'serverAddr = "frp.example.com"'
assert_contains "$tmp_dir/tayd/frpc.toml" 'includes = ["frpc.d/*.toml"]'
[ ! -e "$tmp_dir/tayd/frpc.d/ssh.toml" ] || fail "init created a default ssh mapping"

empty_list_output=$(TAYD_HOME="$tmp_dir/tayd" "$test_bin" list)
printf '%s\n' "$empty_list_output" | grep -F 'no mappings' >/dev/null 2>&1 || fail "empty list did not report no mappings"

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add web 8080 18080
assert_contains "$tmp_dir/tayd/frpc.d/web.toml" 'name = "web"'
assert_contains "$tmp_dir/tayd/frpc.d/web.toml" 'type = "tcp"'
assert_contains "$tmp_dir/tayd/frpc.d/web.toml" 'localIP = "127.0.0.1"'
assert_contains "$tmp_dir/tayd/frpc.d/web.toml" 'localPort = 8080'
assert_contains "$tmp_dir/tayd/frpc.d/web.toml" 'remotePort = 18080'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add web-blue 8083 18080 --group web --group-key shard-a
assert_contains "$tmp_dir/tayd/frpc.d/web-blue.toml" 'name = "web-blue"'
assert_contains "$tmp_dir/tayd/frpc.d/web-blue.toml" 'type = "tcp"'
assert_contains "$tmp_dir/tayd/frpc.d/web-blue.toml" 'remotePort = 18080'
assert_contains "$tmp_dir/tayd/frpc.d/web-blue.toml" 'loadBalancer.group = "web"'
assert_contains "$tmp_dir/tayd/frpc.d/web-blue.toml" 'loadBalancer.groupKey = "shard-a"'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add game 0.0.0.0:8680 2929 --type both
assert_contains "$tmp_dir/tayd/frpc.d/game.toml" 'name = "game-tcp"'
assert_contains "$tmp_dir/tayd/frpc.d/game.toml" 'type = "tcp"'
assert_contains "$tmp_dir/tayd/frpc.d/game.toml" 'name = "game-udp"'
assert_contains "$tmp_dir/tayd/frpc.d/game.toml" 'type = "udp"'
assert_contains "$tmp_dir/tayd/frpc.d/game.toml" 'localIP = "0.0.0.0"'
assert_contains "$tmp_dir/tayd/frpc.d/game.toml" 'localPort = 8680'
assert_contains "$tmp_dir/tayd/frpc.d/game.toml" 'remotePort = 2929'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add dns udp://:5353 5353
assert_contains "$tmp_dir/tayd/frpc.d/dns.toml" 'name = "dns"'
assert_contains "$tmp_dir/tayd/frpc.d/dns.toml" 'type = "udp"'
assert_contains "$tmp_dir/tayd/frpc.d/dns.toml" 'localIP = "127.0.0.1"'
assert_contains "$tmp_dir/tayd/frpc.d/dns.toml" 'localPort = 5353'
assert_contains "$tmp_dir/tayd/frpc.d/dns.toml" 'remotePort = 5353'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add blog 3000 https://blog.example.com
assert_contains "$tmp_dir/tayd/frpc.d/blog.toml" 'name = "blog"'
assert_contains "$tmp_dir/tayd/frpc.d/blog.toml" 'type = "https"'
assert_contains "$tmp_dir/tayd/frpc.d/blog.toml" 'localIP = "127.0.0.1"'
assert_contains "$tmp_dir/tayd/frpc.d/blog.toml" 'localPort = 3000'
assert_contains "$tmp_dir/tayd/frpc.d/blog.toml" 'customDomains = ["blog.example.com"]'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add blog-blue 3001 https://blog.example.com --group blog --group-key shard-a
assert_contains "$tmp_dir/tayd/frpc.d/blog-blue.toml" 'name = "blog-blue"'
assert_contains "$tmp_dir/tayd/frpc.d/blog-blue.toml" 'type = "https"'
assert_contains "$tmp_dir/tayd/frpc.d/blog-blue.toml" 'customDomains = ["blog.example.com"]'
assert_contains "$tmp_dir/tayd/frpc.d/blog-blue.toml" 'loadBalancer.group = "blog"'
assert_contains "$tmp_dir/tayd/frpc.d/blog-blue.toml" 'loadBalancer.groupKey = "shard-a"'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add site 3002 http://site.example.com --group site --group-key shard-a
assert_contains "$tmp_dir/tayd/frpc.d/site.toml" 'name = "site"'
assert_contains "$tmp_dir/tayd/frpc.d/site.toml" 'type = "http"'
assert_contains "$tmp_dir/tayd/frpc.d/site.toml" 'customDomains = ["site.example.com"]'
assert_contains "$tmp_dir/tayd/frpc.d/site.toml" 'loadBalancer.group = "site"'
assert_contains "$tmp_dir/tayd/frpc.d/site.toml" 'loadBalancer.groupKey = "shard-a"'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add blog-http http://3080 https://blog-http.example.com --crt certs/blog.crt --key certs/blog.key
assert_contains "$tmp_dir/tayd/frpc.d/blog-http.toml" 'name = "blog-http"'
assert_contains "$tmp_dir/tayd/frpc.d/blog-http.toml" 'type = "https"'
assert_contains "$tmp_dir/tayd/frpc.d/blog-http.toml" 'customDomains = ["blog-http.example.com"]'
assert_contains "$tmp_dir/tayd/frpc.d/blog-http.toml" '[proxies.plugin]'
assert_contains "$tmp_dir/tayd/frpc.d/blog-http.toml" 'type = "https2http"'
assert_contains "$tmp_dir/tayd/frpc.d/blog-http.toml" 'localAddr = "127.0.0.1:3080"'
assert_contains "$tmp_dir/tayd/frpc.d/blog-http.toml" 'crtPath = "certs/blog.crt"'
assert_contains "$tmp_dir/tayd/frpc.d/blog-http.toml" 'keyPath = "certs/blog.key"'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add baidu https://www.baidu.com:443 https://baidu-proxy.example.com:443 --crt certs/baidu.crt --key certs/baidu.key
assert_contains "$tmp_dir/tayd/frpc.d/baidu.toml" 'name = "baidu"'
assert_contains "$tmp_dir/tayd/frpc.d/baidu.toml" 'type = "https"'
assert_contains "$tmp_dir/tayd/frpc.d/baidu.toml" 'customDomains = ["baidu-proxy.example.com"]'
assert_contains "$tmp_dir/tayd/frpc.d/baidu.toml" 'type = "https2https"'
assert_contains "$tmp_dir/tayd/frpc.d/baidu.toml" 'localAddr = "www.baidu.com:443"'
assert_contains "$tmp_dir/tayd/frpc.d/baidu.toml" 'hostHeaderRewrite = "www.baidu.com"'
assert_contains "$tmp_dir/tayd/frpc.d/baidu.toml" 'crtPath = "certs/baidu.crt"'
assert_contains "$tmp_dir/tayd/frpc.d/baidu.toml" 'keyPath = "certs/baidu.key"'

if TAYD_HOME="$tmp_dir/tayd" "$test_bin" add ambiguous-cert 3090 https://ambiguous.example.com --crt certs/a.crt --key certs/a.key >/tmp/tayd-ambiguous-cert.log 2>&1; then
	fail "--crt/--key with local port but no local scheme was accepted"
fi

if TAYD_HOME="$tmp_dir/tayd" "$test_bin" add invalid tcp+udp://8681 18181 >/tmp/tayd-invalid-type.log 2>&1; then
	fail "tcp+udp:// endpoint was accepted"
fi

if TAYD_HOME="$tmp_dir/tayd" "$test_bin" add bad-group-key 8084 18084 --group-key lonely >/tmp/tayd-bad-group-key.log 2>&1; then
	fail "--group-key without --group was accepted"
fi

if TAYD_HOME="$tmp_dir/tayd" "$test_bin" add dns-group udp://5354 5354 --group dns >/tmp/tayd-bad-udp-group.log 2>&1; then
	fail "udp group was accepted"
fi

list_output=$(TAYD_HOME="$tmp_dir/tayd" "$test_bin" list)
printf '%s\n' "$list_output" | grep -F 'baidu https://baidu-proxy.example.com:443 -> https://www.baidu.com:443' >/dev/null 2>&1 || fail "list did not include baidu"
printf '%s\n' "$list_output" | grep -F 'blog https://blog.example.com -> 127.0.0.1:3000' >/dev/null 2>&1 || fail "list did not include blog"
printf '%s\n' "$list_output" | grep -F 'blog-blue https://blog.example.com -> 127.0.0.1:3001' >/dev/null 2>&1 || fail "list did not include blog-blue"
printf '%s\n' "$list_output" | grep -F 'blog-http https://blog-http.example.com -> http://127.0.0.1:3080' >/dev/null 2>&1 || fail "list did not include blog-http"
printf '%s\n' "$list_output" | grep -F 'dns udp://frp.example.com:5353 -> 127.0.0.1:5353' >/dev/null 2>&1 || fail "list did not include dns"
printf '%s\n' "$list_output" | grep -F 'game tcp,udp frp.example.com:2929 -> 0.0.0.0:8680' >/dev/null 2>&1 || fail "list did not include game"
printf '%s\n' "$list_output" | grep -F 'site http://site.example.com -> 127.0.0.1:3002' >/dev/null 2>&1 || fail "list did not include site"
printf '%s\n' "$list_output" | grep -F 'web tcp://frp.example.com:18080 -> 127.0.0.1:8080' >/dev/null 2>&1 || fail "list did not include web"
printf '%s\n' "$list_output" | grep -F 'web-blue tcp://frp.example.com:18080 -> 127.0.0.1:8083' >/dev/null 2>&1 || fail "list did not include web-blue"

show_output=$(TAYD_HOME="$tmp_dir/tayd" "$test_bin" show web)
printf '%s\n' "$show_output" | grep -F 'remotePort = 18080' >/dev/null 2>&1 || fail "show did not print web config"

if TAYD_HOME="$tmp_dir/tayd" "$test_bin" add web 8081 18081 >/tmp/tayd-duplicate.log 2>&1; then
	fail "duplicate proxy name was accepted"
fi

TAYD_HOME="$tmp_dir/tayd" "$test_bin" add web-udp 8081 18080 --type udp
assert_contains "$tmp_dir/tayd/frpc.d/web-udp.toml" 'type = "udp"'
TAYD_HOME="$tmp_dir/tayd" "$test_bin" add conflict 8082 18080 --type tcp
assert_contains "$tmp_dir/tayd/frpc.d/conflict.toml" 'remotePort = 18080'
TAYD_HOME="$tmp_dir/tayd" "$test_bin" add udp-conflict 8082 18080 --type udp
assert_contains "$tmp_dir/tayd/frpc.d/udp-conflict.toml" 'remotePort = 18080'

TAYD_HOME="$tmp_dir/tayd" "$test_bin" remove web
[ ! -e "$tmp_dir/tayd/frpc.d/web.toml" ] || fail "remove did not delete web"
[ -e "$tmp_dir/tayd/frpc.d/game.toml" ] || fail "remove deleted game"

service_home="$tmp_dir/service-home"
mkdir -p "$service_home"
HOME="$service_home" TAYD_HOME="$tmp_dir/tayd" TAYD_SERVICE_SKIP_ENABLE=1 "$test_bin" service install
[ -e "$service_home/.config/systemd/user/tayd-frpc.service" ] || fail "service install did not write systemd user unit"
assert_contains "$service_home/.config/systemd/user/tayd-frpc.service" 'ExecStart='
service_status=$(HOME="$service_home" TAYD_HOME="$tmp_dir/tayd" "$test_bin" service status)
printf '%s\n' "$service_status" | grep -F 'installed:' >/dev/null 2>&1 || fail "service status did not report installed"
HOME="$service_home" TAYD_HOME="$tmp_dir/tayd" TAYD_SERVICE_SKIP_ENABLE=1 "$test_bin" service uninstall
[ ! -e "$service_home/.config/systemd/user/tayd-frpc.service" ] || fail "service uninstall did not remove systemd user unit"

TAYD_HOME="$tmp_dir/frps-vhost" "$test_bin" init-server --token server-token --http-port 8080 --https-port 8443
assert_contains "$tmp_dir/frps-vhost/frps.toml" 'vhostHTTPPort = 8080'
assert_contains "$tmp_dir/frps-vhost/frps.toml" 'vhostHTTPSPort = 8443'

TAYD_HOME="$tmp_dir/frps-info" "$test_bin" init-server \
	--token server-token \
	--addr frp.example.com \
	--raw-base-url https://example.com/scripts \
	--http-port 8080 \
	--https-port 8443
assert_contains "$tmp_dir/frps-info/server-install.json" '"addr": "frp.example.com"'
assert_contains "$tmp_dir/frps-info/server-install.json" '"token": "server-token"'
server_info=$(TAYD_HOME="$tmp_dir/frps-info" "$test_bin" server info)
printf '%s\n' "$server_info" | grep -F 'Server: frp.example.com:7000' >/dev/null 2>&1 || fail "server info did not include server address"
printf '%s\n' "$server_info" | grep -F 'Token: server-token' >/dev/null 2>&1 || fail "server info did not include token"
printf '%s\n' "$server_info" | grep -F 'QR payload: tayd://server?addr=frp.example.com&port=7000&token=server-token' >/dev/null 2>&1 || fail "server info did not include QR payload"
printf '%s\n' "$server_info" | grep -F 'curl -fsSL https://example.com/scripts/install-client.sh | sh -s -- frp.example.com server-token' >/dev/null 2>&1 || fail "server info did not include client install command"
info_output=$(TAYD_HOME="$tmp_dir/frps-info" "$test_bin" info)
printf '%s\n' "$info_output" | grep -F 'Token: server-token' >/dev/null 2>&1 || fail "top-level info did not include token"
server_log=$(TAYD_HOME="$tmp_dir/frps-info" "$test_bin" server log)
printf '%s\n' "$server_log" | grep -F 'Token: server-token' >/dev/null 2>&1 || fail "server log did not include token"

src="$tmp_dir/source"
home="$tmp_dir/home"
install_dir="$tmp_dir/install"
bin_dir="$home/.local/bin"
mkdir -p "$src" "$home"
cp -R "$repo_root/." "$src"
rm -rf "$src/.git"
(cd "$src" && cargo build --target-dir "$tmp_dir/source-target" >/tmp/tayd-source-cargo-build.log)
mkdir -p "$src/bin"
cp "$tmp_dir/source-target/debug/tayd" "$src/bin/tayd-$(uname -s | tr '[:upper:]' '[:lower:]')-$(uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/')"
cat >"$src/bin/frpc" <<'EOF'
#!/bin/sh
exit 0
EOF
cat >"$src/bin/frps" <<'EOF'
#!/bin/sh
exit 0
EOF
chmod +x "$src/bin/frpc" "$src/bin/frps"
git -C "$src" init -q
git -C "$src" add .
git -C "$src" -c user.name=test -c user.email=test@example.com commit -q -m init

default_home="$tmp_dir/default-home"
default_bin_dir="$default_home/.local/bin"
mkdir -p "$default_home"

HOME="$default_home" \
	BIN_DIR="$default_bin_dir" \
	REPO_URL="$src" \
	SKIP_START=1 \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-client.sh" 198.51.100.10 default-token >/tmp/tayd-default-client.log

default_client_dir="$default_home/.tayd-client"
[ -e "$default_client_dir/.tayd-client" ] || fail "default client install marker was not created"
assert_contains "$default_client_dir/frpc.toml" 'serverAddr = "198.51.100.10"'
[ ! -e "$default_client_dir/frpc.d/ssh.toml" ] || fail "default client install created a default ssh mapping"
default_client_list=$(HOME="$default_home" "$default_bin_dir/tayd" list)
printf '%s\n' "$default_client_list" | grep -F 'no mappings' >/dev/null 2>&1 || fail "default client list did not report no mappings"

HOME="$default_home" \
	REPO_URL="$src" \
	SKIP_START=1 \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-server.sh" frp.default.example >"/tmp/tayd-default-server.log"

default_server_dir="$default_home/.tayd-server"
[ -e "$default_server_dir/.tayd-server" ] || fail "default server install marker was not created"
assert_contains "$default_server_dir/frps.toml" 'auth.token = '
[ -e "$default_client_dir/frpc.toml" ] || fail "default server install clobbered client config"
[ ! -e "$default_home/.tayd" ] || fail "default installers still wrote to shared ~/.tayd"

HOME="$home" \
	INSTALL_DIR="$install_dir" \
	BIN_DIR="$bin_dir" \
	REPO_URL="$src" \
	PROXY_NAME=web \
	LOCAL_PORT=8080 \
	REMOTE_PORT=18080 \
	SKIP_START=1 \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-client.sh" 203.0.113.10 test-token >/tmp/tayd-install-client.log

[ -e "$bin_dir/tayd" ] || fail "installer did not create tayd command"
assert_contains "$install_dir/frpc.toml" 'serverAddr = "203.0.113.10"'
assert_contains "$install_dir/frpc.d/web.toml" 'remotePort = 18080'
assert_contains /tmp/tayd-install-client.log "[uninstall] $bin_dir/tayd uninstall"

installed_list=$(HOME="$home" "$bin_dir/tayd" list)
printf '%s\n' "$installed_list" | grep -F 'web tcp://203.0.113.10:18080 -> 127.0.0.1:8080' >/dev/null 2>&1 || fail "installed tayd list did not include web"

HOME="$home" \
	INSTALL_DIR="$install_dir" \
	BIN_DIR="$bin_dir" \
	REPO_URL="$src" \
	PROXY_NAME=web \
	LOCAL_PORT=8080 \
	REMOTE_PORT=18081 \
	SKIP_START=1 \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-client.sh" 203.0.113.11 next-token >/tmp/tayd-update-client.log
assert_contains "$install_dir/frpc.toml" 'serverAddr = "203.0.113.11"'
assert_contains "$install_dir/frpc.toml" 'auth.token = "next-token"'
assert_contains "$install_dir/frpc.d/web.toml" 'remotePort = 18081'

HOME="$home" TAYD_SKIP_STOP=1 "$bin_dir/tayd" uninstall >/tmp/tayd-uninstall.log
[ ! -e "$install_dir" ] || fail "tayd uninstall did not remove install dir"
[ ! -e "$bin_dir/tayd" ] || fail "tayd uninstall did not remove tayd symlink"

server_dir="$tmp_dir/server"
server_output="$tmp_dir/server.out"
RAW_BASE_URL="https://example.com/scripts" \
	REPO_URL="$src" \
	INSTALL_DIR="$server_dir" \
	SKIP_START=1 \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-server.sh" frp.example.com --http-port 8080 --https-port 8443 >"$server_output"

server_token=$(sed -n 's/^Token: //p' "$server_output")
[ -n "$server_token" ] || fail "server installer did not print token"
assert_contains "$server_dir/frps.toml" "auth.token = \"$server_token\""
assert_contains "$server_dir/frps.toml" 'vhostHTTPPort = 8080'
assert_contains "$server_dir/frps.toml" 'vhostHTTPSPort = 8443'
assert_contains "$server_dir/server-install.json" '"addr": "frp.example.com"'
assert_contains "$server_dir/server-install.json" '"http_port": 8080'
assert_contains "$server_dir/server-install.json" '"https_port": 8443'
assert_contains "$server_output" "Client install commands:"
assert_contains "$server_output" "QR payload: tayd://server?addr=frp.example.com&port=7000&token=$server_token"
assert_contains "$server_output" "Linux/macOS:"
assert_contains "$server_output" "curl -fsSL https://example.com/scripts/install-client.sh | sh -s -- frp.example.com $server_token"
assert_contains "$server_output" "Windows PowerShell:"
assert_contains "$server_output" "Invoke-RestMethod 'https://example.com/scripts/install-client.ps1'"
assert_contains "$server_output" "initialized server"

RAW_BASE_URL="https://example.com/scripts" \
	REPO_URL="$src" \
	INSTALL_DIR="$server_dir" \
	SKIP_START=1 \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-server.sh" frp.example.com --http-port 8080 --https-port 8443 >"$tmp_dir/server-update.out"
updated_server_token=$(sed -n 's/^Token: //p' "$tmp_dir/server-update.out")
[ "$updated_server_token" = "$server_token" ] || fail "server update did not preserve token"

running_server_dir="$tmp_dir/server-running"
RAW_BASE_URL="https://example.com/scripts" \
	REPO_URL="$src" \
	INSTALL_DIR="$running_server_dir" \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-server.sh" frp.running.example >"$tmp_dir/server-running-first.out"
running_server_token=$(sed -n 's/^Token: //p' "$tmp_dir/server-running-first.out")
RAW_BASE_URL="https://example.com/scripts" \
	REPO_URL="$src" \
	INSTALL_DIR="$running_server_dir" \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-server.sh" frp.running.example >"$tmp_dir/server-running-second.out"
updated_running_server_token=$(sed -n 's/^Token: //p' "$tmp_dir/server-running-second.out")
[ "$updated_running_server_token" = "$running_server_token" ] || fail "running server update did not preserve token"

legacy_server_dir="$tmp_dir/server-legacy"
RAW_BASE_URL="https://example.com/scripts" \
	REPO_URL="$src" \
	INSTALL_DIR="$legacy_server_dir" \
	HTTP_PORT=8081 \
	HTTPS_PORT=8444 \
	SKIP_START=1 \
	SKIP_FRP_DOWNLOAD=1 \
	sh "$repo_root/scripts/install-server.sh" legacy.example >/tmp/tayd-legacy-server.out
assert_contains "$legacy_server_dir/frps.toml" 'vhostHTTPPort = 8081'
assert_contains "$legacy_server_dir/frps.toml" 'vhostHTTPSPort = 8444'

echo "tayd tests passed"
