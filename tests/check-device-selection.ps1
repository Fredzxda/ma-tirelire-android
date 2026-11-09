$ErrorActionPreference = 'Stop'
. (Join-Path (Split-Path $PSScriptRoot -Parent) 'tools/Resolve-AndroidDevice.ps1')
function FakeAdb {
    $global:LASTEXITCODE = 0
    if ($args[0] -eq 'devices') { return $script:devices }
    if ($args[0] -eq '-s') { return $script:identities[$args[1]] }
    throw 'Commande ADB inattendue dans le test.'
}
$script:devices = @('List of devices attached', 'wifi-dynamic-1 device', 'samsung device')
$script:identities = @{ 'wifi-dynamic-1' = 'FAKE-ASUS'; samsung = 'FAKE-SAMSUNG' }
if ((Resolve-AndroidDevice FakeAdb 'FAKE-ASUS') -ne 'wifi-dynamic-1') { throw 'Mauvaise cible.' }
$script:devices = @('wifi-new-port device')
$script:identities = @{ 'wifi-new-port' = 'FAKE-ASUS' }
if ((Resolve-AndroidDevice FakeAdb 'FAKE-ASUS') -ne 'wifi-new-port') { throw 'Connexion dynamique non retrouvee.' }
foreach ($case in @('absent', 'unauthorized', 'ambiguous')) {
    $script:devices = switch ($case) {
        absent { @('samsung device') }
        unauthorized { @('wifi-new-port unauthorized') }
        ambiguous { @('wifi-new-port device', 'usb device') }
    }
    $script:identities = @{ samsung = 'FAKE-SAMSUNG'; 'wifi-new-port' = 'FAKE-ASUS'; usb = 'FAKE-ASUS' }
    $rejected = $false
    try { Resolve-AndroidDevice FakeAdb 'FAKE-ASUS' | Out-Null } catch { $rejected = $true }
    if (-not $rejected) { throw "Cas dangereux accepte : $case" }
}
Write-Output 'PASS: cible unique, connexion dynamique, absence, non-autorisation, ambiguite.'
