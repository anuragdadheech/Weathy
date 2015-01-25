package com.nlmm01.weathy.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * Created by nlmm01 on 25/1/15.
 */
public class WeathySyncService extends Service {
    private static final Object sSyncAdapterLock = new Object();
    private static WeathySyncAdapter sWeathySyncAdapter = null;

    @Override
    public void onCreate() {
        Log.d("WeathySyncService", "onCreate - WeathySyncService");
        synchronized (sSyncAdapterLock) {
            if (sWeathySyncAdapter == null) {
                sWeathySyncAdapter = new WeathySyncAdapter(getApplicationContext(), true);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return sWeathySyncAdapter.getSyncAdapterBinder();
    }
}
