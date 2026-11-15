<#
  Genera las imagenes PNG y SVG de los diagramas UML (docs\uml\*.puml).

  Uso (desde la carpeta del proyecto, en PowerShell):
      .\docs\uml\generar.ps1

  Descarga PlantUML con el wrapper de Maven la primera vez. No necesita Graphviz
  (los diagramas usan el motor smetana incluido en PlantUML).

  Nota: el archivo no lleva tildes a proposito; Windows PowerShell 5.1 lee mal los scripts UTF-8 sin BOM.
#>
$ErrorActionPreference = 'Stop'
$carpeta = $PSScriptRoot
$raiz = Resolve-Path (Join-Path $carpeta '..\..')
$versionPlantUml = '1.2024.7'

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    $jdk = Get-ChildItem "$env:USERPROFILE\.jdks" -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '17' } | Select-Object -First 1
    if (-not $jdk) {
        throw 'No se encontro un JDK 17. Define la variable JAVA_HOME.'
    }
    $env:JAVA_HOME = $jdk.FullName
}
$java = "$env:JAVA_HOME\bin\java.exe"

$repositorio = if ($env:MAVEN_REPO) { $env:MAVEN_REPO } else { "$env:USERPROFILE\.m2\repository" }
$jar = "$repositorio\net\sourceforge\plantuml\plantuml\$versionPlantUml\plantuml-$versionPlantUml.jar"
if (-not (Test-Path $jar)) {
    Write-Host "Descargando PlantUML $versionPlantUml..."
    & "$raiz\mvnw.cmd" -q dependency:get "-Dartifact=net.sourceforge.plantuml:plantuml:$versionPlantUml"
    if ($LASTEXITCODE -ne 0) {
        throw 'No se pudo descargar PlantUML.'
    }
}

$diagramas = Get-ChildItem $carpeta -Filter '*.puml' | Sort-Object Name | ForEach-Object { $_.FullName }
foreach ($formato in 'png', 'svg') {
    Write-Host "Generando $formato..."
    & $java '-Djava.awt.headless=true' '-DPLANTUML_LIMIT_SIZE=16384' -jar $jar -charset UTF-8 "-t$formato" -o (Join-Path $carpeta $formato) @diagramas
    if ($LASTEXITCODE -ne 0) {
        throw "PlantUML fallo al generar $formato. Revisa los mensajes anteriores."
    }
}
Write-Host "Listo: $carpeta\png y $carpeta\svg"
