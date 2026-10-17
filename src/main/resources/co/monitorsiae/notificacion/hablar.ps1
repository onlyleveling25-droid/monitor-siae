# Lee en voz alta el texto de la variable de entorno MONITOR_SIAE_VOZ.
# Usa las voces modernas de Windows (las que se instalan desde Configuracion > Voz), prefiriendo
# espanol. Si esas voces no estan disponibles, recurre al sintetizador clasico de Windows.
#
# Con -SoloVerificar no habla: imprime el nombre de la voz en espanol disponible, o NINGUNA.
#
# Este archivo se mantiene en ASCII: Windows PowerShell 5 lee los scripts sin BOM como ANSI.

param([switch] $SoloVerificar)

$ErrorActionPreference = 'Stop'

function Get-VozEspanol {
    $null = [Windows.Media.SpeechSynthesis.SpeechSynthesizer, Windows.Media.SpeechSynthesis, ContentType = WindowsRuntime]
    [Windows.Media.SpeechSynthesis.SpeechSynthesizer]::AllVoices |
        Where-Object { $_.Language -like 'es*' } |
        Select-Object -First 1
}

function Wait-Operacion($operacion, [Type] $tipoResultado) {
    $asTask = [System.WindowsRuntimeSystemExtensions].GetMethods() |
        Where-Object {
            $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and
            $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1'
        } |
        Select-Object -First 1
    $tarea = $asTask.MakeGenericMethod($tipoResultado).Invoke($null, @($operacion))
    $tarea.Wait() | Out-Null
    $tarea.Result
}

function Invoke-VozModerna([string] $texto) {
    Add-Type -AssemblyName System.Runtime.WindowsRuntime
    $sintetizador = New-Object Windows.Media.SpeechSynthesis.SpeechSynthesizer
    $voz = Get-VozEspanol
    if ($voz) {
        $sintetizador.Voice = $voz
    }
    $flujo = Wait-Operacion ($sintetizador.SynthesizeTextToStreamAsync($texto)) `
        ([Windows.Media.SpeechSynthesis.SpeechSynthesisStream])
    $audio = [System.IO.WindowsRuntimeStreamExtensions]::AsStreamForRead($flujo.GetInputStreamAt(0))
    $reproductor = New-Object System.Media.SoundPlayer $audio
    $reproductor.PlaySync()
}

function Invoke-VozClasica([string] $texto) {
    Add-Type -AssemblyName System.Speech
    $sintetizador = New-Object System.Speech.Synthesis.SpeechSynthesizer
    $sintetizador.Volume = 100
    $sintetizador.Speak($texto)
}

if ($SoloVerificar) {
    try {
        $voz = Get-VozEspanol
    } catch {
        $voz = $null
    }
    if ($voz) {
        Write-Output "$($voz.DisplayName) ($($voz.Language))"
    } else {
        Write-Output 'NINGUNA'
    }
    exit 0
}

$texto = $env:MONITOR_SIAE_VOZ
if ([string]::IsNullOrWhiteSpace($texto)) {
    exit 0
}

try {
    Invoke-VozModerna $texto
} catch {
    Invoke-VozClasica $texto
}
