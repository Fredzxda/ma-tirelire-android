param([string[]]$Tasks = @('assembleDebug', 'testDebugUnitTest', 'lintDebug'))
$ErrorActionPreference = 'Stop'
$studioJava = 'C:\Program Files\Android\Android Studio\jbr'
if (-not (Test-Path -LiteralPath "$studioJava/bin/java.exe")) { throw 'Configurer le chemin du JDK Android Studio dans tools/build.ps1.' }
$env:JAVA_HOME = $studioJava
$env:PATH = "$studioJava/bin;$env:PATH"
Push-Location (Split-Path $PSScriptRoot -Parent)
try { & .\gradlew.bat @Tasks; if ($LASTEXITCODE -ne 0) { throw "Gradle a echoue : $LASTEXITCODE" } }
finally { Pop-Location }
