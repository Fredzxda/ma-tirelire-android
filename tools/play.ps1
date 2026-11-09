$ErrorActionPreference = 'Stop'
$configPath = Join-Path $PSScriptRoot 'device.local.json'
if (-not (Test-Path -LiteralPath $configPath)) { throw 'Configurer tools/device.local.json avec hardwareSerial du telephone autorise.' }
$config = Get-Content -LiteralPath $configPath -Raw -Encoding UTF8 | ConvertFrom-Json
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if (-not (Test-Path -LiteralPath $adb)) { throw 'Android SDK Platform Tools absent.' }
. (Join-Path $PSScriptRoot 'Resolve-AndroidDevice.ps1')
$connection = Resolve-AndroidDevice -AdbCommand $adb -HardwareSerial $config.hardwareSerial
& (Join-Path $PSScriptRoot 'build-install.ps1') -DeviceSerial $connection -IncludeDeviceTestApk
& (Join-Path $PSScriptRoot 'test-vault-device.ps1')
