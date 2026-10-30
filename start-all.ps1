$ErrorActionPreference = 'Stop'
mvn -q clean package
$root = $PSScriptRoot
$processes = @()
try {
    $processes += Start-Process java -ArgumentList '-jar', "$root/eureka-server/target/eureka-server-1.0.0-exec.jar" -PassThru -NoNewWindow
    Start-Sleep -Seconds 8
    $processes += Start-Process java -ArgumentList '-jar', "$root/hotel-service/target/hotel-service-1.0.0-exec.jar" -PassThru -NoNewWindow
    $processes += Start-Process java -ArgumentList '-jar', "$root/booking-service/target/booking-service-1.0.0-exec.jar" -PassThru -NoNewWindow
    Start-Sleep -Seconds 8
    $processes += Start-Process java -ArgumentList '-jar', "$root/api-gateway/target/api-gateway-1.0.0-exec.jar" -PassThru -NoNewWindow
    Write-Host 'Gateway: http://localhost:8080 | Eureka: http://localhost:8761'
    Wait-Process -Id $processes.Id
} finally {
    $processes | Where-Object { -not $_.HasExited } | Stop-Process
}
