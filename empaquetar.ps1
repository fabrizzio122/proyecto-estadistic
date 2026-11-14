<#
  Empaqueta Business Analyzer Desktop para Windows con jpackage (incluido en el JDK 17).

  Uso (desde la carpeta del proyecto, en PowerShell):
      .\empaquetar.ps1               compila, ejecuta las pruebas y empaqueta
      .\empaquetar.ps1 -SinPruebas   empaqueta sin ejecutar las pruebas

  Resultado en target\instalador:
      Business Analyzer\                       aplicacion lista para usar (incluye su propio Java)
      BusinessAnalyzer-1.0.0-portable.zip      la misma carpeta comprimida, para copiar a otro equipo
      Business Analyzer-1.0.0.exe              instalador con acceso directo y menu Inicio
                                               (solo si WiX Toolset 3 esta instalado)

  Nota: el archivo no lleva tildes a proposito; Windows PowerShell 5.1 lee mal los scripts UTF-8 sin BOM.
#>
param([switch]$SinPruebas)

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$version = '1.0.0'
$nombre = 'Business Analyzer'
# Identificador fijo: permite que una version nueva del instalador actualice la anterior.
$uuidActualizacion = '6f1d3c2a-8b4e-4f7a-9c5d-2e8a7b1f0c43'

# --- JDK con jpackage -------------------------------------------------------------------
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

# --- 1. Compilar (y probar) ---------------------------------------------------------------
$argumentosMaven = @('-q', 'clean', 'package')
if ($SinPruebas) {
    $argumentosMaven += '-DskipTests'
}
Write-Host 'Compilando con Maven...'
& "$raiz\mvnw.cmd" @argumentosMaven
if ($LASTEXITCODE -ne 0) {
    throw 'La compilacion o las pruebas fallaron. Revisa los mensajes de Maven.'
}

# --- 2. Entrada de jpackage: jar principal y dependencias en una sola carpeta ---------------
$entrada = Join-Path $raiz 'target\jpackage-entrada'
$salida = Join-Path $raiz 'target\instalador'
Remove-Item -Recurse -Force $entrada, $salida -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force $entrada | Out-Null
New-Item -ItemType Directory -Force $salida | Out-Null
Copy-Item (Join-Path $raiz 'target\business-analyzer.jar') $entrada
Copy-Item (Join-Path $raiz 'target\lib\*.jar') $entrada

# Modulos del JDK que necesita la aplicacion (JavaFX, SQLite, JasperReports, POI, JFreeChart).
# jdk.localedata es imprescindible para los nombres de meses y formatos en espanol.
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
    '--main-class', 'com.businessanalyzer.Launcher',
    '--icon', (Join-Path $raiz 'src\main\packaging\business-analyzer.ico'),
    '--add-modules', $modulos,
    '--jlink-options', '--strip-debug --no-man-pages --no-header-files',
    '--java-options', '-Dfile.encoding=UTF-8',
    '--java-options', '-Xmx1024m',
    '--dest', $salida
)

# --- 3. Aplicacion portable -----------------------------------------------------------------
Write-Host 'Creando la aplicacion (app-image)...'
& $jpackage --type app-image @comunes
if ($LASTEXITCODE -ne 0) {
    throw 'jpackage no pudo crear la aplicacion.'
}
$zip = Join-Path $salida "BusinessAnalyzer-$version-portable.zip"
Compress-Archive -Path (Join-Path $salida $nombre) -DestinationPath $zip
Write-Host "Aplicacion portable: $(Join-Path $salida $nombre)"
Write-Host "Comprimida:          $zip"

# --- 4. Instalador .exe (requiere WiX Toolset 3.x) -------------------------------------------
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
    Write-Host 'Para generarlo instala WiX 3.14 (https://wixtoolset.org/docs/wix3/), agrega su carpeta bin'
    Write-Host 'al PATH y vuelve a ejecutar este script. La aplicacion portable ya funciona sin instalar nada.'
}
