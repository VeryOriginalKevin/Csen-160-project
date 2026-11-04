<#
.SYNOPSIS
    Locates a JDK. Dot-source this from the other scripts: . "$PSScriptRoot\Jdk.ps1"
.DESCRIPTION
    The `java` on PATH here is an old 1.8 JRE that cannot run classes compiled
    for 17, and it has no compiler at all, so every script looks a JDK up rather
    than trusting PATH. Search order: JAVA_HOME, then the JDKs IntelliJ keeps
    under ~/.jdks (newest first), then IntelliJ's own bundled runtime.
#>

function Find-JdkTool {
    param(
        # Tool to find, without the .exe: javac, java, or javadoc.
        [Parameter(Mandatory = $true)]
        [string] $Name
    )

    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\$Name.exe")) {
        return "$env:JAVA_HOME\bin\$Name.exe"
    }

    $candidates = @()
    if (Test-Path "$env:USERPROFILE\.jdks") {
        $candidates += Get-ChildItem "$env:USERPROFILE\.jdks" -Directory |
            Sort-Object Name -Descending |
            ForEach-Object { "$($_.FullName)\bin\$Name.exe" }
    }
    $candidates += Get-ChildItem 'C:\Program Files\JetBrains' -Directory -ErrorAction SilentlyContinue |
        ForEach-Object { "$($_.FullName)\jbr\bin\$Name.exe" }

    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) { return $candidate }
    }

    throw "No JDK found ($Name). Install JDK 17+ and set JAVA_HOME, or add one through IntelliJ."
}
