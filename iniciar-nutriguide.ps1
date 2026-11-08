<#
.SYNOPSIS
    Arranca NutriGuide completo en Windows: PostgreSQL + pgvector (Docker), backend y frontend.

.DESCRIPTION
    1. Lee backend\.env si existe (copiado de backend\.env.example; no se sube a git).
    2. Levanta la base de datos con docker compose y espera a que esté lista.
    3. Compila el backend si no existe el .jar (o si se usa -Compilar).
    4. Arranca el backend en segundo plano (registro en backend\logs\backend.log).
    5. Sirve el frontend en http://localhost:5500 si ese puerto está libre
       (si ya usas Live Server en el 5500, se respeta).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File .\iniciar-nutriguide.ps1
    powershell -ExecutionPolicy Bypass -File .\iniciar-nutriguide.ps1 -Compilar
    powershell -ExecutionPolicy Bypass -File .\iniciar-nutriguide.ps1 -Detener
#>
param(
    [switch]$Compilar,
    [switch]$Detener
)

$ErrorActionPreference = 'Stop'
$raiz = $PSScriptRoot
$backend = Join-Path $raiz 'backend'
$frontend = Join-Path $raiz 'frontend'
$jar = Join-Path $backend 'target\nutriguide-backend-1.0.0.jar'
$logs = Join-Path $backend 'logs'

function Proceso-EnPuerto([int]$puerto) {
    $c = Get-NetTCPConnection -LocalPort $puerto -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($c) { return $c.OwningProcess }
    return $null
}

function Esperar-Url([string]$url, [int]$segundos) {
    for ($i = 0; $i -lt $segundos; $i++) {
        try {
            $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
            if ($r.StatusCode -eq 200) { return $true }
        } catch { }
        Start-Sleep -Seconds 1
    }
    return $false
}

# ------------------------------------------------------------------ detener
if ($Detener) {
    $pid8080 = Proceso-EnPuerto 8080
    if ($pid8080) { Stop-Process -Id $pid8080 -Force; Write-Host "Backend detenido." }
    docker compose -f (Join-Path $backend 'docker-compose.yml') stop | Out-Null
    Write-Host "Base de datos detenida. (El frontend en el 5500, si lo arrancó este script, se cierra con su ventana o desde el Administrador de tareas.)"
    exit 0
}

# ------------------------------------------------------------------ variables de entorno
$archivoEnv = Join-Path $backend '.env'
if (Test-Path $archivoEnv) {
    Get-Content $archivoEnv -Encoding UTF8 | ForEach-Object {
        $linea = $_.Trim()
        if ($linea -and -not $linea.StartsWith('#') -and $linea.Contains('=')) {
            $nombre, $valor = $linea.Split('=', 2)
            $valor = $valor.Trim()
            if ($valor -and -not $valor.StartsWith('TU_') -and -not $valor.StartsWith('CAMBIA_')) {
                Set-Item -Path "Env:$($nombre.Trim())" -Value $valor
            }
        }
    }
    Write-Host "Variables cargadas de backend\.env"
} else {
    Write-Host "No existe backend\.env (cópialo de backend\.env.example para guardar tu GEMINI_API_KEY)."
}

if (-not $env:ADMIN_TOKEN) {
    $env:ADMIN_TOKEN = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    Write-Host "ADMIN_TOKEN generado para esta sesión: $($env:ADMIN_TOKEN)"
}
if (-not $env:GEMINI_API_KEY) {
    Write-Warning "GEMINI_API_KEY no está definida: el chat responderá que la IA no está configurada y los documentos quedarán pendientes de indexar."
}

# ------------------------------------------------------------------ base de datos
try { docker info *> $null } catch { }
if ($LASTEXITCODE -ne 0) {
    throw "Docker no está en ejecución. Abre Docker Desktop y vuelve a ejecutar el script."
}
Write-Host "Levantando PostgreSQL + pgvector..."
docker compose -f (Join-Path $backend 'docker-compose.yml') up -d | Out-Null
for ($i = 0; $i -lt 60; $i++) {
    $estado = docker inspect -f '{{.State.Health.Status}}' nutriguide-pgvector 2>$null
    if ($estado -eq 'healthy') { break }
    Start-Sleep -Seconds 2
}
if ($estado -ne 'healthy') { throw "La base de datos no quedó lista. Revisa: docker logs nutriguide-pgvector" }
Write-Host "Base de datos lista (puerto $(if ($env:DB_PORT) { $env:DB_PORT } else { '5434' }))."

# ------------------------------------------------------------------ backend
if (Proceso-EnPuerto 8080) {
    Write-Host "El puerto 8080 ya está en uso: se asume que el backend ya está corriendo."
} else {
    if ($Compilar -or -not (Test-Path $jar)) {
        Write-Host "Compilando el backend (la primera vez tarda unos minutos)..."
        Push-Location $backend
        try {
            & (Join-Path $backend 'mvnw.cmd') -q -DskipTests package
            if ($LASTEXITCODE -ne 0) { throw "La compilación falló." }
        } finally { Pop-Location }
    }
    New-Item -ItemType Directory -Force -Path $logs | Out-Null
    $java = 'java'
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\java.exe'))) {
        $java = Join-Path $env:JAVA_HOME 'bin\java.exe'
    }
    Write-Host "Arrancando el backend..."
    $opciones = @{
        FilePath               = $java
        ArgumentList           = @('-jar', "`"$jar`"")
        WorkingDirectory       = $backend
        WindowStyle            = 'Hidden'
        RedirectStandardOutput = (Join-Path $logs 'backend.log')
        RedirectStandardError  = (Join-Path $logs 'backend-error.log')
    }
    Start-Process @opciones
    if (-not (Esperar-Url 'http://localhost:8080/api/alimentos' 120)) {
        throw "El backend no respondió. Revisa backend\logs\backend.log"
    }
}
Write-Host "Backend listo en http://localhost:8080"

# ------------------------------------------------------------------ frontend
if (Proceso-EnPuerto 5500) {
    Write-Host "El puerto 5500 ya está en uso (por ejemplo, Live Server): se usa ese frontend."
} else {
    Start-Process -FilePath 'python' -ArgumentList @('-m', 'http.server', '5500') -WorkingDirectory $frontend -WindowStyle Hidden
    Start-Sleep -Seconds 2
}

Write-Host ""
Write-Host "NutriGuide funcionando:"
Write-Host "  Frontend: http://localhost:5500"
Write-Host "  API:      http://localhost:8080/api/alimentos"
Write-Host "  Detener:  powershell -ExecutionPolicy Bypass -File .\iniciar-nutriguide.ps1 -Detener"
