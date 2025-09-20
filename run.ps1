$ErrorActionPreference = 'Stop'

if (Test-Path .env) {
  Get-Content .env | ForEach-Object {
    if($_ -match '^(?!#)([^=]+)=(.*)$') { Set-Item -Path Env:$($matches[1]) -Value $matches[2] }
  }
}

function Invoke-LocalMode {
  Write-Host "==> Running in LOCAL mode" -ForegroundColor Cyan
  $GLOBALS:procs = @()
  function Start-ServiceProject($name){
    Write-Host "Starting $name" -ForegroundColor Yellow
    $p = Start-Process mvn -WorkingDirectory $name -ArgumentList '-q','-DskipTests','spring-boot:run' -PassThru
    $GLOBALS:procs += $p
  }
  Start-ServiceProject 'discovery-service'
  Start-Sleep -Seconds 5
  foreach($svc in 'inventory-service','user-service','cart-service') { Start-ServiceProject $svc; Start-Sleep -Seconds 2 }
  Write-Host "Building frontend" -ForegroundColor Yellow
  Push-Location frontend; npm run build | Out-Null; Pop-Location
  New-Item -ItemType Directory -Force -Path 'api-gateway/src/main/resources/static' | Out-Null
  Remove-Item 'api-gateway/src/main/resources/static/*' -Force -ErrorAction SilentlyContinue
  Copy-Item frontend/dist/* api-gateway/src/main/resources/static -Recurse -Force
  Start-ServiceProject 'api-gateway'
  Write-Host "All services started (local). Press Ctrl+C to stop." -ForegroundColor Green
  Wait-Process -Id ($procs | Select-Object -ExpandProperty Id)
}

function Invoke-DockerMode {
  Write-Host "==> Running in DOCKER mode" -ForegroundColor Cyan
  if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    Write-Host "Docker not found. Falling back to local mode." -ForegroundColor Yellow
    Invoke-LocalMode; return
  }
  $composeCmd = 'docker compose'
  try { docker compose version | Out-Null } catch {
    if (Get-Command docker-compose -ErrorAction SilentlyContinue) { $composeCmd = 'docker-compose' } else { Write-Host "No docker compose available, fallback local." -ForegroundColor Yellow; Invoke-LocalMode; return }
  }
  $argsList = 'up','-d'
  if ($args.Length -ge 2 -and $args[1] -eq '--build') { $argsList += '--build' }
  if ((& $composeCmd @argsList) -ne $null) { }
  Write-Host "Containers starting. View logs with: $composeCmd logs -f" -ForegroundColor Green
  Write-Host "Gateway: http://localhost:$($Env:GATEWAY_PORT ?? 8080)" -ForegroundColor Green
  Write-Host "Press Ctrl+C to stop log tail (containers continue)." -ForegroundColor Yellow
  & $composeCmd logs -f api-gateway
}

if ($args.Length -ge 1 -and $args[0] -eq '--docker' -or ($Env:DOCKER_MODE -eq 'true')) {
  Invoke-DockerMode @args
} else {
  Invoke-LocalMode
}

