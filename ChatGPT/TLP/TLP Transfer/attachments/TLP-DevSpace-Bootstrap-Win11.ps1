# TLP DevSpace Bootstrap - Windows 11, interactive and non-elevated by default.
# Review before running. Installs only with explicit confirmation.
$ErrorActionPreference = 'Stop'
function Say($msg) { Write-Host "`n[TLP] $msg" -ForegroundColor Cyan }
function Ask($msg) { (Read-Host "$msg [y/N]").Trim().ToLowerInvariant() -eq 'y' }
function Refresh-Path {
  $env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')
}
Say 'Checking operating system'
$os = Get-CimInstance Win32_OperatingSystem
Write-Host "OS: $($os.Caption) $($os.Version)"
if ($os.Caption -notmatch 'Windows 11') { Write-Warning 'This installer was designed for Windows 11. Exiting.'; exit 1 }
$root = Join-Path $env:USERPROFILE 'TLP-Workspace'
if (!(Test-Path $root)) { New-Item -ItemType Directory -Path $root | Out-Null }
Write-Host "Workspace: $root"
Say 'Checking Git, Node.js, npm, and Bash'
foreach ($cmd in @('git','node','npm','bash','winget')) {
  $found = Get-Command $cmd -ErrorAction SilentlyContinue
  Write-Host ("{0}: {1}" -f $cmd, $(if($found){$found.Source}else{'NOT FOUND'}))
}
if (!(Get-Command winget -ErrorAction SilentlyContinue)) {
  Write-Warning 'winget not found. Install/update App Installer from Microsoft, then rerun.'
  exit 2
}
if (!(Get-Command git -ErrorAction SilentlyContinue)) {
  if (Ask 'Install Git for Windows using winget?') {
    winget install --id Git.Git --exact --source winget --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) { throw 'Git installation failed' }
    Refresh-Path
  }
}
$needNode = $true
if (Get-Command node -ErrorAction SilentlyContinue) {
  $version = (& node --version).Trim().TrimStart('v')
  $major = [int]($version.Split('.')[0]); $minor = [int]($version.Split('.')[1])
  if (($major -gt 22 -and $major -lt 27) -or ($major -eq 22 -and $minor -ge 19)) { $needNode = $false }
}
if ($needNode) {
  Write-Warning 'DevSpace requires Node >=22.19 and <27. An existing Node installation may need updating.'
  if (Ask 'Install or upgrade Node.js 22 LTS using winget?') {
    winget install --id OpenJS.NodeJS.LTS --exact --source winget --accept-package-agreements --accept-source-agreements
    if ($LASTEXITCODE -ne 0) { Write-Warning 'Node installation failed or requires manual upgrade'; exit 3 }
    Refresh-Path
  }
}
$gitBash = Join-Path $env:ProgramFiles 'Git\bin\bash.exe'
if (!(Test-Path $gitBash)) { $gitBash = Join-Path $env:ProgramFiles 'Git\usr\bin\bash.exe' }
if (!(Test-Path $gitBash)) { Write-Warning 'Git Bash not found. Install Git for Windows, then rerun.'; exit 4 }
$env:Path = (Split-Path $gitBash) + ';' + $env:Path
if (!(Get-Command node -ErrorAction SilentlyContinue) -or !(Get-Command npm -ErrorAction SilentlyContinue)) {
  Write-Warning 'Node/npm not available in this shell. Open a new PowerShell window and rerun.'; exit 5
}
Say 'Environment versions'
& node --version
& npm --version
& git --version
& $gitBash --version | Select-Object -First 1
Say 'DevSpace setup (optional)'
Write-Host 'Next command runs the official npm package interactively: npx --yes @waishnav/devspace@1.0.8 init'
Write-Host 'Choose ChatGPT; allow only the TLP-Workspace folder. Do not expose C:\ or your user home.'
Write-Host 'You will need a secure HTTPS tunnel URL; the script does not create or expose a tunnel.'
if (Ask 'Run DevSpace init now?') {
  & npx --yes '@waishnav/devspace@1.0.8' init
  if ($LASTEXITCODE -ne 0) { Write-Warning 'DevSpace initialization did not complete.' }
}
if (Ask 'Run DevSpace doctor?') {
  & npx --yes '@waishnav/devspace@1.0.8' doctor
}
Say 'Finished. No tunnel, service, or background task was started.'
Write-Host 'After tunnel and OAuth are configured, start with: npx @waishnav/devspace@1.0.8 serve'
Write-Host 'Keep your Owner password and auth.json private. Never paste them into chat.'
