package cn.nua.wenguan.schedule.test;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.TextView;
import cn.nua.wenguan.schedule.ScheduleSyncActivity;
import cn.nua.wenguan.schedule.data.SyncAttemptPolicy;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/** Explicitly invoked device test only. Not present in the delivered application APK. */
public final class SyncFailureInstrumentation extends Instrumentation {
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); start(); }
    private Object field(Object target, String name) throws Exception {
        Field field = ScheduleSyncActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
    private void call(Object target, String name, Class<?>[] types, Object... arguments) throws Exception {
        Method method = ScheduleSyncActivity.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        method.invoke(target, arguments);
    }
    private void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private Throwable uiError;
    private interface Checked { void run() throws Exception; }
    private void ui(Checked action) throws Exception {
        uiError = null;
        runOnMainSync(() -> { try { action.run(); } catch (Throwable error) { uiError = error; } });
        if (uiError != null) throw new Exception("Device UI assertion failed", uiError);
    }
    @Override public void onStart() {
        Bundle result = new Bundle();
        try {
            Map<String, ?> oldSchedule = new HashMap<>(getTargetContext()
                    .getSharedPreferences("official_schedule", Context.MODE_PRIVATE).getAll());
            Intent intent = new Intent(getTargetContext(), ScheduleSyncActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            Activity activity = startActivitySync(intent);
            // Let the normal asynchronous fresh-login clearing complete; do not read cookies.
            long deadline = SystemClock.elapsedRealtime() + 10000L;
            boolean[] ready = {false};
            while (!ready[0] && SystemClock.elapsedRealtime() < deadline) {
                ui(() -> {
                    Object reset = field(activity, "loginReset");
                    ready[0] = !(Boolean) reset.getClass().getMethod("isClearing").invoke(reset);
                });
                if (!ready[0]) SystemClock.sleep(100);
            }
            require(ready[0], "Fresh login clearing completed");
            for (SyncAttemptPolicy.Route first : new SyncAttemptPolicy.Route[]{
                    SyncAttemptPolicy.Route.DIRECT, SyncAttemptPolicy.Route.VPN}) {
                ui(() -> {
                    call(activity, "prepareRetry", new Class<?>[0]);
                    SyncAttemptPolicy attempts = (SyncAttemptPolicy) field(activity, "attempts");
                    attempts.observe(first);
                    call(activity, "failAttempt", new Class<?>[]{String.class, String.class}, "device fixture failure 1", null);
                    // Duplicate errors from this failed window must not consume attempt two.
                    call(activity, "failAttempt", new Class<?>[]{String.class, String.class}, "duplicate fixture failure", null);
                    require(attempts.failures() == 1, "Old callbacks counted once");
                });
                waitForIdleSync();
                ui(() -> {
                    SyncAttemptPolicy attempts = (SyncAttemptPolicy) field(activity, "attempts");
                    require(attempts.isRunning() && attempts.failures() == 1, "Exactly one fallback launched");
                    require(attempts.expected() != first, "Fallback has opposite route");
                    call(activity, "failAttempt", new Class<?>[]{String.class, String.class}, "device fixture failure 2", null);
                    AlertDialog dialog = (AlertDialog) field(activity, "failureDialog");
                    require(dialog != null && dialog.isShowing(), "Failure dialog is visible");
                    TextView text = dialog.findViewById(android.R.id.message);
                    require(text != null && "获取失败，请稍后重试".contentEquals(text.getText()), "Exact failure message");
                    call(activity, "failAttempt", new Class<?>[]{String.class, String.class}, "late fixture failure", null);
                    require(attempts.failures() == 2 && field(activity, "failureDialog") == dialog, "No third attempt or repeated dialog");
                    require(oldSchedule.equals(getTargetContext().getSharedPreferences("official_schedule", Context.MODE_PRIVATE).getAll()),
                            "Existing schedule and start date unchanged");
                });
            }
            Bitmap screen = getUiAutomation().takeScreenshot();
            File evidence = new File(getContext().getExternalFilesDir(null), "two-failures-1.1.17.png");
            try (FileOutputStream out = new FileOutputStream(evidence)) { screen.compress(Bitmap.CompressFormat.PNG, 100, out); }
            screen.recycle();
            ui(() -> {
                call(activity, "prepareRetry", new Class<?>[0]);
                SyncAttemptPolicy attempts = (SyncAttemptPolicy) field(activity, "attempts");
                attempts.succeed();
                call(activity, "failAttempt", new Class<?>[]{String.class, String.class}, "late-after-success", null);
                require(attempts.failures() == 0 && field(activity, "failureDialog") == null, "Success never tries fallback");
                activity.finish();
            });
            result.putString("result", "PASS: both route orders, one fallback, duplicate/stale errors, exact failure dialog, success stops and saved schedule unchanged");
            result.putString("evidence", evidence.getAbsolutePath());
            finish(Activity.RESULT_OK, result);
        } catch (Throwable error) {
            // Only the exception class; never leak page/cookie/password data through test output.
            result.putString("result", "FAIL: " + error.getClass().getSimpleName());
            finish(Activity.RESULT_CANCELED, result);
        }
    }
}
