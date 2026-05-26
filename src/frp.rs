use crate::{
    endpoint::{frp_proxy_name, proxy_types},
    model::{Proxy, State},
    paths::{loc_relay_home, write_private},
    Result,
};
use std::{
    ffi::OsStr,
    fs, io,
    path::PathBuf,
    process::{Command, Stdio},
};

pub(crate) fn render_state(state: &State) -> Result<()> {
    let home = loc_relay_home()?;
    let proxy_dir = home.join("frpc.d");
    fs::create_dir_all(&proxy_dir)?;

    for entry in fs::read_dir(&proxy_dir)? {
        let path = entry?.path();
        if path.extension() == Some(OsStr::new("toml")) {
            fs::remove_file(path)?;
        }
    }

    let main = format!(
        "serverAddr = {}\nserverPort = {}\n\nauth.method = \"token\"\nauth.token = {}\n\nincludes = [\"frpc.d/*.toml\"]\n",
        toml_string(&state.server.addr),
        state.server.port,
        toml_string(&state.server.token)
    );
    write_private(home.join("frpc.toml"), main)?;

    for proxy in &state.proxies {
        write_private(
            proxy_dir.join(format!("{}.toml", proxy.name)),
            render_proxy(proxy)?,
        )?;
    }
    Ok(())
}

pub(crate) fn render_proxy(proxy: &Proxy) -> Result<String> {
    let types = proxy_types(proxy)?;
    let multi_type = types.len() > 1;
    let mut config = String::new();
    for proxy_type in types {
        config.push_str(&format!(
            "[[proxies]]\nname = {}\ntype = {}\n",
            toml_string(&frp_proxy_name(proxy, &proxy_type, multi_type)),
            toml_string(&proxy_type)
        ));
        if let Some(group) = proxy.group.as_deref() {
            config.push_str(&format!("loadBalancer.group = {}\n", toml_string(group)));
        }
        if let Some(group_key) = proxy.group_key.as_deref() {
            config.push_str(&format!(
                "loadBalancer.groupKey = {}\n",
                toml_string(group_key)
            ));
        }
        match proxy_type.as_str() {
            "tcp" | "udp" => {
                config.push_str(&format!(
                    "localIP = {}\nlocalPort = {}\nremotePort = {}\n\n",
                    toml_string(&proxy.local_ip),
                    proxy.local_port,
                    proxy
                        .remote_port
                        .ok_or_else(|| format!("proxy {} remote_port is required", proxy.name))?
                ));
            }
            "http" | "https" => {
                config.push_str(&format!(
                    "customDomains = {}\n",
                    toml_string_array(&[proxy.remote_domain.as_deref().ok_or_else(|| format!(
                        "proxy {} remote_domain is required",
                        proxy.name
                    ))?])
                ));
                if let Some(plugin) = &proxy.plugin {
                    config.push_str("\n[proxies.plugin]\n");
                    config.push_str(&format!(
                        "type = {}\nlocalAddr = {}\n",
                        toml_string(&plugin.plugin_type),
                        toml_string(&format!("{}:{}", proxy.local_ip, proxy.local_port))
                    ));
                    if let Some(crt_path) = plugin.crt_path.as_deref() {
                        config.push_str(&format!("crtPath = {}\n", toml_string(crt_path)));
                    }
                    if let Some(key_path) = plugin.key_path.as_deref() {
                        config.push_str(&format!("keyPath = {}\n", toml_string(key_path)));
                    }
                    if let Some(host_header) = plugin.host_header_rewrite.as_deref() {
                        config.push_str(&format!(
                            "hostHeaderRewrite = {}\n",
                            toml_string(host_header)
                        ));
                    }
                    config.push('\n');
                } else {
                    config.push_str(&format!(
                        "localIP = {}\nlocalPort = {}\n\n",
                        toml_string(&proxy.local_ip),
                        proxy.local_port
                    ));
                }
            }
            unknown => return Err(format!("unsupported proxy type: {unknown}").into()),
        }
    }
    Ok(config)
}

pub(crate) fn run_frp(binary: &str, args: &[&str]) -> Result<()> {
    let status = Command::new(frp_binary(binary)?)
        .args(args)
        .current_dir(loc_relay_home()?)
        .stdin(Stdio::inherit())
        .stdout(Stdio::inherit())
        .stderr(Stdio::inherit())
        .status()?;
    if !status.success() {
        return Err(format!("{binary} exited with {status}").into());
    }
    Ok(())
}

pub(crate) fn start_process(binary: &str, args: &[&str]) -> Result<()> {
    start_process_with_announce(binary, args, true)
}

pub(crate) fn start_process_with_announce(
    binary: &str,
    args: &[&str],
    announce: bool,
) -> Result<()> {
    let home = loc_relay_home()?;
    let pid_file = home.join(format!("{binary}.pid"));
    if pid_file.exists() {
        return Err(format!("{binary} already has pid file: {}", pid_file.display()).into());
    }

    let log = fs::OpenOptions::new()
        .create(true)
        .append(true)
        .open(home.join(format!("{binary}.log")))?;
    let err_log = log.try_clone()?;
    let child = Command::new(frp_binary(binary)?)
        .args(args)
        .current_dir(&home)
        .stdin(Stdio::null())
        .stdout(Stdio::from(log))
        .stderr(Stdio::from(err_log))
        .spawn()?;
    fs::write(&pid_file, format!("{}\n", child.id()))?;
    if announce {
        println!("started {binary} pid {}", child.id());
    }
    Ok(())
}

pub(crate) fn stop_process(binary: &str) -> Result<()> {
    stop_process_with_announce(binary, true)
}

pub(crate) fn stop_process_with_announce(binary: &str, announce: bool) -> Result<()> {
    let home = loc_relay_home()?;
    let pid_file = home.join(format!("{binary}.pid"));
    if !pid_file.exists() {
        return Ok(());
    }
    let pid = fs::read_to_string(&pid_file)?.trim().to_owned();
    if !pid.is_empty() {
        let _ = kill_pid(&pid);
    }
    let _ = fs::remove_file(pid_file);
    if announce {
        println!("stopped {binary}");
    }
    Ok(())
}

pub(crate) fn frp_binary(binary: &str) -> io::Result<PathBuf> {
    let name = if cfg!(windows) {
        format!("{binary}.exe")
    } else {
        binary.to_owned()
    };
    Ok(loc_relay_home()?.join("bin").join(name))
}

fn kill_pid(pid: &str) -> io::Result<()> {
    if cfg!(windows) {
        let _ = Command::new("taskkill")
            .args(["/PID", pid, "/F"])
            .status()?;
    } else {
        let _ = Command::new("kill").arg(pid).status()?;
    }
    Ok(())
}

pub(crate) fn toml_string(value: &str) -> String {
    serde_json::to_string(value).expect("string serialization cannot fail")
}

fn toml_string_array(values: &[&str]) -> String {
    let values = values
        .iter()
        .map(|value| toml_string(value))
        .collect::<Vec<_>>()
        .join(", ");
    format!("[{values}]")
}
