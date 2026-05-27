$ErrorActionPreference = 'SilentlyContinue'
try {
    $tcp = New-Object System.Net.Sockets.TcpClient
    $tcp.Connect('localhost', 8082)
    if ($tcp.Connected) {
        Write-Host "TCP connected to localhost:8082"
        $tcp.Close()
    } else {
        Write-Host "TCP NOT connected"
    }
} catch {
    Write-Host "TCP FAILED: $_"
}

try {
    $tcp2 = New-Object System.Net.Sockets.TcpClient
    $tcp2.Connect('127.0.0.1', 8082)
    if ($tcp2.Connected) {
        Write-Host "TCP connected to 127.0.0.1:8082"
        $tcp2.Close()
    } else {
        Write-Host "TCP NOT connected"
    }
} catch {
    Write-Host "TCP FAILED: $_"
}

$hostname = [System.Net.Dns]::GetHostName()
Write-Host "Hostname: $hostname"
$entry = [System.Net.Dns]::GetHostEntry($hostname)
foreach ($addr in $entry.AddressList) {
    Write-Host "  IP: $($addr.IPAddressToString)"
}