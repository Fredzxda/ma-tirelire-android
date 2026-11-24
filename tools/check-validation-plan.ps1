$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'Get-ValidationPlan.ps1')
foreach ($name in @('play.ps1','build-install.ps1','test-vault-device.ps1','Get-ValidationPlan.ps1')) {
    $tokens = $null; $parseErrors = $null
    $null = [System.Management.Automation.Language.Parser]::ParseFile((Join-Path $PSScriptRoot $name), [ref]$tokens, [ref]$parseErrors)
    if ($parseErrors.Count) { throw ($parseErrors | Out-String) }
}
$count = 0
foreach ($suite in @('None','NextDate','Icons','Forms','Operations','Storage')) {
    $plan = Get-ValidationPlan -Suite $suite
    if ($plan.AllUnitTests) { throw "$suite ne doit pas demander tous les tests unitaires." }
    if ($suite -eq 'None' -and ($plan.DeviceClasses -or $plan.UnitTests.Count)) { throw 'None doit etre vide.' }
    $preview = & (Join-Path $PSScriptRoot 'play.ps1') -Suite $suite -PlanOnly | ConvertFrom-Json
    if ($preview.DeviceClasses -ne $plan.DeviceClasses) { throw 'Play ne respecte pas le plan.' }
    $tasks = & (Join-Path $PSScriptRoot 'build-install.ps1') -DeviceSerial 'plan-only' -Suite $suite -PlanOnly | ConvertFrom-Json
    if ($suite -eq 'None' -and 'testDebugUnitTest' -in $tasks) { throw 'None lance des tests unitaires.' }
    if ($suite -ne 'None' -and @($tasks | Where-Object { $_ -eq '--tests' }).Count -ne $plan.UnitTests.Count) { throw 'Filtres unitaires absents.' }
    foreach ($deviceClass in ($plan.DeviceClasses -split ',' | Where-Object { $_ })) {
        $bits = $deviceClass -split '#'
        $file = Join-Path (Split-Path $PSScriptRoot -Parent) ('app/src/androidTest/java/' + $bits[0].Replace('.','/') + '.kt')
        if (-not (Test-Path -LiteralPath $file)) { throw "Classe absente : $deviceClass" }
        if ($bits.Count -gt 1 -and (Get-Content -LiteralPath $file -Raw -Encoding UTF8) -notmatch ([regex]::Escape('fun ' + $bits[1] + '('))) { throw "Methode absente : $deviceClass" }
    }
    $count++
}
$rejected = $false
try { & (Join-Path $PSScriptRoot 'play.ps1') -PlanOnly } catch { $rejected = $true }
if (-not $rejected) { throw 'Suite omise acceptee.' }
$rejected = $false
try { Get-ValidationPlan -Suite Full } catch { $rejected = $true }
if (-not $rejected) { throw 'Full sans justification accepte.' }
$full = Get-ValidationPlan -Suite Full -FullReason 'Verification du plan uniquement, aucun test execute'
if (-not $full.AllUnitTests) { throw 'Plan Full invalide.' }
Write-Output "$count plans cibles verifies, omission et Full sans justification refuses. Aucun ADB, build ou test telephone execute."
