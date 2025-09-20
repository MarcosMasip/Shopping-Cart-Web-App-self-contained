Write-Host "==> Shopping Cart Self-Contained Setup" -ForegroundColor Cyan
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { Write-Error "Java not found (need JDK 17+)"; exit 1 }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { Write-Error "Maven not found"; exit 1 }
if (-not (Get-Command node -ErrorAction SilentlyContinue)) { Write-Error "Node not found"; exit 1 }
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) { Write-Error "npm not found"; exit 1 }

if (-not (Test-Path .env)) { Copy-Item .env.example .env }

"Pre-building backend dependencies"
foreach ($svc in 'discovery-service','api-gateway','inventory-service','cart-service','user-service') {
  Push-Location $svc; mvn -q -DskipTests dependency:go-offline; Pop-Location
}

"Installing frontend dependencies"
Push-Location frontend; npm install --no-audit --no-fund; Pop-Location

Write-Host "Setup complete. Run ./run.ps1" -ForegroundColor Green
