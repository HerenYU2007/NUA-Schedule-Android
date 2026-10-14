$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$ResourceRoot = Join-Path $ProjectRoot "app\src\main\res"

$XmlFiles = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot "app\src\main") -Recurse -Filter *.xml
foreach ($File in $XmlFiles) {
    try {
        [xml](Get-Content -Raw -LiteralPath $File.FullName) | Out-Null
    } catch {
        throw "Invalid XML: $($File.FullName): $($_.Exception.Message)"
    }
}

$RequiredFiles = @(
    "settings.gradle.kts",
    "build.gradle.kts",
    "gradle\wrapper\gradle-wrapper.properties",
    "app\build.gradle.kts",
    "app\src\main\AndroidManifest.xml",
    "app\src\main\res\layout\activity_main.xml",
    "app\src\main\res\layout\activity_schedule_sync.xml",
    "app\src\main\assets\schedule-navigation.js",
    "app\src\main\assets\schedule-extract.js",
    "app\src\main\java\cn\nua\wenguan\schedule\data\OfficialScheduleParser.java",
    "app\src\main\java\cn\nua\wenguan\schedule\data\ExportedXlsReader.java",
    "app\src\main\java\cn\nua\wenguan\schedule\data\AcademicExportDownload.java",
    "app\libs\jxl-2.6.12.jar",
    "third_party\jxl-2.6.12-sources.jar",
    "third_party\LGPL-2.1.txt",
    "app\src\main\assets\LGPL-2.1.txt",
    "app\src\main\res\layout\widget_today.xml",
    "app\src\main\res\xml\today_schedule_widget_info.xml",
    "app\src\main\java\cn\nua\wenguan\schedule\MainActivity.java",
    "app\src\main\java\cn\nua\wenguan\schedule\ScheduleSyncActivity.java",
    "app\src\main\java\cn\nua\wenguan\schedule\widget\TodayScheduleWidgetProvider.java"
)
foreach ($Relative in $RequiredFiles) {
    $Full = Join-Path $ProjectRoot $Relative
    if (-not (Test-Path -LiteralPath $Full)) { throw "Missing required file: $Relative" }
}

$AllTextFiles = Get-ChildItem -LiteralPath $ProjectRoot -Recurse -File |
    Where-Object { $_.Extension -in @(".java", ".xml", ".kts", ".properties", ".md", ".ps1", ".bat") }
foreach ($File in $AllTextFiles) {
    [string]$Text = Get-Content -Raw -LiteralPath $File.FullName
    if ($null -eq $Text) { $Text = "" }
    $ForbiddenDriveBackslash = ([char]67) + ":\\"
    $ForbiddenDriveSlash = ([char]67) + ":/"
    if ($Text.Contains($ForbiddenDriveBackslash) -or $Text.Contains($ForbiddenDriveSlash)) {
        throw "Non-E drive path found in $($File.FullName)"
    }
}

$WidgetJava = Get-Content -Raw -LiteralPath (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\widget\TodayScheduleWidgetProvider.java")
$WidgetLayout = Get-Content -Raw -LiteralPath (Join-Path $ResourceRoot "layout\widget_today.xml")
$WidgetLayout += Get-Content -Raw -LiteralPath (Join-Path $ResourceRoot "layout\widget_phone.xml")
$Ids = [regex]::Matches($WidgetJava, "R\.id\.([A-Za-z0-9_]+)") | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique
foreach ($Id in $Ids) {
    if ($WidgetLayout -notmatch "@\+id/$([regex]::Escape($Id))") {
        throw "Widget Java references missing layout ID: $Id"
    }
}

$Manifest = Get-Content -Raw -LiteralPath (Join-Path $ProjectRoot "app\src\main\AndroidManifest.xml")
if ($Manifest -notmatch "TodayScheduleWidgetProvider") { throw "Widget provider is not registered." }
if ($Manifest -notmatch "android\.appwidget\.provider") { throw "Widget metadata is not registered." }

Write-Output "Project validation passed: XML, required files, widget IDs, manifest, and E-drive path policy are valid."
