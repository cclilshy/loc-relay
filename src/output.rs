use crate::{
    endpoint::proxy_types,
    model::{Proxy, Server, ServerInstallInfo},
    Result,
};
use qrcode::{Color as QrColor, EcLevel, QrCode};
use std::{
    env,
    io::{self, IsTerminal},
};

const DEFAULT_RAW_BASE_URL: &str = "https://raw.githubusercontent.com/cclilshy/tayd/main/scripts";
const TERMINAL_QR_QUIET_ZONE: isize = 4;
const TERMINAL_QR_STYLE: &str = "\x1b[30;47m";
const TERMINAL_QR_RESET: &str = "\x1b[0m";

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
        "tayd://server?addr={}&port={}&token={}",
        url_encode(&info.addr),
        info.port,
        url_encode(&info.token)
    )
}

fn render_terminal_qr(payload: &str) -> Result<String> {
    let code = terminal_qr_code(payload)?;
    let width = code.width();
    let colors = code.to_colors();
    let quiet = TERMINAL_QR_QUIET_ZONE;
    let right_quiet = quiet + terminal_qr_extra_right_quiet(width);
    let mut output = String::new();

    for top_y in (-quiet..(width as isize + quiet)).step_by(2) {
        output.push_str(TERMINAL_QR_STYLE);
        for x in -quiet..(width as isize + right_quiet) {
            let top_dark = terminal_qr_module_is_dark(&colors, width, x, top_y);
            let bottom_dark = terminal_qr_module_is_dark(&colors, width, x, top_y + 1);
            output.push(match (top_dark, bottom_dark) {
                (true, true) => '█',
                (true, false) => '▀',
                (false, true) => '▄',
                (false, false) => ' ',
            });
        }
        output.push_str(TERMINAL_QR_RESET);
        output.push('\n');
    }

    Ok(output)
}

fn terminal_qr_code(payload: &str) -> Result<QrCode> {
    // Terminal output is high-contrast; low correction keeps install QR codes compact.
    Ok(QrCode::with_error_correction_level(
        payload.as_bytes(),
        EcLevel::L,
    )?)
}

fn terminal_qr_text_width(code_width: usize) -> usize {
    let standard_width = code_width + (TERMINAL_QR_QUIET_ZONE as usize * 2);
    // Half-block rendering packs two module rows into one terminal row, so an
    // odd QR width needs one extra quiet column to keep the rendered shape even.
    if standard_width % 2 == 0 {
        standard_width
    } else {
        standard_width + 1
    }
}

fn terminal_qr_extra_right_quiet(code_width: usize) -> isize {
    (terminal_qr_text_width(code_width) - code_width - (TERMINAL_QR_QUIET_ZONE as usize * 2))
        as isize
}

fn terminal_qr_module_is_dark(colors: &[QrColor], width: usize, x: isize, y: isize) -> bool {
    if x >= 0 && y >= 0 && x < width as isize && y < width as isize {
        colors[y as usize * width + x as usize] == QrColor::Dark
    } else {
        false
    }
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

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn terminal_qr_renders_compact_square_with_standard_quiet_zone() {
        let payload =
            "tayd://server?addr=frp.example.com&port=7000&token=0123456789abcdef0123456789abcdef0123456789abcdef";
        let qr = render_terminal_qr(payload).expect("QR should render");
        let code = terminal_qr_code(payload).expect("payload should be encodable");
        let module_height = code.width() + (TERMINAL_QR_QUIET_ZONE as usize * 2);
        let text_width = terminal_qr_text_width(code.width());
        let lines: Vec<&str> = qr.lines().collect();

        assert_eq!(lines.len(), module_height.div_ceil(2));
        assert_eq!(text_width, lines.len() * 2);
        for line in &lines {
            assert!(line.starts_with(TERMINAL_QR_STYLE));
            assert!(line.ends_with(TERMINAL_QR_RESET));
            assert_eq!(strip_ansi(line).chars().count(), text_width);
        }
    }

    #[test]
    fn terminal_qr_keeps_white_border_for_scanners() {
        let payload = "tayd://server?addr=frp.example.com&port=7000&token=server-token";
        let qr = render_terminal_qr(payload).expect("QR should render");
        let visible_lines: Vec<String> = qr.lines().map(strip_ansi).collect();
        let quiet = TERMINAL_QR_QUIET_ZONE as usize;

        for line in visible_lines.iter().take(quiet / 2) {
            assert!(line.chars().all(|ch| ch == ' '));
        }
        for line in &visible_lines {
            assert!(line.chars().take(quiet).all(|ch| ch == ' '));
            assert!(line.chars().rev().take(quiet).all(|ch| ch == ' '));
        }
    }

    fn strip_ansi(value: &str) -> String {
        let mut output = String::new();
        let mut chars = value.chars();
        while let Some(ch) = chars.next() {
            if ch == '\x1b' {
                for next in chars.by_ref() {
                    if next == 'm' {
                        break;
                    }
                }
            } else {
                output.push(ch);
            }
        }
        output
    }
}

pub(crate) fn usage() {
    println!(
        r#"tayd

Usage:
  tayd <command> [arguments] [options]
  tayd add <name> <local> <remote> [options]

Commands:
  tayd add <name> <local> <remote>          Add a client mapping
  tayd remove <name>                        Remove a mapping
  tayd list                                 List mappings
  tayd show [name]                          Show rendered proxy config
  tayd render                               Rebuild frpc config files
  tayd verify                               Verify frpc config with frpc
  tayd restart                              Restart the client gateway
  tayd install                              Start the client gateway
  tayd uninstall                            Remove the local client install
  tayd info                                 Show server token and client install commands
  tayd service install                      Install auto-start service
  tayd service status                       Show auto-start service status
  tayd service uninstall                    Remove auto-start service
  tayd server install                       Start the server gateway
  tayd server uninstall                     Stop the server gateway
  tayd server restart                       Restart the server gateway
  tayd server info                          Show server token and client install commands
  tayd server log                           Show server install log
  tayd init --server <addr> --token <token>
                                           Initialize client state
  tayd init-server --token <token>          Initialize server state

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
  tayd init --server frp.example.com --token <token>
  tayd add web 8080 18080
  tayd add dns udp://5353 5353
  tayd add game 8680 2929 --type both
  tayd add site 3000 http://site.example.com
  tayd add blog 3000 https://blog.example.com --group blog --group-key shard-a
  tayd add app http://3000 https://app.example.com --crt fullchain.pem --key privkey.pem"#
    );
}
