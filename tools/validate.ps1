param([string]$ExportedXls)
$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$Output = Join-Path $ProjectRoot "tools\out"
New-Item -ItemType Directory -Force -Path $Output | Out-Null

$Sources = @(
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
