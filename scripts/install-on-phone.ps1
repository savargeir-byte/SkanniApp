# Install SkanniApp on connected Android device
param(
    [string]$ApkPath
)

$ErrorActionPreference = 'Stop'

# Find ADB
$adbPath = "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe"
if (-not (Test-Path $adbPath)) {
    Write-Error "ADB not found at $adbPath. Please install Android SDK or set correct path."
    exit 1
}

# Find the most recent APK if not specified
if (-not $ApkPath) {
    $buildDirs = Get-ChildItem -Path ".\app\build_android*" | Sort-Object Name -Descending
    if ($buildDirs.Count -eq 0) {
        Write-Error "No build directories found. Please build the app first."
        exit 1
    }
    
    $latestBuildDir = $buildDirs[0]
    $ApkPath = Join-Path $latestBuildDir.FullName "outputs\apk\debug\app-debug.apk"
    
    if (-not (Test-Path $ApkPath)) {
        Write-Error "APK not found at $ApkPath"
        exit 1
    }
}

Write-Host "=== Installing SkanniApp on Android Device ===" -ForegroundColor Cyan
Write-Host "APK: $ApkPath" -ForegroundColor Yellow

# Check for connected devices
Write-Host "`nChecking for connected Android devices..." -ForegroundColor Green
& $adbPath devices

$devices = & $adbPath devices | Select-String "device$"
if ($devices.Count -eq 0) {
    Write-Host "`nNo Android devices found. Please:" -ForegroundColor Red
    Write-Host "1. Connect your phone via USB" -ForegroundColor Yellow
    Write-Host "2. Enable Developer Options on your phone" -ForegroundColor Yellow
    Write-Host "3. Enable USB Debugging" -ForegroundColor Yellow
    Write-Host "4. Accept the debugging authorization prompt" -ForegroundColor Yellow
    exit 1
}

Write-Host "`nFound $($devices.Count) connected device(s)" -ForegroundColor Green

# Install the APK
Write-Host "`nInstalling APK..." -ForegroundColor Green
& $adbPath install -r $ApkPath

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✅ SkanniApp installed successfully!" -ForegroundColor Green
    Write-Host "You can now find 'SkanniApp' in your phone's app drawer." -ForegroundColor Yellow
} else {
    Write-Host "`n❌ Installation failed. Check the error messages above." -ForegroundColor Red
    exit 1
}