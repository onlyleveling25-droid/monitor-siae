# Ejecuta Maven sin exigir una instalacion global.
# La primera vez descarga la version indicada en maven-wrapper.properties a
# %USERPROFILE%\.m2\wrapper\dists, verifica su SHA-512 y la reutiliza en adelante.

$ErrorActionPreference = 'Stop'

$raizProyecto = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$archivoPropiedades = Join-Path $PSScriptRoot 'maven-wrapper.properties'

$lineaUrl = Get-Content $archivoPropiedades | Where-Object { $_ -match '^distributionUrl=' } | Select-Object -First 1
if (-not $lineaUrl) {
    throw "No se encontro distributionUrl en $archivoPropiedades"
}
$urlDistribucion = $lineaUrl.Substring('distributionUrl='.Length).Trim()

$nombreZip = Split-Path -Leaf $urlDistribucion
$nombreDistribucion = $nombreZip -replace '-bin\.zip$', ''
$dirDistribuciones = Join-Path $env:USERPROFILE '.m2\wrapper\dists'
$mavenHome = Join-Path $dirDistribuciones $nombreDistribucion
$mvnCmd = Join-Path $mavenHome 'bin\mvn.cmd'

if (-not (Test-Path $mvnCmd)) {
    New-Item -ItemType Directory -Force -Path $dirDistribuciones | Out-Null
    $zip = Join-Path $dirDistribuciones $nombreZip
    $archivoSha = "$zip.sha512"

    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    # En Windows PowerShell 5 la barra de progreso vuelve la descarga varias veces mas lenta.
    $ProgressPreference = 'SilentlyContinue'

    Write-Host "Descargando $nombreDistribucion (solo la primera vez)..."
    Invoke-WebRequest -Uri $urlDistribucion -OutFile $zip -UseBasicParsing
    Invoke-WebRequest -Uri "$urlDistribucion.sha512" -OutFile $archivoSha -UseBasicParsing

    $shaEsperado = (Get-Content $archivoSha -Raw).Trim().Split(' ')[0]
    $shaObtenido = (Get-FileHash $zip -Algorithm SHA512).Hash
    if ($shaObtenido -ne $shaEsperado) {
        Remove-Item $zip, $archivoSha -Force
        throw "La descarga de Maven no coincide con su SHA-512 publicado. Se elimino el archivo."
    }

    Expand-Archive -Path $zip -DestinationPath $dirDistribuciones -Force
    Remove-Item $zip, $archivoSha -Force
}

Push-Location $raizProyecto
try {
    & $mvnCmd @args
    $codigoSalida = $LASTEXITCODE
}
finally {
    Pop-Location
}
exit $codigoSalida
