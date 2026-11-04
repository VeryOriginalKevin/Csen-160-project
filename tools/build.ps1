<#
.SYNOPSIS
    Compiles the project to out/classes.
.DESCRIPTION
    Compiles everything under src/ with warnings on, then copies resources/
    beside the classes, if that directory exists, so getResource() finds them.
    Run from anywhere; paths resolve relative to this script.
#>
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Jdk.ps1"

$root    = Split-Path -Parent $PSScriptRoot
$classes = Join-Path $root 'out\classes'
$javac   = Find-JdkTool 'javac'

New-Item -ItemType Directory -Force $classes | Out-Null
$sources = Get-ChildItem (Join-Path $root 'src') -Filter *.java -Recurse |
    Select-Object -ExpandProperty FullName

Write-Output "javac: $javac"
& $javac -encoding UTF-8 -Xlint:all -d $classes $sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$resources = Join-Path $root 'resources'
if (Test-Path $resources) {
    Get-ChildItem $resources | ForEach-Object {
        Copy-Item $_.FullName $classes -Recurse -Force
    }
}
Write-Output "Compiled $($sources.Count) file(s) to $classes"
