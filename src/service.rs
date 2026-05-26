use crate::{
    frp::frp_binary,
    paths::{config_home, loc_relay_home, user_home},
    Result,
};
use std::{
    env, fs, io,
    path::{Path, PathBuf},
    process::{Command, Stdio},
};

pub(crate) fn install_service() -> Result<()> {
    if cfg!(target_os = "linux") {
        install_systemd_user_service()
    } else if cfg!(target_os = "macos") {
        install_launchd_service()
    } else if cfg!(windows) {
        install_windows_startup()
    } else {
        Err(format!("service install is not supported on {}", env::consts::OS).into())
    }
}

pub(crate) fn uninstall_service() -> Result<()> {
    if cfg!(target_os = "linux") {
        uninstall_systemd_user_service()
    } else if cfg!(target_os = "macos") {
        uninstall_launchd_service()
    } else if cfg!(windows) {
        uninstall_windows_startup()
    } else {
        Ok(())
    }
}

pub(crate) fn service_status() -> Result<()> {
    let path = service_file_path()?;
    if path.exists() {
        println!("installed: {}", path.display());
    } else {
        println!("not installed");
    }
    Ok(())
}

fn service_file_path() -> Result<PathBuf> {
    if cfg!(target_os = "linux") {
        Ok(config_home()?.join("systemd/user/loc-relay-frpc.service"))
    } else if cfg!(target_os = "macos") {
        Ok(user_home()?.join("Library/LaunchAgents/com.cclilshy.locrelay.frpc.plist"))
    } else if cfg!(windows) {
        let appdata = env::var_os("APPDATA").ok_or("APPDATA is not set")?;
        Ok(PathBuf::from(appdata)
            .join(r"Microsoft\Windows\Start Menu\Programs\Startup\loc-relay-frpc.cmd"))
    } else {
        Err(format!("service is not supported on {}", env::consts::OS).into())
    }
}

fn install_systemd_user_service() -> Result<()> {
    let home = loc_relay_home()?;
    let path = service_file_path()?;
    fs::create_dir_all(path.parent().ok_or("invalid service path")?)?;
    let content = format!(
        "[Unit]\nDescription=loc-relay frpc\nAfter=network-online.target\n\n[Service]\nWorkingDirectory={home}\nExecStart={frpc} -c {home}/frpc.toml\nRestart=always\nRestartSec=3\n\n[Install]\nWantedBy=default.target\n",
        home = home.display(),
        frpc = frp_binary("frpc")?.display()
    );
    fs::write(&path, content)?;
    if env::var("LOC_RELAY_SERVICE_SKIP_ENABLE").ok().as_deref() != Some("1") {
        let _ = Command::new("systemctl")
            .args(["--user", "daemon-reload"])
            .status();
        let _ = Command::new("systemctl")
            .args(["--user", "enable", "--now", "loc-relay-frpc.service"])
            .status();
    }
    println!("installed service: {}", path.display());
    Ok(())
}

fn uninstall_systemd_user_service() -> Result<()> {
    let path = service_file_path()?;
    if env::var("LOC_RELAY_SERVICE_SKIP_ENABLE").ok().as_deref() != Some("1") {
        let _ = Command::new("systemctl")
            .args(["--user", "disable", "--now", "loc-relay-frpc.service"])
            .status();
    }
    let _ = fs::remove_file(&path);
    if env::var("LOC_RELAY_SERVICE_SKIP_ENABLE").ok().as_deref() != Some("1") {
        let _ = Command::new("systemctl")
            .args(["--user", "daemon-reload"])
            .status();
    }
    println!("removed service: {}", path.display());
    Ok(())
}

fn install_launchd_service() -> Result<()> {
    let home = loc_relay_home()?;
    let path = service_file_path()?;
    fs::create_dir_all(path.parent().ok_or("invalid service path")?)?;
    let content = format!(
        r#"<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
  <key>Label</key><string>com.cclilshy.locrelay.frpc</string>
  <key>WorkingDirectory</key><string>{home}</string>
  <key>ProgramArguments</key>
  <array>
    <string>{frpc}</string>
    <string>-c</string>
    <string>{home}/frpc.toml</string>
  </array>
  <key>RunAtLoad</key><true/>
  <key>KeepAlive</key><true/>
  <key>StandardOutPath</key><string>{home}/frpc.log</string>
  <key>StandardErrorPath</key><string>{home}/frpc.err.log</string>
</dict>
</plist>
"#,
        home = xml_escape(&home.display().to_string()),
        frpc = xml_escape(&frp_binary("frpc")?.display().to_string())
    );
    fs::write(&path, content)?;
    if env::var("LOC_RELAY_SERVICE_SKIP_ENABLE").ok().as_deref() != Some("1") {
        let uid = Command::new("id").arg("-u").output()?;
        let uid = String::from_utf8_lossy(&uid.stdout).trim().to_owned();
        let _ = Command::new("launchctl")
            .args([
                "bootstrap",
                &format!("gui/{uid}"),
                path.to_string_lossy().as_ref(),
            ])
            .status();
        let _ = Command::new("launchctl")
            .args(["enable", &format!("gui/{uid}/com.cclilshy.locrelay.frpc")])
            .status();
    }
    println!("installed service: {}", path.display());
    Ok(())
}

fn uninstall_launchd_service() -> Result<()> {
    let path = service_file_path()?;
    if env::var("LOC_RELAY_SERVICE_SKIP_ENABLE").ok().as_deref() != Some("1") {
        let uid = Command::new("id").arg("-u").output()?;
        let uid = String::from_utf8_lossy(&uid.stdout).trim().to_owned();
        let _ = Command::new("launchctl")
            .args([
                "bootout",
                &format!("gui/{uid}"),
                path.to_string_lossy().as_ref(),
            ])
            .status();
    }
    let _ = fs::remove_file(&path);
    println!("removed service: {}", path.display());
    Ok(())
}

fn install_windows_startup() -> Result<()> {
    let home = loc_relay_home()?;
    let path = service_file_path()?;
    fs::create_dir_all(path.parent().ok_or("invalid startup path")?)?;
    let config = home.join("frpc.toml");
    let content = format!(
        "@echo off\r\ncd /d \"{home}\"\r\nstart \"\" \"{frpc}\" -c \"{config}\"\r\n",
        home = home.display(),
        frpc = frp_binary("frpc")?.display(),
        config = config.display()
    );
    fs::write(&path, content)?;
    println!("installed startup command: {}", path.display());
    Ok(())
}

fn uninstall_windows_startup() -> Result<()> {
    let path = service_file_path()?;
    let _ = fs::remove_file(&path);
    println!("removed startup command: {}", path.display());
    Ok(())
}

pub(crate) fn remove_loc_relay_symlink(home: &Path) -> Result<()> {
    let user_home = match user_home() {
        Ok(path) => path,
        Err(_) => return Ok(()),
    };

    if cfg!(windows) {
        let cmd = user_home.join(".local/bin/loc-relay.cmd");
        let data = match fs::read_to_string(&cmd) {
            Ok(data) => data,
            Err(err) if err.kind() == io::ErrorKind::NotFound => return Ok(()),
            Err(err) => return Err(err.into()),
        };
        if data.contains(&home.display().to_string()) {
            fs::remove_file(cmd)?;
        }
        return Ok(());
    }

    let link = user_home.join(".local/bin/loc-relay");
    let target = match fs::read_link(&link) {
        Ok(target) => target,
        Err(err) if err.kind() == io::ErrorKind::NotFound => return Ok(()),
        Err(err) => return Err(err.into()),
    };
    let resolved = if target.is_absolute() {
        target
    } else {
        link.parent().unwrap_or_else(|| Path::new(".")).join(target)
    };
    if resolved.starts_with(home) {
        fs::remove_file(link)?;
    }
    Ok(())
}

pub(crate) fn restart_service_if_running() -> Result<bool> {
    if cfg!(target_os = "linux") {
        let active = match Command::new("systemctl")
            .args(["--user", "is-active", "--quiet", "loc-relay-frpc.service"])
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .status()
        {
            Ok(status) => status.success(),
            Err(err) if err.kind() == io::ErrorKind::NotFound => false,
            Err(err) => return Err(err.into()),
        };
        if !active {
            return Ok(false);
        }
        let status = Command::new("systemctl")
            .args(["--user", "restart", "loc-relay-frpc.service"])
            .status()?;
        if !status.success() {
            return Err(format!("systemctl restart exited with {status}").into());
        }
        return Ok(true);
    }

    if cfg!(target_os = "macos") {
        let label = "com.cclilshy.locrelay.frpc";
        let uid = current_uid()?;
        let service = format!("gui/{uid}/{label}");
        let active = match Command::new("launchctl")
            .args(["print", &service])
            .stdout(Stdio::null())
            .stderr(Stdio::null())
            .status()
        {
            Ok(status) => status.success(),
            Err(err) if err.kind() == io::ErrorKind::NotFound => false,
            Err(err) => return Err(err.into()),
        };
        if !active {
            return Ok(false);
        }
        let status = Command::new("launchctl")
            .args(["kickstart", "-k", &service])
            .status()?;
        if !status.success() {
            return Err(format!("launchctl kickstart exited with {status}").into());
        }
        return Ok(true);
    }

    Ok(false)
}

fn current_uid() -> Result<String> {
    let uid = Command::new("id").arg("-u").output()?;
    if !uid.status.success() {
        return Err(format!("id -u exited with {}", uid.status).into());
    }
    Ok(String::from_utf8_lossy(&uid.stdout).trim().to_owned())
}

fn xml_escape(value: &str) -> String {
    value
        .replace('&', "&amp;")
        .replace('<', "&lt;")
        .replace('>', "&gt;")
        .replace('"', "&quot;")
        .replace('\'', "&apos;")
}
