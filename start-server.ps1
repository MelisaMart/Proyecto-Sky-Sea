# Starts Sky & Sea backend on 8080 after releasing any existing listener.
$ErrorActionPreference = "Stop"

$listeners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique

if ($listeners) {
    Write-Host "Liberando puerto 8080 (PID(s): $($listeners -join ', '))..."
    foreach ($procId in $listeners) {
        try {
            Stop-Process -Id $procId -Force -ErrorAction Stop
        } catch {
            Write-Host "No se pudo detener PID ${procId}: $($_.Exception.Message)"
        }
    }
    Start-Sleep -Milliseconds 400
}

Write-Host "Iniciando servidor Spring Boot..."
& .\mvnw.cmd spring-boot:run
