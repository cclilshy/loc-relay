use crate::{
    model::{Proxy, ProxyPlugin},
    Result,
};

pub(crate) struct ParsedLocal {
    pub(crate) scheme: Option<String>,
    pub(crate) local_ip: String,
    pub(crate) local_port: u16,
}

pub(crate) struct ParsedRemote {
    pub(crate) scheme: Option<String>,
    pub(crate) port: Option<u16>,
    pub(crate) domain: Option<String>,
    pub(crate) display_port: Option<u16>,
}

pub(crate) fn parse_port(value: &str) -> Result<u16> {
    value
        .parse::<u16>()
        .map_err(|_| format!("invalid port: {value}").into())
}

pub(crate) fn parse_proxy_types(value: &str) -> Result<Vec<String>> {
    let value = value.trim().to_ascii_lowercase();
    if value.is_empty() {
        return Err("proxy type is required".into());
    }
    if value.contains("://") {
        return Err("proxy type must be tcp, udp, both, tcp,udp, http, or https".into());
    }
    if value.contains('+') {
        return Err("use --type both or --type tcp,udp for dual protocol".into());
    }

    let raw_types: Vec<&str> = if value == "both" {
        vec!["tcp", "udp"]
    } else {
        value.split(',').map(str::trim).collect()
    };

    let multi_type = raw_types.len() > 1;
    let mut has_tcp = false;
    let mut has_udp = false;
    let mut has_http = false;
    let mut has_https = false;
    for proxy_type in raw_types {
        match proxy_type {
            "tcp" => has_tcp = true,
            "udp" => has_udp = true,
            "http" if !multi_type => has_http = true,
            "https" if !multi_type => has_https = true,
            "" => return Err("empty proxy type".into()),
            unknown => return Err(format!("unsupported proxy type: {unknown}").into()),
        }
    }

    let mut types = Vec::new();
    if has_tcp {
        types.push(String::from("tcp"));
    }
    if has_udp {
        types.push(String::from("udp"));
    }
    if has_http {
        types.push(String::from("http"));
    }
    if has_https {
        types.push(String::from("https"));
    }
    Ok(types)
}

pub(crate) fn parse_local_arg(value: &str) -> Result<ParsedLocal> {
    let value = value.trim();
    if value.is_empty() {
        return Err("local endpoint is required".into());
    }

    let (scheme, endpoint) = if let Some((scheme, endpoint)) = value.split_once("://") {
        let scheme = normalize_endpoint_scheme(scheme)?;
        (Some(scheme), endpoint)
    } else {
        (None, value)
    };

    if endpoint.is_empty() {
        return Err("local endpoint is missing a port".into());
    }

    let default_port = match scheme.as_deref() {
        Some("http") => Some(80),
        Some("https") => Some(443),
        _ => None,
    };
    let (local_ip, local_port) = parse_host_port(endpoint, default_port)?;

    Ok(ParsedLocal {
        scheme,
        local_ip,
        local_port,
    })
}

pub(crate) fn parse_remote_arg(value: &str) -> Result<ParsedRemote> {
    let value = value.trim();
    if value.is_empty() {
        return Err("remote endpoint is required".into());
    }

    let Some((scheme, endpoint)) = value.split_once("://") else {
        return Ok(ParsedRemote {
            scheme: None,
            port: Some(parse_port(value)?),
            domain: None,
            display_port: None,
        });
    };

    let scheme = normalize_endpoint_scheme(scheme)?;
    match scheme.as_str() {
        "tcp" | "udp" => {
            let (_, port) = parse_host_port(endpoint, None)?;
            Ok(ParsedRemote {
                scheme: Some(scheme),
                port: Some(port),
                domain: None,
                display_port: None,
            })
        }
        "http" | "https" => {
            let (domain, display_port) = parse_domain_endpoint(endpoint)?;
            Ok(ParsedRemote {
                scheme: Some(scheme),
                port: None,
                domain: Some(domain),
                display_port,
            })
        }
        _ => unreachable!("endpoint scheme already normalized"),
    }
}

fn normalize_endpoint_scheme(value: &str) -> Result<String> {
    let value = value.trim().to_ascii_lowercase();
    if value.contains('+') {
        return Err("composite endpoint schemes are not supported".into());
    }
    match value.as_str() {
        "tcp" | "udp" | "http" | "https" => Ok(value),
        "" => Err("endpoint scheme is required".into()),
        unknown => Err(format!("unsupported endpoint protocol: {unknown}").into()),
    }
}

fn parse_host_port(value: &str, default_port: Option<u16>) -> Result<(String, u16)> {
    let value = strip_endpoint_path(value)?;
    if value.is_empty() {
        return Err("endpoint is missing a port".into());
    }

    if let Some((host, port)) = value.rsplit_once(':') {
        let host = if host.is_empty() {
            String::from("127.0.0.1")
        } else {
            host.to_owned()
        };
        return Ok((host, parse_port(port)?));
    }

    if value.bytes().all(|byte| byte.is_ascii_digit()) {
        return Ok((String::from("127.0.0.1"), parse_port(value)?));
    }

    if let Some(port) = default_port {
        return Ok((value.to_owned(), port));
    }

    Err(format!("endpoint is missing a port: {value}").into())
}

fn parse_domain_endpoint(value: &str) -> Result<(String, Option<u16>)> {
    let value = strip_endpoint_path(value)?;
    if value.is_empty() {
        return Err("remote domain is required".into());
    }
    if value.bytes().all(|byte| byte.is_ascii_digit()) {
        return Err("http/https remote endpoint requires a domain".into());
    }

    if let Some((host, port)) = value.rsplit_once(':') {
        if host.is_empty() {
            return Err("remote domain is required".into());
        }
        return Ok((host.to_owned(), Some(parse_port(port)?)));
    }

    Ok((value.to_owned(), None))
}

fn strip_endpoint_path(value: &str) -> Result<&str> {
    if value.contains('/') || value.contains('?') || value.contains('#') {
        return Err("endpoint paths are not supported".into());
    }
    Ok(value)
}

pub(crate) fn infer_proxy_types(
    local: &ParsedLocal,
    remote: &ParsedRemote,
    explicit_types: Option<Vec<String>>,
) -> Result<Vec<String>> {
    let inferred = match remote.scheme.as_deref() {
        Some("http") | Some("https") => vec![remote.scheme.clone().expect("checked")],
        Some("tcp") | Some("udp") => vec![remote.scheme.clone().expect("checked")],
        None => match local.scheme.as_deref() {
            Some("tcp") | Some("udp") => vec![local.scheme.clone().expect("checked")],
            _ => {
                return Ok(explicit_types.unwrap_or_else(|| vec![String::from("tcp")]));
            }
        },
        Some(unknown) => return Err(format!("unsupported remote protocol: {unknown}").into()),
    };

    if let Some(explicit_types) = explicit_types {
        if explicit_types != inferred {
            return Err("protocol specified twice with different values".into());
        }
    }

    Ok(inferred)
}

pub(crate) fn validate_load_balancer(
    types: &[String],
    group: &Option<String>,
    group_key: &Option<String>,
    proxy_name: &str,
) -> Result<()> {
    if let Some(group) = group.as_deref() {
        if group.trim().is_empty() {
            return Err(format!("proxy {proxy_name} load balancer group is empty").into());
        }
    }
    if let Some(group_key) = group_key.as_deref() {
        if group_key.trim().is_empty() {
            return Err(format!("proxy {proxy_name} load balancer group key is empty").into());
        }
        if group.is_none() {
            return Err("--group-key requires --group".into());
        }
    }
    if group.is_none() {
        return Ok(());
    }

    for proxy_type in types {
        match proxy_type.as_str() {
            "tcp" | "http" | "https" => {}
            unsupported => {
                return Err(format!(
                    "proxy {proxy_name} type {unsupported} does not support load balancer groups"
                )
                .into());
            }
        }
    }
    Ok(())
}

pub(crate) fn infer_plugin(
    local: &ParsedLocal,
    remote: &ParsedRemote,
    crt_path: Option<String>,
    key_path: Option<String>,
) -> Result<Option<ProxyPlugin>> {
    match (&crt_path, &key_path) {
        (Some(_), Some(_)) => {}
        (None, None) => {
            if remote.scheme.as_deref() == Some("https") && local.scheme.as_deref() == Some("http")
            {
                return Err(
                    "https remote with http local requires --crt and --key for https2http".into(),
                );
            }
            if remote.scheme.as_deref() == Some("http") && local.scheme.as_deref() == Some("https")
            {
                return Ok(Some(ProxyPlugin {
                    plugin_type: String::from("http2https"),
                    crt_path: None,
                    key_path: None,
                    host_header_rewrite: Some(local.local_ip.clone()),
                }));
            }
            return Ok(None);
        }
        _ => return Err("--crt and --key must be provided together".into()),
    }

    if remote.scheme.as_deref() != Some("https") {
        return Err("--crt/--key can only be used with https remote endpoints".into());
    }

    let plugin_type = match local.scheme.as_deref() {
        Some("http") => "https2http",
        Some("https") => "https2https",
        _ => {
            return Err(
                "--crt/--key requires local scheme http:// or https:// to choose a plugin".into(),
            );
        }
    };

    Ok(Some(ProxyPlugin {
        plugin_type: String::from(plugin_type),
        crt_path,
        key_path,
        host_header_rewrite: Some(local.local_ip.clone()),
    }))
}

pub(crate) fn proxy_types(proxy: &Proxy) -> Result<Vec<String>> {
    if !proxy.types.is_empty() {
        return parse_proxy_types(&proxy.types.join(","));
    }
    if let Some(proxy_type) = proxy.legacy_type.as_deref() {
        return parse_proxy_types(proxy_type);
    }
    Ok(vec![String::from("tcp")])
}

pub(crate) fn frp_proxy_name(proxy: &Proxy, proxy_type: &str, multi_type: bool) -> String {
    if multi_type {
        format!("{}-{proxy_type}", proxy.name)
    } else {
        proxy.name.clone()
    }
}

pub(crate) fn valid_name(value: &str) -> bool {
    !value.is_empty()
        && value
            .bytes()
            .all(|b| b.is_ascii_alphanumeric() || b == b'.' || b == b'_' || b == b'-')
}
