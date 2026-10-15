param([string]$JsonTestJar = "E:\AndroidBuildTemp\test-deps\json-20260814.jar")
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Output = Join-Path $ProjectRoot "tools\out"
$Classes = Join-Path $ProjectRoot "app\build\intermediates\javac\debug\compileDebugJavaWithJavac\classes"
$AndroidJar = "E:\AndroidSDK\platforms\android-35\android.jar"
if (!(Test-Path -LiteralPath (Join-Path $Classes "cn\nua\wenguan\schedule\AppUpdates.class"))) { throw "Build debug APK first" }
if (!(Test-Path -LiteralPath $JsonTestJar)) { throw "Standalone JSON test runtime required" }
New-Item -ItemType Directory -Path $Output -Force | Out-Null
& javac -encoding UTF-8 -cp "$Classes;$JsonTestJar;$AndroidJar" -d $Output (Join-Path $ProjectRoot "tools\ValidateSignedUpdateManifest.java")
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$Output;$Classes;$JsonTestJar;$AndroidJar" cn.nua.wenguan.schedule.ValidateSignedUpdateManifest
exit $LASTEXITCODE
