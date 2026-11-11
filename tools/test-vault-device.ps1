$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Resolve-AndroidDevice.ps1')
$project = Split-Path $PSScriptRoot -Parent
$configuration = Get-Content -LiteralPath (Join-Path $PSScriptRoot 'device.local.json') -Raw -Encoding UTF8 | ConvertFrom-Json
$adb = Join-Path $env:LOCALAPPDATA 'Android/Sdk/platform-tools/adb.exe'
$device = Resolve-AndroidDevice $adb $configuration.hardwareSerial
$apk = Join-Path $project 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
if (-not (Test-Path -LiteralPath $apk)) { throw 'Compiler assembleDebugAndroidTest avant ce controle.' }
& $adb -s $device install -r $apk
if ($LASTEXITCODE -ne 0) { throw 'Installation du test impossible.' }
$result = @(& $adb -s $device shell am instrument -w -r -e class 'org.matirelire.android.VaultStorageDeviceTest,org.matirelire.android.OperationsUiDeviceTest' org.matirelire.android.demo.test/androidx.test.runner.AndroidJUnitRunner)
$result | Write-Output
if ($LASTEXITCODE -ne 0 -or -not ($result -match 'OK \([1-9][0-9]* tests?\)') -or ($result -match 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed')) {
    throw 'Tests de stockage ou interface sur appareil en echec.'
}
$launch = @(& $adb -s $device shell am start -W -n org.matirelire.android.demo/org.matirelire.android.MainActivity)
if ($LASTEXITCODE -ne 0 -or -not ($launch -match '^Status:\s*ok\s*$')) { throw 'Lancement apres test non confirme.' }
Write-Output 'Controles SQLCipher, Keystore et interface reussis ; application relancee.'
