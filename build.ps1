param([Parameter(ValueFromRemainingArguments=$true)][string[]]$GradleArgs)
$ErrorActionPreference = 'Stop'
$previousJava = $env:JAVA_HOME
$exitCode = 1
Push-Location $PSScriptRoot
try {
    $localJdk = Get-ChildItem -LiteralPath (Join-Path $PSScriptRoot 'tools/jdk17') -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path (Join-Path $_.FullName 'bin/javac.exe') } | Select-Object -First 1
    if ($localJdk) { $env:JAVA_HOME = $localJdk.FullName }
    if (-not $GradleArgs) { $GradleArgs = @('build', '--console=plain') }
    & (Join-Path $PSScriptRoot 'gradlew.bat') @GradleArgs
    $exitCode = $LASTEXITCODE
} finally {
    $env:JAVA_HOME = $previousJava
    Pop-Location
}
exit $exitCode
