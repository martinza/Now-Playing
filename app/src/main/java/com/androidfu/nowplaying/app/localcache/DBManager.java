package com.androidfu.nowplaying.app.localcache;

import android.content.Context;

import com.j256.ormlite.android.apptools.OpenHelperManager;

import hugo.weaving.DebugLog;

@DebugLog
public class DBManager {

    private static DBHelper dbHelper = null;

    public static DBHelper getHelper(Context context) {
        if (dbHelper == null) {
            dbHelper = OpenHelperManager.getHelper(context, DBHelper.class);
        }
        return dbHelper;
    }

    public static void releaseHelper() {
        if (dbHelper != null) {
            OpenHelperManager.releaseHelper();
            dbHelper = null;
        }
    }
}
