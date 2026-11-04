<#
.SYNOPSIS
    Compiles src/ and test/, then runs the checks.
.DESCRIPTION
    No test framework is involved: each check class has a main method that
    prints failures and exits non-zero if any check failed. Register a new
    check class in $checkClasses below.
#>
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Jdk.ps1"

& (Join-Path $PSScriptRoot 'build.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$root        = Split-Path -Parent $PSScriptRoot
$classes     = Join-Path $root 'out\classes'
$testClasses = Join-Path $root 'out\test-classes'

$testSources = Get-ChildItem (Join-Path $root 'test') -Filter *.java -Recurse |
    Select-Object -ExpandProperty FullName
if (-not $testSources) {
    Write-Output 'No check classes found under test/.'
    exit 0
}

New-Item -ItemType Directory -Force $testClasses | Out-Null
& (Find-JdkTool 'javac') -encoding UTF-8 -Xlint:all -cp $classes -d $testClasses $testSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$java = Find-JdkTool 'java'
$checkClasses = @(
    'backgammon.trivia.TriviaChecks'
)

$failed = 0
foreach ($class in $checkClasses) {
    Write-Output "--- $class"
    & $java -cp "$classes;$testClasses" $class
    if ($LASTEXITCODE -ne 0) { $failed++ }
}

if ($failed -gt 0) {
    Write-Output "$failed check class(es) reported failures."
    exit 1
}
Write-Output 'All checks passed.'
