use crate::{
    endpoint::{
        infer_plugin, infer_proxy_types, parse_local_arg, parse_port, parse_proxy_types,
        parse_remote_arg, validate_load_balancer,
    },
    frp::{
        render_proxy, render_state, run_frp, start_process, start_process_with_announce,
        stop_process, stop_process_with_announce, toml_string,
    },
    model::{Proxy, Server, ServerInstallInfo, State, STATE_VERSION},
    output::{format_proxy_route, print_server_install_info, status, usage},
    paths::{loc_relay_home, write_private},
    service::{
        install_service, remove_loc_relay_symlink, restart_service_if_running, service_status,
        uninstall_service,
    },
    state::{load_server_install_info, load_state, save_server_install_info, save_state},
    Result,
};
use std::{env, fs};

pub(crate) fn run() -> Result<()> {
    let mut args = env::args().skip(1);
    let Some(command) = args.next() else {
        usage();
        return Err("missing command".into());
    };
    let rest: Vec<String> = args.collect();

    match command.as_str() {
        "init" => cmd_init(&rest),
        "init-server" => cmd_init_server(&rest),
        "add" => cmd_add(&rest),
        "remove" => cmd_remove(&rest),
        "list" => cmd_list(),
        "show" => cmd_show(&rest),
        "render" => cmd_render(),
        "verify" => run_frp("frpc", &["verify", "-c", "frpc.toml"]),
        "restart" => cmd_restart(),
        "install" | "up" => start_process("frpc", &["-c", "frpc.toml"]),
        "info" => cmd_server_info(),
        "server" => cmd_server(&rest),
        "server-up" => start_process("frps", &["-c", "frps.toml"]),
        "server-down" => stop_process("frps"),
        "service" => cmd_service(&rest),
        "uninstall" | "down" => cmd_down(),
        "help" | "-h" | "--help" => {
            usage();
            Ok(())
        }
        _ => Err(format!("unknown command: {command}").into()),
    }
}

fn cmd_init_server(args: &[String]) -> Result<()> {
    let mut token = String::new();
    let mut port = 7000;
    let mut addr: Option<String> = None;
    let mut http_port: Option<u16> = None;
    let mut https_port: Option<u16> = None;
    let mut raw_base_url: Option<String> = None;

    let mut i = 0;
    while i < args.len() {
        match args[i].as_str() {
            "--addr" | "--server" | "--server-addr" => {
                addr = Some(option_value(args, i)?.to_owned());
                i += 2;
            }
            "--token" => {
                token = option_value(args, i)?.to_owned();
                i += 2;
            }
            "--port" => {
                port = parse_port(option_value(args, i)?)?;
                i += 2;
            }
            "--http-port" => {
                http_port = Some(parse_port(option_value(args, i)?)?);
                i += 2;
            }
            "--https-port" => {
                https_port = Some(parse_port(option_value(args, i)?)?);
                i += 2;
            }
            "--raw-base-url" => {
                raw_base_url = Some(option_value(args, i)?.to_owned());
                i += 2;
            }
            unknown => return Err(format!("unknown init-server option: {unknown}").into()),
        }
    }

    if token.is_empty() {
        return Err("--token is required".into());
    }

    let mut config = format!(
        "bindPort = {}\n\nauth.method = \"token\"\nauth.token = {}\n",
        port,
        toml_string(&token)
    );
    if let Some(port) = http_port {
        config.push_str(&format!("vhostHTTPPort = {port}\n"));
    }
    if let Some(port) = https_port {
        config.push_str(&format!("vhostHTTPSPort = {port}\n"));
    }
    let home = loc_relay_home()?;
    fs::create_dir_all(&home)?;
    write_private(home.join("frps.toml"), config)?;
    if let Some(addr) = addr {
        save_server_install_info(&ServerInstallInfo {
            addr,
            port,
            token: token.clone(),
            http_port,
            https_port,
            raw_base_url,
        })?;
    }
    println!("initialized server {}", home.display());
    Ok(())
}

fn cmd_init(args: &[String]) -> Result<()> {
    let mut server = String::new();
    let mut token = String::new();
    let mut server_port = 7000;
    let mut name = String::from("mapping");
    let mut types = vec![String::from("tcp")];
    let mut local_ip = String::from("127.0.0.1");
    let mut local_port: Option<u16> = None;
    let mut remote_port: Option<u16> = None;
    let mut mapping_requested = false;

    let mut i = 0;
    while i < args.len() {
        match args[i].as_str() {
            "--server" => {
                server = option_value(args, i)?.to_owned();
                i += 2;
            }
            "--token" => {
                token = option_value(args, i)?.to_owned();
                i += 2;
            }
            "--server-port" => {
                server_port = parse_port(option_value(args, i)?)?;
                i += 2;
            }
            "--name" => {
                name = option_value(args, i)?.to_owned();
                mapping_requested = true;
                i += 2;
            }
            "--type" => {
                types = parse_proxy_types(option_value(args, i)?)?;
                mapping_requested = true;
                i += 2;
            }
            "--local-ip" => {
                local_ip = option_value(args, i)?.to_owned();
                mapping_requested = true;
                i += 2;
            }
            "--local-port" => {
                local_port = Some(parse_port(option_value(args, i)?)?);
                mapping_requested = true;
                i += 2;
            }
            "--remote-port" => {
                remote_port = Some(parse_port(option_value(args, i)?)?);
                mapping_requested = true;
                i += 2;
            }
            unknown => return Err(format!("unknown init option: {unknown}").into()),
        }
    }

    if server.is_empty() {
        return Err("--server is required".into());
    }
    if token.is_empty() {
        return Err("--token is required".into());
    }

    let proxies = if mapping_requested {
        let local_port =
            local_port.ok_or("--local-port is required when initializing a mapping")?;
        let remote_port =
            remote_port.ok_or("--remote-port is required when initializing a mapping")?;
        vec![Proxy {
            name,
            types,
            legacy_type: None,
            local_scheme: None,
            local_ip,
            local_port,
            remote_port: Some(remote_port),
            remote_scheme: None,
            remote_domain: None,
            remote_display_port: None,
            group: None,
            group_key: None,
            plugin: None,
        }]
    } else {
        Vec::new()
    };

    let state = State {
        version: STATE_VERSION,
        server: Server {
            addr: server,
            port: server_port,
            token,
        },
        proxies,
    };
    save_state(&state)?;
    println!(
        "{} client -> {}",
        status("initialized"),
        loc_relay_home()?.display()
    );
    Ok(())
}

fn cmd_add(args: &[String]) -> Result<()> {
    if args.len() < 3 {
        return Err(
            "usage: tayd add <name> <local> <remote> [--type tcp|udp|both|tcp,udp|http|https] [--group name --group-key key] [--crt path --key path] [--no-restart]"
                .into(),
        );
    }

    let name = args[0].clone();
    let mut parsed_local = parse_local_arg(&args[1])?;
    let parsed_remote = parse_remote_arg(&args[2])?;
    let mut explicit_types: Option<Vec<String>> = None;
    let mut local_ip = parsed_local.local_ip.clone();
    let local_port = parsed_local.local_port;
    let mut crt_path: Option<String> = None;
    let mut key_path: Option<String> = None;
    let mut group: Option<String> = None;
    let mut group_key: Option<String> = None;
    let mut no_restart = false;

    let mut i = 3;
    while i < args.len() {
        match args[i].as_str() {
            "--type" => {
                explicit_types = Some(parse_proxy_types(option_value(args, i)?)?);
                i += 2;
            }
            "--local-ip" => {
                local_ip = option_value(args, i)?.to_owned();
                parsed_local.local_ip = local_ip.clone();
                i += 2;
            }
            "--crt" => {
                crt_path = Some(option_value(args, i)?.to_owned());
                i += 2;
            }
            "--key" => {
                key_path = Some(option_value(args, i)?.to_owned());
                i += 2;
            }
            "--group" => {
                group = Some(option_value(args, i)?.to_owned());
                i += 2;
            }
            "--group-key" => {
                group_key = Some(option_value(args, i)?.to_owned());
                i += 2;
            }
            "--no-restart" => {
                no_restart = true;
                i += 1;
            }
            unknown => return Err(format!("unknown add option: {unknown}").into()),
        }
    }

    let types = infer_proxy_types(&parsed_local, &parsed_remote, explicit_types)?;
    validate_load_balancer(&types, &group, &group_key, &name)?;
    let plugin = infer_plugin(&parsed_local, &parsed_remote, crt_path, key_path)?;

    let mut state = load_state()?;
    if state.proxies.iter().any(|proxy| proxy.name == name) {
        return Err(format!(
            "[error] mapping \"{name}\" already exists\nhint: use `tayd remove {name}` first"
        )
        .into());
    }
    let proxy = Proxy {
        name: name.clone(),
        types,
        local_scheme: parsed_local.scheme,
        legacy_type: None,
        local_ip,
        local_port,
        remote_port: parsed_remote.port,
        remote_scheme: parsed_remote.scheme,
        remote_domain: parsed_remote.domain,
        remote_display_port: parsed_remote.display_port,
        group,
        group_key,
        plugin,
    };
    state.proxies.push(proxy.clone());
    save_state(&state)?;
    println!(
        "{} {} {}",
        status("added"),
        proxy.name,
        format_proxy_route(&state.server, &proxy)?
    );
    refresh_client_after_edit(no_restart)?;
    Ok(())
}

fn cmd_remove(args: &[String]) -> Result<()> {
    if args.is_empty() || args.len() > 2 {
        return Err("usage: tayd remove <name> [--no-restart]".into());
    }

    let mut name: Option<&String> = None;
    let mut no_restart = false;
    for arg in args {
        if arg == "--no-restart" {
            no_restart = true;
        } else if name.is_none() {
            name = Some(arg);
        } else {
            return Err(format!("unknown remove option: {arg}").into());
        }
    }

    let name = name.ok_or("usage: tayd remove <name> [--no-restart]")?;
    let mut state = load_state()?;
    let index = state
        .proxies
        .iter()
        .position(|proxy| proxy.name == *name)
        .ok_or_else(|| format!("mapping {name} not found"))?;
    let removed = state.proxies.remove(index);
    save_state(&state)?;
    println!(
        "{} {} {}",
        status("removed"),
        removed.name,
        format_proxy_route(&state.server, &removed)?
    );
    refresh_client_after_edit(no_restart)?;
    Ok(())
}

fn cmd_list() -> Result<()> {
    let mut state = load_state()?;
    if state.proxies.is_empty() {
        println!("no mappings");
        return Ok(());
    }
    state.proxies.sort_by(|a, b| a.name.cmp(&b.name));
    for proxy in state.proxies {
        println!(
            "{} {}",
            proxy.name,
            format_proxy_route(&state.server, &proxy)?
        );
    }
    Ok(())
}

fn cmd_show(args: &[String]) -> Result<()> {
    if args.len() > 1 {
        return Err("usage: tayd show [name]".into());
    }
    let state = load_state()?;
    for proxy in state.proxies {
        if args.len() == 1 && proxy.name != args[0] {
            continue;
        }
        print!("{}", render_proxy(&proxy)?);
        if args.len() == 1 {
            return Ok(());
        }
    }
    if args.len() == 1 {
        return Err(format!("proxy {} not found", args[0]).into());
    }
    Ok(())
}

fn cmd_render() -> Result<()> {
    let state = load_state()?;
    render_state(&state)?;
    println!("{} client config", status("rendered"));
    Ok(())
}

fn cmd_down() -> Result<()> {
    let home = loc_relay_home()?;
    let marker = home.join(".tayd-client");
    let legacy_marker = home.join(".loc-relay-client");
    if !marker.exists()
        && !legacy_marker.exists()
        && !env_flag("TAYD_FORCE_DOWN", "LOC_RELAY_FORCE_DOWN")
    {
        return Err(format!(
            "{} is not marked as a client install; set TAYD_FORCE_DOWN=1 to remove it anyway",
            home.display()
        )
        .into());
    }

    if !env_flag("TAYD_SKIP_STOP", "LOC_RELAY_SKIP_STOP") {
        let _ = stop_process("frpc");
    }

    let _ = uninstall_service();
    remove_loc_relay_symlink(&home)?;
    fs::remove_dir_all(&home)?;
    println!("removed {}", home.display());
    Ok(())
}

fn cmd_restart() -> Result<()> {
    if restart_service_if_running()? {
        println!("{} client gateway", status("restarted"));
        return Ok(());
    }
    stop_process("frpc")?;
    start_process("frpc", &["-c", "frpc.toml"])
}

fn cmd_service(args: &[String]) -> Result<()> {
    if args.len() != 1 {
        return Err("usage: tayd service <install|uninstall|status>".into());
    }
    match args[0].as_str() {
        "install" => install_service(),
        "uninstall" => uninstall_service(),
        "status" => service_status(),
        unknown => Err(format!("unknown service command: {unknown}").into()),
    }
}

fn cmd_server(args: &[String]) -> Result<()> {
    if args.len() != 1 {
        return Err("usage: tayd server <install|uninstall|restart|info|log>".into());
    }
    match args[0].as_str() {
        "install" => start_process("frps", &["-c", "frps.toml"]),
        "uninstall" => stop_process("frps"),
        "restart" => {
            stop_process("frps")?;
            start_process("frps", &["-c", "frps.toml"])
        }
        "info" | "log" => cmd_server_info(),
        unknown => Err(format!("unknown server command: {unknown}").into()),
    }
}

fn cmd_server_info() -> Result<()> {
    let info = load_server_install_info()?;
    print_server_install_info(&info)
}

fn env_flag(primary: &str, legacy: &str) -> bool {
    env::var(primary).ok().as_deref() == Some("1") || env::var(legacy).ok().as_deref() == Some("1")
}

fn option_value(args: &[String], index: usize) -> Result<&str> {
    args.get(index + 1)
        .map(String::as_str)
        .ok_or_else(|| format!("missing value for {}", args[index]).into())
}

fn refresh_client_after_edit(no_restart: bool) -> Result<()> {
    if no_restart {
        println!("{} restart skipped", status("saved"));
        return Ok(());
    }
    if restart_service_if_running()? {
        println!("{} client gateway", status("restarted"));
        return Ok(());
    }

    let pid_file = loc_relay_home()?.join("frpc.pid");
    if pid_file.exists() {
        stop_process_with_announce("frpc", false)?;
        start_process_with_announce("frpc", &["-c", "frpc.toml"], false)?;
        println!("{} client gateway", status("restarted"));
    } else {
        println!("{} client gateway is not running", status("saved"));
    }
    Ok(())
}
