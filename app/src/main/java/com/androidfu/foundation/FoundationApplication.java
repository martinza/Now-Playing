package com.androidfu.foundation;

import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.StrictMode;

import com.androidfu.foundation.api.GetApplicationSettingsRequest;
import com.androidfu.foundation.api.GetApplicationSettingsRequestHanlder;
import com.androidfu.foundation.util.EventBus;
import com.androidfu.foundation.util.Log;
import com.androidfu.foundation.util.SharedPreferencesHelper;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Logger;
import com.google.android.gms.analytics.Tracker;

import java.util.HashMap;

import hugo.weaving.DebugLog;

/**
 * Created by Bill on 8/1/14.
 */
public class FoundationApplication extends Application {

    private static final String TAG = FoundationApplication.class.getSimpleName();

    public static int APP_VERSION_CODE;
    public static String APP_VERSION_NAME;

    @DebugLog
    @Override
    public void onCreate() {
        super.onCreate();

        if (!BuildConfig.DEBUG) {
            // Crashlytics.start(this);
        }

        try {
            Log.setLogging(BuildConfig.DEBUG && Boolean.valueOf(getString(R.string.logging)));
            Log.setLogLevel(Integer.valueOf(getString(R.string.logging_level)));
        } catch (Exception e) {
            Log.e(TAG, "Something went wrong setting logging and/or logging level.  App will set what it can and use defaults otherwise.", e);
        }

        new StrictModeHelper().setupStrictMode();

        APP_VERSION_CODE = getApplicationVersionCode();
        APP_VERSION_NAME = getApplicationVersionName();

        GoogleAnalytics.getInstance(this).enableAutoActivityReports(this);
        GoogleAnalytics.getInstance(this).getLogger().setLogLevel(BuildConfig.DEBUG ? Logger.LogLevel.VERBOSE : Logger.LogLevel.ERROR);
        GoogleAnalytics.getInstance(this).setDryRun(BuildConfig.DEBUG);
        SharedPreferencesHelper.initialize(this);

        registerHandlersWithEventBus();
        EventBus.post(new GetApplicationSettingsRequest(getString(R.string.application_settings_url)));
    }

    /**
     * Register all the Handlers with the EventBus singleton.
     */
    @DebugLog
    private void registerHandlersWithEventBus() {
        // Register all our Handlers on the EventBus.
        EventBus.register(new GetApplicationSettingsRequestHanlder());
    }

    /**
     * Get the application version code as stored in the manifest or build.gradle file.
     *
     * @return the application version code as an int.
     */
    @DebugLog
    private int getApplicationVersionCode() {
        try {
            return this.getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Get the application version name and append a "d" if we're using a developer build.
     *
     * @return the application version name as a String.
     */
    @DebugLog
    private String getApplicationVersionName() {
        String devBuild = BuildConfig.DEBUG ? "d" : "";
        try {
            return this.getPackageManager().getPackageInfo(getPackageName(), 0).versionName + devBuild;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public class StrictModeHelper {

        public final String TAG = StrictModeHelper.class.getSimpleName();

        @DebugLog
        public void setupStrictMode() {
            if (shouldEnableStrictMode()) {
                Log.wtf(TAG, "*** Strict Mode Enforced ***");
                enableStrictMode();
            } else {
                Log.wtf(TAG, "*** Strict Mode NOT Enforced ***");
            }
        }

        @DebugLog
        private boolean shouldEnableStrictMode() {
            return BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
        }

        @DebugLog
        private void enableStrictMode() {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());

            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                            //.penaltyDeath()  // No need to go to this extreme all the time
                    .build());
        }
    }

    @DebugLog
    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {

            GoogleAnalytics analytics = GoogleAnalytics.getInstance(this);
            Tracker tracker = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(R.xml.application_tracker)
                    : (trackerId == TrackerName.GLOBAL_TRACKER) ? analytics.newTracker(R.xml.global_tracker)
                    : analytics.newTracker(R.xml.ecommerce_tracker);
            mTrackers.put(trackerId, tracker);
        }
        return mTrackers.get(trackerId);
    }

    /**
     * Enum used to identify the tracker that needs to be used for tracking.
     *
     * A single tracker is usually enough for most purposes. In case you do need multiple trackers,
     * storing them all in Application object helps ensure that they are created only once per
     * application instance.
     */
    public enum TrackerName {
        APP_TRACKER, // Tracker used only in this app.
        GLOBAL_TRACKER, // Tracker used by all the apps from a company. eg: roll-up tracking.
        ECOMMERCE_TRACKER, // Tracker used by all ecommerce transactions from a company.
    }

    HashMap<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
}
