<#
.SYNOPSIS
    Generates the javadoc for every package into docs/api (git-ignored).
#>
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Jdk.ps1"

$root = Split-Path -Parent $PSScriptRoot
$sources = Get-ChildItem (Join-Path $root 'src') -Filter *.java -Recurse |
    Select-Object -ExpandProperty FullName

& (Find-JdkTool 'javadoc') -encoding UTF-8 -quiet -d (Join-Path $root 'docs\api') $sources
