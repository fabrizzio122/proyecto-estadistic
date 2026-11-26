param([switch]$SinPruebas)

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$nombre = 'Business Analyzer'

$pom = [xml](Get-Content -Raw -Encoding UTF8 (Join-Path $raiz 'pom.xml'))
$version = $pom.project.version
if ($version -notmatch '^\d+\.\d+\.\d+$') {
    throw "La version del pom.xml ('$version') debe tener el formato x.y.z (por ejemplo 1.0.1) para jpackage."
}
Write-Host "Version: $version"
$uuidActualizacion = '6f1d3c2a-8b4e-4f7a-9c5d-2e8a7b1f0c43'

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) {
    $jdk = Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '17' -and (Test-Path "$($_.FullName)\bin\jpackage.exe") } |
            Select-Object -First 1
    if (-not $jdk) {
        throw 'No se encontro un JDK 17 con jpackage. Define la variable JAVA_HOME.'
    }
    $env:JAVA_HOME = $jdk.FullName
}
$jpackage = "$env:JAVA_HOME\bin\jpackage.exe"
Write-Host "JDK: $env:JAVA_HOME"

$argumentosMaven = @('-q', 'clean', 'package')
if ($SinPruebas) {
    $argumentosMaven += '-DskipTests'
}
Write-Host 'Compilando con Maven...'
& "$raiz\mvnw.cmd" @argumentosMaven
if ($LASTEXITCODE -ne 0) {
    throw 'La compilacion o las pruebas fallaron. Revisa los mensajes de Maven.'
}

$entrada = Join-Path $raiz 'target\jpackage-entrada'
$salida = Join-Path $raiz 'target\instalador'
Remove-Item -Recurse -Force $entrada, $salida -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $entrada | Out-Null
New-Item -ItemType Directory -Force $salida | Out-Null
Copy-Item (Join-Path $raiz 'target\business-analyzer.jar') $entrada
Copy-Item (Join-Path $raiz 'target\lib\*.jar') $entrada

$modulos = @(
    'java.base', 'java.compiler', 'java.datatransfer', 'java.desktop', 'java.logging', 'java.management',
    'java.naming', 'java.net.http', 'java.prefs', 'java.scripting', 'java.security.jgss', 'java.sql',
    'java.sql.rowset', 'java.xml', 'java.xml.crypto', 'jdk.charsets', 'jdk.crypto.ec', 'jdk.jfr',
    'jdk.localedata', 'jdk.unsupported', 'jdk.unsupported.desktop', 'jdk.zipfs'
) -join ','

$comunes = @(
    '--name', $nombre,
    '--app-version', $version,
    '--vendor', 'Business Analyzer',
    '--description', 'Analisis financiero, estadistico y predictivo para negocios',
    '--copyright', 'Business Analyzer Desktop 1.0',
    '--input', $entrada,
    '--main-jar', 'business-analyzer.jar',
    '--main-class', 'com.businessanalyzer.LauncherFGEM',
    '--icon', (Join-Path $raiz 'src\main\packaging\business-analyzer.ico'),
    '--add-modules', $modulos,
    '--jlink-options', '--strip-debug --no-man-pages --no-header-files',
    '--java-options', '-Dfile.encoding=UTF-8',
    '--java-options', '-Xmx1024m',
    '--dest', $salida
)

Write-Host 'Creando la aplicacion (app-image)...'
& $jpackage --type app-image @comunes
if ($LASTEXITCODE -ne 0) {
    throw 'jpackage no pudo crear la aplicacion.'
}
$zip = Join-Path $salida "BusinessAnalyzer-$version-portable.zip"
Compress-Archive -Path (Join-Path $salida $nombre) -DestinationPath $zip
Write-Host "Aplicacion portable: $(Join-Path $salida $nombre)"
Write-Host "Comprimida:          $zip"

if (-not (Get-Command candle.exe -ErrorAction SilentlyContinue)) {
    $candidatas = @("$env:USERPROFILE\tools\wix314")
    if ($env:WIX) {
        $candidatas += (Join-Path $env:WIX 'bin')
    }
    foreach ($carpetaWix in $candidatas) {
        if (Test-Path (Join-Path $carpetaWix 'candle.exe')) {
            $env:PATH = "$carpetaWix;$env:PATH"
            Write-Host "WiX: $carpetaWix"
            break
        }
    }
}
if (Get-Command candle.exe -ErrorAction SilentlyContinue) {
    Write-Host 'Creando el instalador .exe...'
    & $jpackage --type exe @comunes `
        --win-menu --win-menu-group $nombre `
        --win-shortcut --win-shortcut-prompt `
        --win-dir-chooser --win-per-user-install `
        --win-upgrade-uuid $uuidActualizacion
    if ($LASTEXITCODE -ne 0) {
        throw 'jpackage no pudo crear el instalador.'
    }
    Write-Host "Instalador: $(Join-Path $salida "$nombre-$version.exe")"
} else {
    Write-Host ''
    Write-Host 'WiX Toolset 3 no esta instalado: se omitio el instalador .exe.'
    Write-Host 'Para generarlo descomprime wix314-binaries.zip (https://github.com/wixtoolset/wix3/releases)'
    Write-Host 'en %USERPROFILE%\tools\wix314 y vuelve a ejecutar este script.'
    Write-Host 'La aplicacion portable ya funciona sin instalar nada.'
}
