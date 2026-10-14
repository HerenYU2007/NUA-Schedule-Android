$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$env:GRADLE_USER_HOME = "E:\GradleCache"
$env:ANDROID_USER_HOME = Join-Path $ProjectRoot ".android-user-home"
$env:TEMP = "E:\AndroidBuildTemp"
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force -Path $env:TEMP | Out-Null

if (-not $env:ANDROID_SDK_ROOT) {
    $env:ANDROID_SDK_ROOT = "E:\AndroidSDK"
}
if (-not $env:ANDROID_HOME) {
    $env:ANDROID_HOME = $env:ANDROID_SDK_ROOT
}

if (-not (Test-Path -LiteralPath $env:ANDROID_SDK_ROOT)) {
    throw "Android SDK not found at $env:ANDROID_SDK_ROOT. Set ANDROID_SDK_ROOT to an E: path."
}

$LocalGradle = "E:\GradleCache\gradle-8.7\bin\gradle.bat"
if (Test-Path -LiteralPath $LocalGradle) {
    & $LocalGradle :app:assembleDebug @args
} else {
    & (Join-Path $ProjectRoot "gradlew.bat") :app:assembleDebug @args
}
exit $LASTEXITCODE
