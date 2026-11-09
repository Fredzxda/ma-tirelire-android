function Resolve-AndroidDevice {
    param([string]$AdbCommand, [string]$HardwareSerial)
    if ([string]::IsNullOrWhiteSpace($HardwareSerial)) { throw 'Identite du telephone manquante.' }
    $devices = @(& $AdbCommand devices)
    if ($LASTEXITCODE -ne 0) { throw 'Impossible de lister les appareils ADB.' }
    $candidates = @()
    foreach ($line in $devices) {
        if ($line -match '^(\S+)\s+device\s*$') {
            $connection = $Matches[1]
            $identity = @(& $AdbCommand -s $connection shell getprop ro.serialno)
            if ($LASTEXITCODE -eq 0 -and ($identity -join '').Trim() -ceq $HardwareSerial) {
                $candidates += $connection
            }
        }
    }
    if ($candidates.Count -ne 1) {
        throw 'ASUS cible absent, non autorise ou connexions multiples. Garder une seule connexion ADB vers ce telephone.'
    }
    return $candidates[0]
}
