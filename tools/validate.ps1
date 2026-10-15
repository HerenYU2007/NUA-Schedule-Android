param([string]$ExportedXls, [string]$JsonTestJar = "E:\AndroidBuildTemp\test-deps\json-20260814.jar")
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Output = Join-Path $ProjectRoot "tools\out"
New-Item -ItemType Directory -Force -Path $Output | Out-Null

$Sources = @(
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ParallelUpdateCheck.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\UpdateCheckConnections.java"),
    (Join-Path $ProjectRoot "tools\ValidateParallelUpdates.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ui\WeekPrefetchPolicy.java"),
    (Join-Path $ProjectRoot "tools\ValidateFastPaging.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ui\GridOccupancyPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ui\AgendaGridPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\AgendaConflictIndex.java"),
    (Join-Path $ProjectRoot "tools\ValidateConflictGrid.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ui\OutsideTapPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ui\WeekPagerPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\widget\WidgetHostPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\widget\LatestTaskQueue.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\UpdateCheckPolicy.java"),
    (Join-Path $ProjectRoot "tools\ValidateUpgradeUi.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\widget\WidgetRecoveryPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\widget\DailyWidgetSelectionPolicy.java"),
    (Join-Path $ProjectRoot "tools\ValidateWidgetRecovery.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ui\GridPlacementPolicy.java"),
    (Join-Path $ProjectRoot "tools\ValidateGridInteractions.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\SchoolPeriodTimes.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\SlotEditPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\widget\WeekBitmapPolicy.java"),
    (Join-Path $ProjectRoot "tools\ValidateLocal123.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ReminderPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\ScheduleConflicts.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\ScheduleExport.java"),
    (Join-Path $ProjectRoot "tools\ValidateFeatureBatch.java"),
    (Join-Path $ProjectRoot "tools\ValidateBackupRoundTrip.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\UpdateRouteRanking.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\UpdateMirrorPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\UpdateDownload.java"),
    (Join-Path $ProjectRoot "tools\ValidateUpdateMirrors.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ui\WeekSwipePolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\model\AgendaEvent.java"),
    (Join-Path $ProjectRoot "tools\ValidateUiPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\ui\PeriodGrid.java"),
    (Join-Path $ProjectRoot "tools\ValidatePeriodGrid.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\widget\WidgetSizePolicy.java"),
    (Join-Path $ProjectRoot "tools\ValidateWidgetSize.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\model\CourseSession.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\model\CourseStatus.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\model\ScheduleRule.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\ScheduleRepository.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\LoginSessionReset.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\OfficialScheduleParser.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\SchoolSyncPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\SyncAttemptPolicy.java"),
    (Join-Path $ProjectRoot "tools\ValidateSyncAttemptPolicy.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\ExportedXlsReader.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\data\AcademicExportDownload.java"),
    (Join-Path $ProjectRoot "tools\ValidateExportedXls.java"),
    (Join-Path $ProjectRoot "tools\ValidateAcademicExportDownload.java"),
    (Join-Path $ProjectRoot "tools\ValidateSchedule.java"),
    (Join-Path $ProjectRoot "app\src\main\java\cn\nua\wenguan\schedule\widget\WidgetLayoutPolicy.java"),
    (Join-Path $ProjectRoot "tools\ValidateWidgetLayout.java"),
    (Join-Path $ProjectRoot "tools\ValidateLoginSessionReset.java"),
    (Join-Path $ProjectRoot "tools\ValidateOfficialParser.java"),
    (Join-Path $ProjectRoot "tools\ValidateSchoolSyncPolicy.java")
)

$AndroidJar = "E:\AndroidSDK\platforms\android-35\android.jar"
$JxlJar = Join-Path $ProjectRoot "app\libs\jxl-2.6.12.jar"
& javac -encoding UTF-8 -cp "$AndroidJar;$JxlJar" -d $Output $Sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java "-Djava.io.tmpdir=E:\AndroidBuildTemp" -cp $Output ValidateUpdateMirrors $Output
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateSchedule
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateUiPolicy
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateParallelUpdates
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateFastPaging
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateConflictGrid
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateUpgradeUi
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateLocal123
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateGridInteractions
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateWidgetRecovery
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$Output;$AndroidJar;$JxlJar" ValidateFeatureBatch
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if (Test-Path -LiteralPath $JsonTestJar) {
    & java -cp "$JsonTestJar;$Output;$AndroidJar;$JxlJar" ValidateBackupRoundTrip
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else { Write-Output "JSON JVM round-trip test skipped: pass -JsonTestJar with a standalone org.json runtime (test-only, not an app dependency)." }
& java -cp $Output ValidatePeriodGrid
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateWidgetSize
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateWidgetLayout
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateLoginSessionReset
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$Output;$AndroidJar" ValidateOfficialParser
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateSchoolSyncPolicy
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp $Output ValidateSyncAttemptPolicy
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
if ($ExportedXls) { & java -cp "$Output;$AndroidJar;$JxlJar" ValidateExportedXls $ExportedXls }
else { & java -cp "$Output;$AndroidJar;$JxlJar" ValidateExportedXls }
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$Output;$AndroidJar;$JxlJar" ValidateAcademicExportDownload
exit $LASTEXITCODE
