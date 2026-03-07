# Stops the process listening on port 8080 (if any).
$listeners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue |
    Select-Object -ExpandProperty OwningProcess -Unique

if (-not $listeners) {
    Write-Host "No hay procesos escuchando en 8080."
    exit 0
}

foreach ($procId in $listeners) {
    try {
        Stop-Process -Id $procId -Force -ErrorAction Stop
        Write-Host "Proceso detenido: PID $procId"
    } catch {
        Write-Host "No se pudo detener PID ${procId}: $($_.Exception.Message)"
    }
}
