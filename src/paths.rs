use crate::Result;
use std::{
    env, fs, io,
    path::{Path, PathBuf},
};

pub(crate) fn user_home() -> Result<PathBuf> {
    if let Some(home) = env::var_os("HOME") {
        return Ok(PathBuf::from(home));
    }
    if let Some(home) = env::var_os("USERPROFILE") {
        return Ok(PathBuf::from(home));
    }
    Err("HOME or USERPROFILE is not set".into())
}

pub(crate) fn config_home() -> Result<PathBuf> {
    if let Some(home) = env::var_os("XDG_CONFIG_HOME") {
        return Ok(PathBuf::from(home));
    }
    Ok(user_home()?.join(".config"))
}

pub(crate) fn loc_relay_home() -> io::Result<PathBuf> {
    if let Ok(home) = env::var("TAYD_HOME").or_else(|_| env::var("LOC_RELAY_HOME")) {
        let path = PathBuf::from(home);
        if path.exists() {
            return fs::canonicalize(path);
        }
        return Ok(path);
    }

    let exe = env::current_exe()?;
    let resolved = fs::canonicalize(&exe).unwrap_or(exe);
    Ok(resolved
        .parent()
        .and_then(Path::parent)
        .unwrap_or_else(|| Path::new("."))
        .to_path_buf())
}

pub(crate) fn write_private(path: PathBuf, data: String) -> Result<()> {
    fs::write(&path, data)?;
    #[cfg(unix)]
    {
        use std::os::unix::fs::PermissionsExt;
        fs::set_permissions(&path, fs::Permissions::from_mode(0o600))?;
    }
    Ok(())
}
