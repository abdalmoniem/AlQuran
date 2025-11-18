$SDKPath = $env:ANDROID_HOME

if (-not $SDKPath) {
    Write-Error "ERROR: SDKPath parameter is missing, please make sure ANDROID_HOME environment variable is set"
    exit 1
}

Write-Output "SDKPath: $SDKPath"

$HeadUnitExe = "$SDKPath\extras\google\auto\desktop-head-unit.exe"
$ConfigIni = "$SDKPath\extras\google\auto\config\default_720p.ini"

adb forward tcp:5277 tcp:5277

Write-Output "Running $HeadUnitExe -c $ConfigIni..."

# Run the desktop-head-unit.exe asynchronously and hidden
Start-Process -FilePath $HeadUnitExe `
    -ArgumentList "-c $ConfigIni" `
    -Wait:$false `
    -NoNewWindow
