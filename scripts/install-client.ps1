param(
    [Parameter(Mandatory = $true)]
    [string]$ServerAddr,

    [Parameter(Mandatory = $true)]
    [string]$Token,

    [int]$ServerPort = 7000,
    [string]$ProxyName = "",
    [string]$ProxyType = "",
    [string]$LocalIP = "127.0.0.1",
    [int]$LocalPort = 0,
    [string]$LocalEndpoint = "",
    [int]$RemotePort = 0,
    [string]$InstallDir = "",
    [string]$BinDir = (Join-Path $HOME ".local\bin"),
    [string]$RepoUrl = "https://github.com/cclilshy/loc-relay.git",
    [string]$FrpVersion = "0.69.0",
    [switch]$SkipStart,
    [switch]$SkipFrpDownload
)

$ErrorActionPreference = "Stop"

if ($env:INSTALL_DIR) { $InstallDir = $env:INSTALL_DIR }
elseif ($env:CLIENT_INSTALL_DIR) { $InstallDir = $env:CLIENT_INSTALL_DIR }
elseif ($InstallDir -eq "") {
    $taydClientDir = Join-Path $HOME ".tayd-client"
    $legacyClientDir = Join-Path $HOME ".loc-relay-client"
    if (Test-Path $taydClientDir) { $InstallDir = $taydClientDir }
    elseif (Test-Path $legacyClientDir) { $InstallDir = $legacyClientDir }
    else { $InstallDir = $taydClientDir }
}
if ($env:BIN_DIR) { $BinDir = $env:BIN_DIR }
if ($env:REPO_URL) { $RepoUrl = $env:REPO_URL }
if ($env:FRP_VERSION) { $FrpVersion = $env:FRP_VERSION }
if ($env:PROXY_NAME) { $ProxyName = $env:PROXY_NAME }
if ($env:PROXY_TYPE) { $ProxyType = $env:PROXY_TYPE }
if ($env:LOCAL_IP) { $LocalIP = $env:LOCAL_IP }
if ($env:LOCAL_PORT) { $LocalPort = [int]$env:LOCAL_PORT }
if ($env:LOCAL_ENDPOINT) { $LocalEndpoint = $env:LOCAL_ENDPOINT }
if ($env:REMOTE_PORT) { $RemotePort = [int]$env:REMOTE_PORT }
if ($env:SKIP_START -eq "1") { $SkipStart = $true }
if ($env:SKIP_FRP_DOWNLOAD -eq "1") { $SkipFrpDownload = $true }

function Require-Command {
    param([string]$Name)
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "$Name is required"
    }
}

function Get-TaydArch {
    switch ($env:PROCESSOR_ARCHITECTURE) {
        "AMD64" { return "amd64" }
        "ARM64" { return "arm64" }
        default { throw "unsupported architecture: $env:PROCESSOR_ARCHITECTURE" }
    }
}

function Clone-Or-Update {
    Require-Command git

    $gitDir = Join-Path $InstallDir ".git"
    if (Test-Path $gitDir) {
        git -C $InstallDir pull --ff-only
        return
    }

    if (Test-Path $InstallDir) {
        throw "$InstallDir exists but is not a git repo"
    }

    git clone $RepoUrl $InstallDir
}

function Select-TaydBinary {
    $arch = Get-TaydArch
    $candidates = @(
        (Join-Path $InstallDir "bin\tayd-windows-$arch.exe"),
        (Join-Path $InstallDir "bin\loc-relay-windows-$arch.exe"),
        (Join-Path $InstallDir "bin\tayd.exe"),
        (Join-Path $InstallDir "bin\loc-relay.exe")
    )

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            return $candidate
        }
    }

    throw "no prebuilt tayd binary for windows-$arch; run ./build.sh before publishing"
}

function Install-Frp {
    if ($SkipFrpDownload) {
        return
    }

    $arch = Get-TaydArch
    $archive = "frp_${FrpVersion}_windows_${arch}.zip"
    $url = "https://github.com/fatedier/frp/releases/download/v${FrpVersion}/${archive}"
    $tmpDir = Join-Path ([System.IO.Path]::GetTempPath()) ("tayd-" + [System.Guid]::NewGuid().ToString("N"))
    $zip = Join-Path $tmpDir $archive

    New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null
    try {
        Invoke-WebRequest -Uri $url -OutFile $zip
        Expand-Archive -Path $zip -DestinationPath $tmpDir -Force

        $frpDir = Get-ChildItem -Path $tmpDir -Directory -Filter "frp_${FrpVersion}_windows_${arch}" | Select-Object -First 1
        if (-not $frpDir) {
            throw "could not find extracted FRP directory"
        }

        $bin = Join-Path $InstallDir "bin"
        New-Item -ItemType Directory -Force -Path $bin | Out-Null
        Copy-Item (Join-Path $frpDir.FullName "frpc.exe") (Join-Path $bin "frpc.exe") -Force
        Copy-Item (Join-Path $frpDir.FullName "frps.exe") (Join-Path $bin "frps.exe") -Force
    }
    finally {
        Remove-Item $tmpDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

function Install-TaydCommand {
    param([string]$TaydBin)

    New-Item -ItemType Directory -Force -Path $BinDir | Out-Null
    $cmd = Join-Path $BinDir "tayd.cmd"
    "@echo off`r`n`"$TaydBin`" %*`r`n" | Set-Content -Path $cmd -Encoding ASCII
    $legacyCmd = Join-Path $BinDir "loc-relay.cmd"
    Remove-Item $legacyCmd -Force -ErrorAction SilentlyContinue
    return $cmd
}

Clone-Or-Update

$taydBin = Select-TaydBinary
Install-Frp

& $taydBin init `
    --server $ServerAddr `
    --token $Token `
    --server-port $ServerPort

$proxyConfigured = ($ProxyName -ne "") -or ($LocalEndpoint -ne "") -or ($LocalPort -ne 0) -or ($RemotePort -ne 0)
if ($proxyConfigured) {
    if ($ProxyName -eq "") { throw "PROXY_NAME is required when configuring an initial mapping" }
    if ($RemotePort -eq 0) { throw "REMOTE_PORT is required when configuring an initial mapping" }
    if ($LocalEndpoint -eq "") {
        if ($LocalPort -eq 0) { throw "LOCAL_PORT or LOCAL_ENDPOINT is required when configuring an initial mapping" }
        if ($LocalIP -eq "127.0.0.1") {
            $LocalEndpoint = [string]$LocalPort
        }
        else {
            $LocalEndpoint = "${LocalIP}:${LocalPort}"
        }
    }

    & $taydBin remove $ProxyName --no-restart *> $null
    $addArgs = @("add", $ProxyName, $LocalEndpoint, [string]$RemotePort)
    if ($ProxyType -ne "") {
        $addArgs += @("--type", $ProxyType)
    }
    $addArgs += "--no-restart"
    & $taydBin @addArgs
}

New-Item -ItemType File -Force -Path (Join-Path $InstallDir ".tayd-client") | Out-Null
$taydCmd = Install-TaydCommand $taydBin

if (-not $SkipStart) {
    & $taydBin restart
    Write-Output "[restarted] client gateway"
}
else {
    Write-Output "[saved] start skipped"
}

Write-Output "[installed] tayd client -> $InstallDir"
Write-Output "[server] ${ServerAddr}:${ServerPort}"
Write-Output "[command] $taydCmd"
if ($proxyConfigured) {
    Write-Output "[proxy] $ProxyName $LocalEndpoint -> $RemotePort"
}
Write-Output "[uninstall] & `"$taydCmd`" uninstall"
