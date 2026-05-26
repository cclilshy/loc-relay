use serde::{Deserialize, Serialize};

pub(crate) const STATE_VERSION: u8 = 1;

#[derive(Debug, Serialize, Deserialize, Clone)]
pub(crate) struct State {
    pub(crate) version: u8,
    pub(crate) server: Server,
    pub(crate) proxies: Vec<Proxy>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub(crate) struct Server {
    pub(crate) addr: String,
    pub(crate) port: u16,
    pub(crate) token: String,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub(crate) struct ServerInstallInfo {
    pub(crate) addr: String,
    pub(crate) port: u16,
    pub(crate) token: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) http_port: Option<u16>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) https_port: Option<u16>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) raw_base_url: Option<String>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub(crate) struct Proxy {
    pub(crate) name: String,
    #[serde(default, skip_serializing_if = "Vec::is_empty")]
    pub(crate) types: Vec<String>,
    #[serde(default, rename = "type", skip_serializing_if = "Option::is_none")]
    pub(crate) legacy_type: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) local_scheme: Option<String>,
    pub(crate) local_ip: String,
    pub(crate) local_port: u16,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) remote_port: Option<u16>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) remote_scheme: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) remote_domain: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) remote_display_port: Option<u16>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) group: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) group_key: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) plugin: Option<ProxyPlugin>,
}

#[derive(Debug, Serialize, Deserialize, Clone)]
pub(crate) struct ProxyPlugin {
    #[serde(rename = "type")]
    pub(crate) plugin_type: String,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) crt_path: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) key_path: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub(crate) host_header_rewrite: Option<String>,
}
