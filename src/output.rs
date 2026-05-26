use crate::{
    endpoint::proxy_types,
    model::{Proxy, Server, ServerInstallInfo},
    Result,
};
use qrcode::{Color as QrColor, QrCode};
use std::{
    env,
    io::{self, IsTerminal},
};

const DEFAULT_RAW_BASE_URL: &str =
    "https://raw.githubusercontent.com/cclilshy/loc-relay/main/scripts";

fn type_label(types: &[String]) -> String {
    types.join(",")
}

pub(crate) fn format_proxy_route(server: &Server, proxy: &Proxy) -> Result<String> {
    let types = proxy_types(proxy)?;
    let remote = if let (Some(scheme), Some(domain)) = (
        proxy.remote_scheme.as_deref(),
        proxy.remote_domain.as_deref(),
    ) {
        let port = proxy
            .remote_display_port
            .map(|port| format!(":{port}"))
            .unwrap_or_default();
        format!("{scheme}://{domain}{port}")
    } else if types.len() == 1 {
        format!(
            "{}://{}:{}",
            types[0],
            server.addr,
            proxy
                .remote_port
                .ok_or_else(|| format!("proxy {} remote_port is required", proxy.name))?
        )
    } else {
        format!(
            "{} {}:{}",
            type_label(&types),
            server.addr,
            proxy
                .remote_port
                .ok_or_else(|| format!("proxy {} remote_port is required", proxy.name))?
        )
    };
    Ok(format!("{} -> {}", remote, format_local_route(proxy)))
}

fn format_local_route(proxy: &Proxy) -> String {
    match proxy.local_scheme.as_deref() {
        Some("http") | Some("https") => format!(
            "{}://{}:{}",
            proxy.local_scheme.as_deref().unwrap_or_default(),
            proxy.local_ip,
            proxy.local_port
        ),
        _ => format!("{}:{}", proxy.local_ip, proxy.local_port),
    }
}

pub(crate) fn status(label: &str) -> String {
    let text = format!("[{label}]");
    if io::stdout().is_terminal() && env::var_os("NO_COLOR").is_none() {
        format!("\x1b[32m{text}\x1b[0m")
    } else {
        text
    }
}

pub(crate) fn print_server_install_info(info: &ServerInstallInfo) -> Result<()> {
    let raw_base_url = info.raw_base_url.as_deref().unwrap_or(DEFAULT_RAW_BASE_URL);
    let payload = server_qr_payload(info);
    println!("Server: {}:{}", info.addr, info.port);
    println!("Token: {}", info.token);
    if let Some(port) = info.http_port {
        println!("HTTP vhost: {port}");
    }
    if let Some(port) = info.https_port {
        println!("HTTPS vhost: {port}");
    }
    println!();
    println!("Client install commands:");
    println!();
    println!("Linux/macOS:");
    println!(
        "curl -fsSL {raw_base_url}/install-client.sh | sh -s -- {} {}",
        info.addr, info.token
    );
    println!();
    println!("Windows PowerShell:");
    println!(
        "powershell -NoProfile -ExecutionPolicy Bypass -Command \"& ([scriptblock]::Create((Invoke-RestMethod '{raw_base_url}/install-client.ps1'))) -ServerAddr '{}' -Token '{}'\"",
        info.addr, info.token
    );
    println!();
    println!("Android scan:");
    println!("QR payload: {payload}");
    println!("{}", render_terminal_qr(&payload)?);
    Ok(())
}

fn server_qr_payload(info: &ServerInstallInfo) -> String {
    format!(
        "loc-relay://server?addr={}&port={}&token={}",
        url_encode(&info.addr),
        info.port,
        url_encode(&info.token)
    )
}

fn render_terminal_qr(payload: &str) -> Result<String> {
    let code = QrCode::new(payload.as_bytes())?;
    let width = code.width();
    let colors = code.to_colors();
    let quiet = 2isize;
    let mut output = String::new();
    for y in -quiet..(width as isize + quiet) {
        for x in -quiet..(width as isize + quiet) {
            let dark = if x >= 0 && y >= 0 && x < width as isize && y < width as isize {
                colors[y as usize * width + x as usize] == QrColor::Dark
            } else {
                false
            };
            output.push_str(if dark {
                "\x1b[40m  \x1b[0m"
            } else {
                "\x1b[47m  \x1b[0m"
            });
        }
        output.push('\n');
    }
    Ok(output)
}

fn url_encode(value: &str) -> String {
    let mut encoded = String::new();
    for byte in value.bytes() {
        if byte.is_ascii_alphanumeric() || matches!(byte, b'-' | b'.' | b'_' | b'~') {
            encoded.push(byte as char);
        } else {
            encoded.push_str(&format!("%{byte:02X}"));
        }
    }
    encoded
}

pub(crate) fn usage() {
    println!(
        r#"loc-relay

Usage:
  loc-relay <command> [arguments] [options]
  loc-relay add <name> <local> <remote> [options]

Commands:
  loc-relay add <name> <local> <remote>     Add a client mapping
  loc-relay remove <name>                   Remove a mapping
  loc-relay list                            List mappings
  loc-relay show [name]                     Show rendered proxy config
  loc-relay render                          Rebuild frpc config files
  loc-relay verify                          Verify frpc config with frpc
  loc-relay restart                         Restart the client gateway
  loc-relay install                         Start the client gateway
  loc-relay uninstall                       Remove the local client install
  loc-relay service install                 Install auto-start service
  loc-relay service status                  Show auto-start service status
  loc-relay service uninstall               Remove auto-start service
  loc-relay server install                  Start the server gateway
  loc-relay server uninstall                Stop the server gateway
  loc-relay server info                     Show server token and QR
  loc-relay server log                      Show server install log
  loc-relay init --server <addr> --token <token>
                                           Initialize client state
  loc-relay init-server --token <token>     Initialize server state

Endpoint forms:
  local:  [tcp://|udp://|http://|https://][host:]port
  remote: port | tcp://host:port | udp://host:port | http://domain[:port] | https://domain[:port]

Protocol selection:
  remote scheme selects protocol when present
  local tcp:// or udp:// selects protocol when remote is a bare port
  no scheme defaults to tcp
  use --type only for bare-port udp or tcp+udp mappings

Options:
  add options:
    --type tcp|udp|both|tcp,udp|http|https  Set protocol for bare-port mappings
    --local-ip ip                           Override local host for bare local ports
    --group name --group-key key            Join a frp load balancing group
    --crt path --key path                   Enable https2http or https2https plugin
    --no-restart                            Save config without restarting frpc

  init options:
    --server-port 7000                      frps bind port used by frpc

  init-server options:
    --port 7000                             frps bind port
    --addr host                             Public server address for server info
    --http-port 80                          frps HTTP vhost port
    --https-port 443                        frps HTTPS vhost port
    --raw-base-url url                      Installer script base URL

Examples:
  loc-relay init --server frp.example.com --token <token>
  loc-relay add web 8080 18080
  loc-relay add dns udp://5353 5353
  loc-relay add game 8680 2929 --type both
  loc-relay add site 3000 http://site.example.com
  loc-relay add blog 3000 https://blog.example.com --group blog --group-key shard-a
  loc-relay add app http://3000 https://app.example.com --crt fullchain.pem --key privkey.pem"#
    );
}
