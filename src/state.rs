use crate::{
    endpoint::{frp_proxy_name, proxy_types, valid_name, validate_load_balancer},
    frp::render_state,
    model::{ServerInstallInfo, State, STATE_VERSION},
    paths::{loc_relay_home, write_private},
    Result,
};
use std::{collections::HashSet, fs};

pub(crate) fn load_state() -> Result<State> {
    let path = loc_relay_home()?.join("state.json");
    let data =
        fs::read_to_string(&path).map_err(|err| format!("read {}: {err}", path.display()))?;
    let state: State =
        serde_json::from_str(&data).map_err(|err| format!("parse {}: {err}", path.display()))?;
    validate_state(&state)?;
    Ok(state)
}

pub(crate) fn load_server_install_info() -> Result<ServerInstallInfo> {
    let path = loc_relay_home()?.join("server-install.json");
    let data =
        fs::read_to_string(&path).map_err(|err| format!("read {}: {err}", path.display()))?;
    let info: ServerInstallInfo =
        serde_json::from_str(&data).map_err(|err| format!("parse {}: {err}", path.display()))?;
    validate_server_install_info(&info)?;
    Ok(info)
}

pub(crate) fn save_server_install_info(info: &ServerInstallInfo) -> Result<()> {
    validate_server_install_info(info)?;
    let data = serde_json::to_string_pretty(info)? + "\n";
    write_private(loc_relay_home()?.join("server-install.json"), data)
}

fn validate_server_install_info(info: &ServerInstallInfo) -> Result<()> {
    if info.addr.trim().is_empty() {
        return Err("server install addr is required".into());
    }
    if info.token.trim().is_empty() {
        return Err("server install token is required".into());
    }
    Ok(())
}

pub(crate) fn save_state(state: &State) -> Result<()> {
    let state = normalize_state(state)?;
    validate_state(&state)?;
    let home = loc_relay_home()?;
    fs::create_dir_all(&home)?;
    let data = serde_json::to_string_pretty(&state)? + "\n";
    write_private(home.join("state.json"), data)?;
    render_state(&state)
}

fn validate_state(state: &State) -> Result<()> {
    if state.version != STATE_VERSION {
        return Err(format!("unsupported state version: {}", state.version).into());
    }
    if state.server.addr.trim().is_empty() {
        return Err("server.addr is required".into());
    }
    if state.server.token.trim().is_empty() {
        return Err("server.token is required".into());
    }

    let mut names = HashSet::new();
    let mut frp_names = HashSet::new();
    for proxy in &state.proxies {
        if !valid_name(&proxy.name) {
            return Err(format!("invalid proxy name: {}", proxy.name).into());
        }
        if proxy.local_ip.trim().is_empty() {
            return Err(format!("proxy {} local_ip is required", proxy.name).into());
        }
        if !names.insert(proxy.name.clone()) {
            return Err(format!("proxy {} already exists", proxy.name).into());
        }
        let types = proxy_types(proxy)?;
        validate_load_balancer(&types, &proxy.group, &proxy.group_key, &proxy.name)?;
        let multi_type = types.len() > 1;
        for proxy_type in &types {
            match proxy_type.as_str() {
                "tcp" | "udp" => {
                    if proxy.remote_port.is_none() {
                        return Err(format!("proxy {} remote_port is required", proxy.name).into());
                    }
                }
                "http" | "https" => {
                    if proxy
                        .remote_domain
                        .as_deref()
                        .unwrap_or("")
                        .trim()
                        .is_empty()
                    {
                        return Err(
                            format!("proxy {} remote_domain is required", proxy.name).into()
                        );
                    }
                }
                unknown => return Err(format!("unsupported proxy type: {unknown}").into()),
            }
            let frp_name = frp_proxy_name(proxy, proxy_type, multi_type);
            if !frp_names.insert(frp_name.clone()) {
                return Err(format!("frp proxy name conflict: {frp_name}").into());
            }
        }
        if let Some(plugin) = &proxy.plugin {
            match plugin.plugin_type.as_str() {
                "http2https" => {}
                "https2http" | "https2https" => {
                    if plugin.crt_path.as_deref().unwrap_or("").is_empty()
                        || plugin.key_path.as_deref().unwrap_or("").is_empty()
                    {
                        return Err(format!(
                            "proxy {} plugin requires crt_path and key_path",
                            proxy.name
                        )
                        .into());
                    }
                }
                unknown => return Err(format!("unsupported plugin type: {unknown}").into()),
            }
        }
    }
    Ok(())
}

fn normalize_state(state: &State) -> Result<State> {
    let mut state = state.clone();
    for proxy in &mut state.proxies {
        proxy.types = proxy_types(proxy)?;
        proxy.legacy_type = None;
    }
    Ok(state)
}
