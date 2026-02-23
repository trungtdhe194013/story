# Start ngrok tunnel for local development
# Make sure Spring Boot is running on port 8080 first

Write-Host "Starting ngrok tunnel on port 8080..." -ForegroundColor Green
Write-Host "Make sure your Spring Boot application is running!" -ForegroundColor Yellow
Write-Host ""

# Path to ngrok executable
$ngrokPath = "C:\Users\laptop368\Downloads\ngrok-v3-stable-windows-amd64\ngrok.exe"

# Check if ngrok exists
if (-Not (Test-Path $ngrokPath)) {
    Write-Host "Error: ngrok not found at $ngrokPath" -ForegroundColor Red
    Write-Host "Please update the path in this script or install ngrok" -ForegroundColor Red
    exit 1
}

# Start ngrok
Write-Host "Running: $ngrokPath http 8080" -ForegroundColor Cyan
& $ngrokPath http 8080 --log=stdout
