param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$DeviceSerial
)
$ErrorActionPreference = 'Stop'
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
if (-not (Test-Path -LiteralPath $adb)) { throw 'ADB absent : verifier Android SDK Platform Tools.' }
$state = & $adb -s $DeviceSerial get-state 2>$null
if ($LASTEXITCODE -ne 0 -or "$state".Trim() -ne 'device') {
    throw 'Telephone absent ou non autorise. Verifier le cable et accepter le debogage USB sur le telephone.'
}
$project = Split-Path $PSScriptRoot -Parent
& (Join-Path $PSScriptRoot 'build.ps1')
$apk = Join-Path $project 'app/build/outputs/apk/debug/app-debug.apk'
$metadata = Get-Content -LiteralPath (Join-Path $project 'app/build/outputs/apk/debug/output-metadata.json') -Raw -Encoding UTF8 | ConvertFrom-Json
if ($metadata.applicationId -ne 'org.matirelire.android.demo' -or $metadata.elements[0].versionName -notmatch '-dev$') {
    throw 'Installation refusee : APK inattendu ou version sans suffixe -dev.'
}
& $adb -s $DeviceSerial install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'Installation echouee. Aucune desinstallation automatique ne sera tentee.' }
$installed = & $adb -s $DeviceSerial shell dumpsys package org.matirelire.android.demo
if ($LASTEXITCODE -ne 0) { throw 'Impossible de verifier la version installee.' }
$expected = '^\s*versionName=' + [regex]::Escape($metadata.elements[0].versionName) + '\s*$'
if (-not ($installed | Select-String -Pattern $expected)) { throw 'La version installee ne correspond pas a l''APK genere.' }
Write-Output ('Installation verifiee : ' + $metadata.elements[0].versionName)
