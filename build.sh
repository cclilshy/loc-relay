#!/bin/sh
set -eu

RUST_IMAGE=${RUST_IMAGE:-rust:1.85-bookworm}

build_darwin() {
	command -v cargo >/dev/null 2>&1 || {
		echo "cargo is required to build darwin binaries" >&2
		exit 1
	}

	cargo build --release
	cp target/release/loc-relay bin/loc-relay-darwin-$(uname -m | sed 's/x86_64/amd64/;s/aarch64/arm64/')

	if rustup target list --installed | grep -F x86_64-apple-darwin >/dev/null 2>&1; then
		cargo build --release --target x86_64-apple-darwin
		cp target/x86_64-apple-darwin/release/loc-relay bin/loc-relay-darwin-amd64
	fi
}

build_linux() {
	platform=$1
	output=$2
	target_dir=$3

	docker run --rm \
		--platform "$platform" \
		-u "$(id -u):$(id -g)" \
		-e CARGO_HOME=/work/.cargo-home \
		-e CARGO_TARGET_DIR="/work/target/$target_dir" \
		-v "$PWD":/work \
		-w /work \
		"$RUST_IMAGE" \
		sh -c "cargo build --release && cp /work/target/$target_dir/release/loc-relay /work/$output"
}

build_windows_amd64() {
	uid=$(id -u)
	gid=$(id -g)

	docker run --rm \
		-e CARGO_HOME=/work/.cargo-home \
		-e CARGO_TARGET_DIR=/work/target/windows-amd64 \
		-e HOST_UID="$uid" \
		-e HOST_GID="$gid" \
		-v "$PWD":/work \
		-w /work \
		"$RUST_IMAGE" \
		sh -c "apt-get update && apt-get install -y --no-install-recommends gcc-mingw-w64-x86-64 && rustup target add x86_64-pc-windows-gnu && cargo build --release --target x86_64-pc-windows-gnu && cp /work/target/windows-amd64/x86_64-pc-windows-gnu/release/loc-relay.exe /work/bin/loc-relay-windows-amd64.exe && chown -R \"\$HOST_UID:\$HOST_GID\" /work/bin/loc-relay-windows-amd64.exe /work/target/windows-amd64 /work/.cargo-home"
}

mkdir -p bin target .cargo-home

if [ "$(uname -s)" = "Darwin" ]; then
	build_darwin
fi

build_linux linux/amd64 bin/loc-relay-linux-amd64 linux-amd64
build_linux linux/arm64/v8 bin/loc-relay-linux-arm64 linux-arm64
build_windows_amd64

chmod +x bin/loc-relay-*
