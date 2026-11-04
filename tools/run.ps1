<#
.SYNOPSIS
    Builds the project, then runs it.
#>
$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\Jdk.ps1"

& (Join-Path $PSScriptRoot 'build.ps1')
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$root = Split-Path -Parent $PSScriptRoot
& (Find-JdkTool 'java') -cp (Join-Path $root 'out\classes') backgammon.Main
