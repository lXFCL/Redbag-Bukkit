$ErrorActionPreference = 'Stop'

$serverJar = 'D:\catserver\1.12.2\CatServer-5a600445-universal.jar'
$sourceDir = Join-Path $PSScriptRoot 'src\main\java'
$resourceDir = Join-Path $PSScriptRoot 'src\main\resources'
$classesDir = Join-Path $PSScriptRoot 'target\classes'
$pluginYml = Join-Path $resourceDir 'plugin.yml'
$versionLine = Get-Content -Encoding UTF8 $pluginYml | Where-Object { $_ -match '^version:\s*(.+)$' } | Select-Object -First 1
if ($versionLine -notmatch '^version:\s*(.+)$') {
    throw "Plugin version not found in $pluginYml"
}
$version = $Matches[1].Trim()
$jarPath = Join-Path $PSScriptRoot "target\Redbag-$version.jar"

if (!(Test-Path $serverJar)) {
    throw "Server jar not found: $serverJar"
}

if (Test-Path $classesDir) {
    Remove-Item -LiteralPath $classesDir -Recurse -Force
}
New-Item -ItemType Directory -Force -Path $classesDir | Out-Null

$sources = Get-ChildItem -Path $sourceDir -Recurse -Filter '*.java' | ForEach-Object { $_.FullName }
if ($sources.Count -eq 0) {
    throw 'No Java source files found.'
}

javac -encoding UTF-8 -source 8 -target 8 -cp $serverJar -d $classesDir @sources
Copy-Item -Path (Join-Path $resourceDir '*') -Destination $classesDir -Recurse -Force

if (Test-Path $jarPath) {
    Remove-Item -LiteralPath $jarPath -Force
}
jar cf $jarPath -C $classesDir .

Write-Host "Built $jarPath"
