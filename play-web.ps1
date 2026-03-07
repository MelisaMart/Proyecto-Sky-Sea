# One-command launcher: frees port 8080, starts backend in background, waits for readiness,
# resets game state, and opens the browser on the game homepage.
$ErrorActionPreference = "Stop"

function Stop-Port8080Listeners {
    $listeners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty OwningProcess -Unique

    if (-not $listeners) {
        Write-Host "No hay procesos escuchando en 8080."
        return
    }

    Write-Host "Liberando puerto 8080 (PID(s): $($listeners -join ', '))..."
    foreach ($procId in $listeners) {
        try {
            Stop-Process -Id $procId -Force -ErrorAction Stop
            Write-Host "Detenido PID $procId"
        } catch {
            Write-Host "No se pudo detener PID ${procId}: $($_.Exception.Message)"
        }
    }
}

function Start-BackendBackground {
    Write-Host "Iniciando backend en segundo plano..."
    $p = Start-Process -FilePath "powershell" `
        -WorkingDirectory (Get-Location) `
        -ArgumentList "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", ".\\mvnw.cmd spring-boot:run" `
        -PassThru

    Write-Host "Proceso backend lanzado. PID: $($p.Id)"
    return $p
}

function Wait-BackendReady($backendProcess) {
    $maxAttempts = 60
    $url = "http://localhost:8080/api/game/resetPartida"

    Write-Host "Esperando que el backend responda..."
    for ($i = 1; $i -le $maxAttempts; $i++) {
        if ($backendProcess.HasExited) {
            Write-Host "El proceso backend terminó antes de estar listo. ExitCode=$($backendProcess.ExitCode)"
            return $false
        }
        try {
            $null = Invoke-RestMethod -Method Post $url -TimeoutSec 2
            Write-Host "Backend listo (intento $i)."
            return $true
        } catch {
            Start-Sleep -Milliseconds 1000
        }
    }

    return $false
}

Stop-Port8080Listeners
$backend = Start-BackendBackground

if (-not (Wait-BackendReady $backend)) {
    throw "El backend no respondió en el tiempo esperado. Revisá logs con .\mvnw.cmd spring-boot:run"
}

$gameUrl = "http://localhost:8080/frontend/Index.html"
Write-Host "Abriendo juego en navegador: $gameUrl"
Start-Process $gameUrl

Write-Host "Listo. Ya podés jugar desde la web."