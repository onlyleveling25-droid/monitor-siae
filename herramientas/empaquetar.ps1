<#
.SYNOPSIS
    Genera el paquete de Monitor SIAE para Windows con jpackage.

.DESCRIPTION
    Compila el proyecto, reune sus librerias y crea:
      - app-image (por defecto): carpeta + .zip portables, sin instalador. No necesita nada mas
        que el JDK 21.
      - msi / exe: instalador. Necesita ademas WiX Toolset 3.x en el PATH (candle.exe y light.exe).

    El paquete incluye su propio Java: el equipo donde se instale NO necesita JDK.
    Debe ejecutarse en Windows: jpackage solo genera paquetes para el sistema donde corre.

.EXAMPLE
    .\herramientas\empaquetar.ps1
    .\herramientas\empaquetar.ps1 -Tipo msi -Version 1.0.1
#>
param(
    [ValidateSet('app-image', 'msi', 'exe')]
    [string]$Tipo = 'app-image',

    # Version que ve Windows (solo numeros separados por puntos, el primero distinto de cero).
    [string]$Version = '1.0.0',

    # Omite las pruebas (solo para repetir un paquete que ya probaste).
    [switch]$SinPruebas
)

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
Set-Location $raiz

$nombre = 'Monitor SIAE'
$claseInicio = 'co.monitorsiae.app.Lanzador'
# Identificador fijo: permite que una version nueva del .msi actualice a la anterior en vez de duplicarla.
$idActualizacion = '15bd0d48-19ba-4d13-b1c9-31f9e8fc8078'

function Fallar([string]$mensaje) {
    Write-Host "ERROR: $mensaje" -ForegroundColor Red
    exit 1
}

function Ejecutar([string]$paso, [scriptblock]$orden) {
    Write-Host "==> $paso" -ForegroundColor Cyan
    & $orden
    if ($LASTEXITCODE -ne 0) {
        Fallar "Fallo el paso: $paso (codigo $LASTEXITCODE)."
    }
}

# --- 1. Comprobaciones previas -----------------------------------------------------------
if ($Version -notmatch '^[1-9]\d*(\.\d+){0,2}$') {
    Fallar "Version invalida '$Version'. Usa por ejemplo 1.0.0 (el primer numero no puede ser 0)."
}
if (-not $env:JAVA_HOME) {
    Fallar 'JAVA_HOME no esta definido. Apunta a la carpeta de un JDK 21 (ver docs/GUIA-INSTALACION.md).'
}
$jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
if (-not (Test-Path $jpackage)) {
    Fallar "No hay jpackage.exe en '$env:JAVA_HOME\bin'. Necesitas un JDK 21 completo, no un JRE."
}
$infoJdk = Join-Path $env:JAVA_HOME 'release'
if (-not (Test-Path $infoJdk) -or -not (Select-String -Path $infoJdk -Pattern 'JAVA_VERSION="21' -Quiet)) {
    Fallar "JAVA_HOME ('$env:JAVA_HOME') no es un JDK 21."
}
if ($Tipo -ne 'app-image') {
    foreach ($herramienta in 'candle.exe', 'light.exe') {
        if (-not (Get-Command $herramienta -ErrorAction SilentlyContinue)) {
            Fallar "Falta WiX Toolset 3.x ($herramienta no esta en el PATH). Instalalo o usa -Tipo app-image."
        }
    }
}

# --- 2. Compilar y reunir las librerias ---------------------------------------------------
$entrada = Join-Path $raiz 'target\instalador\lib'
$salida = Join-Path $raiz 'target\instalador\salida'
if (Test-Path (Join-Path $raiz 'target\instalador')) {
    Remove-Item (Join-Path $raiz 'target\instalador') -Recurse -Force
}

if ($SinPruebas) {
    Ejecutar 'Compilar (sin pruebas)' { & .\mvnw.cmd -q -DskipTests clean package }
} else {
    Ejecutar 'Compilar y ejecutar las pruebas' { & .\mvnw.cmd -q clean verify }
}
Ejecutar 'Reunir las librerias' {
    & .\mvnw.cmd -q org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy-dependencies `
        -DincludeScope=runtime "-DoutputDirectory=$entrada"
}

$jar = Get-ChildItem (Join-Path $raiz 'target') -Filter 'monitor-siae-*.jar' |
    Where-Object { $_.Name -notmatch 'sources|javadoc' } | Select-Object -First 1
if (-not $jar) {
    Fallar 'No se encontro el .jar de la aplicacion en target\.'
}
Copy-Item $jar.FullName $entrada

# --- 3. jpackage -------------------------------------------------------------------------
$argumentos = @(
    '--type', $Tipo,
    '--name', $nombre,
    '--app-version', $Version,
    '--vendor', 'Emprestur',
    '--description', 'Monitor SIAE: vigila el inventario de armas y alerta al usuario',
    '--input', $entrada,
    '--main-jar', $jar.Name,
    '--main-class', $claseInicio,
    # Se incluye el JDK completo: pesa mas que un runtime recortado, pero evita que falte un modulo.
    '--runtime-image', $env:JAVA_HOME,
    '--java-options', '-Dfile.encoding=UTF-8',
    '--dest', $salida
)
$icono = Join-Path $raiz 'herramientas\icono\monitor-siae.ico'
if (Test-Path $icono) {
    $argumentos += @('--icon', $icono)
}
if ($Tipo -ne 'app-image') {
    $argumentos += @(
        '--win-menu', '--win-shortcut', '--win-dir-chooser',
        # Instala solo para el usuario actual: no pide permisos de administrador.
        '--win-per-user-install',
        '--win-upgrade-uuid', $idActualizacion
    )
}

Ejecutar "Crear el paquete ($Tipo)" { & $jpackage @argumentos }

# --- 4. Resultado ------------------------------------------------------------------------
if ($Tipo -eq 'app-image') {
    $zip = Join-Path $salida "Monitor-SIAE-$Version-portable.zip"
    Compress-Archive -Path (Join-Path $salida $nombre) -DestinationPath $zip -Force
    Write-Host ''
    Write-Host "Listo. Carpeta portable: $salida\$nombre  (ejecutable: $nombre.exe)" -ForegroundColor Green
    Write-Host "Comprimido para copiar a otro equipo: $zip" -ForegroundColor Green
} else {
    $instalador = Get-ChildItem $salida -Filter "*.$Tipo" | Select-Object -First 1
    Write-Host ''
    Write-Host "Listo. Instalador: $($instalador.FullName)" -ForegroundColor Green
}
