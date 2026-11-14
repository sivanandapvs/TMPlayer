# Runs Veylora.exe --self-test and fails unless it passes. The jpackage launcher is a GUI
# program, so the report is read back from the file the self-test writes rather than from stdout.
param([Parameter(Mandatory = $true)][string]$Exe)

$ErrorActionPreference = 'Stop'
if (-not (Test-Path $Exe)) { throw "No launcher at $Exe" }
$report = Join-Path $env:RUNNER_TEMP ("selftest-" + [guid]::NewGuid() + ".txt")
$p = Start-Process -FilePath $Exe -ArgumentList "`"--self-test=$report`"" -Wait -PassThru -WindowStyle Hidden
if (-not (Test-Path $report)) { throw "No self-test report written by $Exe" }
$result = Get-Content -Raw $report
Write-Host $result
Write-Host "exit code $($p.ExitCode) from $Exe"
if ($p.ExitCode -ne 0) { exit 1 }
if ($result -notmatch '(?m)^SELF-TEST PASSED\r?$') { throw "Self-test did not pass: $report" }
