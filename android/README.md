# Tayd Android

Java sources are grouped by feature and role:

- `app/`: app shell, navigation, and Android activity orchestration.
- `gateway/`: gateway settings, logs, startup policy, service lifecycle.
- `proxy/`: local HTTP proxy and SOCKS5 proxy domain/runtime engine.
- `frpc/`: embedded FRPC config, proxy mapping store, and process runtime.
- `webhook/`: WebHook channel model, store, HTTP client, and dispatcher.
- `event/`: Android event models, permissions, receivers, and notification listener.
- `qr/`: server QR payload parsing and scan UI.
- `network/`: network interface domain formatting and Android provider.
- `common/`: shared small utilities that do not belong to one feature.
