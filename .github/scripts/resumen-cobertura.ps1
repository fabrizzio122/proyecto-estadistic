<#
  Resume la cobertura de pruebas (JaCoCo) por capa a partir de target/site/jacoco/jacoco.csv.

  Uso, despues de ".\mvnw.cmd test":
      .\.github\scripts\resumen-cobertura.ps1                 muestra la tabla en la consola
      .\.github\scripts\resumen-cobertura.ps1 -Markdown       la escribe en Markdown (GitHub Actions)

  Nota: el archivo no lleva tildes a proposito; Windows PowerShell 5.1 lee mal los scripts UTF-8 sin BOM.
#>
param([switch]$Markdown, [string]$Csv = 'target/site/jacoco/jacoco.csv')

$ErrorActionPreference = 'Stop'
if (-not (Test-Path $Csv)) { throw "No existe $Csv. Ejecuta antes: mvnw test" }
$filas = Import-Csv $Csv

# Capas del proyecto segun el paquete (com.businessanalyzer.<capa>...)
$capas = [ordered]@{
    'Analitica (calculos puros)'      = '^com\.businessanalyzer\.analytics'
    'Servicios (reglas de negocio)'   = '^com\.businessanalyzer\.service'
    'Repositorios (SQL)'              = '^com\.businessanalyzer\.repository'
    'Modelo (entidades)'              = '^com\.businessanalyzer\.model'
    'Seguridad, utilidades, config'   = '^com\.businessanalyzer\.(seguridad|util|config|exception|businessanalyzer$)|^com\.businessanalyzer$'
    'Informes (PDF y Excel)'          = '^com\.businessanalyzer\.informes'
    'Interfaz (controladores y ui)'   = '^com\.businessanalyzer\.(controller|ui)'
}

function Porcentaje($cubiertas, $perdidas) {
    $total = $cubiertas + $perdidas
    if ($total -eq 0) { return 0 }
    return [math]::Round(100.0 * $cubiertas / $total, 1)
}

function Sumar($conjunto) {
    $lc = ($conjunto | Measure-Object -Property LINE_COVERED -Sum).Sum
    $lm = ($conjunto | Measure-Object -Property LINE_MISSED -Sum).Sum
    $bc = ($conjunto | Measure-Object -Property BRANCH_COVERED -Sum).Sum
    $bm = ($conjunto | Measure-Object -Property BRANCH_MISSED -Sum).Sum
    [pscustomobject]@{
        Lineas  = Porcentaje $lc $lm
        Ramas   = Porcentaje $bc $bm
        Total   = [int]($lc + $lm)
    }
}

$resultado = foreach ($nombre in $capas.Keys) {
    $s = Sumar ($filas | Where-Object { $_.PACKAGE -match $capas[$nombre] })
    [pscustomobject]@{ Capa = $nombre; Lineas = $s.Lineas; Ramas = $s.Ramas; 'Lineas de codigo' = $s.Total }
}
$logica = Sumar ($filas | Where-Object { $_.PACKAGE -notmatch $capas['Interfaz (controladores y ui)'] })
$todo = Sumar $filas

if ($Markdown) {
    '## Cobertura de pruebas (JaCoCo)'
    ''
    "**Logica de negocio (todo menos la interfaz): $($logica.Lineas) % de lineas** - total del proyecto: $($todo.Lineas) %"
    ''
    '| Capa | Lineas | Ramas | Lineas de codigo |'
    '|---|---:|---:|---:|'
    foreach ($r in $resultado) { "| $($r.Capa) | $($r.Lineas) % | $($r.Ramas) % | $($r.'Lineas de codigo') |" }
    "| **Total** | **$($todo.Lineas) %** | **$($todo.Ramas) %** | **$($todo.Total)** |"
    ''
    'La interfaz (pantallas JavaFX) se verifica con capturas de la aplicacion real, no con pruebas unitarias.'
} else {
    $resultado | Format-Table -AutoSize
    "Logica de negocio (sin interfaz): $($logica.Lineas) % de lineas"
    "Total del proyecto:               $($todo.Lineas) % de lineas, $($todo.Ramas) % de ramas"
}
