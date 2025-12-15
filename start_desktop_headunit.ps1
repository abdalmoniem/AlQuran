param([string] $device)

$SDKPath = $env:ANDROID_SDK_ROOT

if (-not $SDKPath)
{
    Write-Error "ERROR: SDKPath parameter is missing, please make sure ANDROID_HOME environment variable is set"
    exit 1
}

Write-Output "SDKPath: $SDKPath"

$HeadUnitExe = "$SDKPath\extras\google\auto\desktop-head-unit.exe"
$ConfigIni = "$SDKPath\extras\google\auto\config\default_720p.ini"

if ($device) {
    adb -s $device forward tcp:5277 tcp:5277
} else {
    adb forward tcp:5277 tcp:5277
}

Write-Output "Running $HeadUnitExe -c $ConfigIni..."

$scriptName = [System.IO.Path]::GetFileNameWithoutExtension($MyInvocation.MyCommand.Name)
$date = Get-Date -Format "ddd_dd_MMM_yyyy_hh_mm_ss_tt"
$date = $date.ToLower()
$stdoutLog = "${scriptName}_${date}_stdout.log"
$stderrLog = "${scriptName}_${date}_stderr.log"

# Run the desktop-head-unit.exe asynchronously and hidden
Start-Process -FilePath $HeadUnitExe `
    -ArgumentList "-c $ConfigIni" `
    -Wait:$false `
    -NoNewWindow `
    -RedirectStandardOutput "$env:TEMP\$stdoutLog" `
    -RedirectStandardError "$env:TEMP\$stderrLog"
