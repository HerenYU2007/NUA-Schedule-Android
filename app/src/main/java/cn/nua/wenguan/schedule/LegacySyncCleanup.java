package cn.nua.wenguan.schedule;

import android.app.job.JobScheduler;
import android.content.Context;
import android.content.Intent;

/** Upgrade cleanup only: never registers jobs, initializes WebView, or opens a login screen. */
public final class LegacySyncCleanup {
    private LegacySyncCleanup() { }
    public static void cancel(Context c) {
        try {
            JobScheduler jobs = c.getSystemService(JobScheduler.class);
            if (jobs != null) jobs.cancel(8417);
            c.stopService(new Intent().setClassName(c.getPackageName(), c.getPackageName() + ".BackgroundSyncJobService"));
        } catch (RuntimeException ignored) { }
        c.getSharedPreferences("sync_preferences", Context.MODE_PRIVATE).edit()
                .remove("keep_login").remove("background").remove("cookie_saved").remove("cookie_checked_at").apply();
    }
}
