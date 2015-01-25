package com.nlmm01.weathy.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by nlmm01 on 25/1/15.
 */
public class WeathyAuthenticatorService extends Service{
    private WeathyAuthenticator mAuthenticator;

    @Override
    public void onCreate() {
        // Create a new authenticator object
        mAuthenticator = new WeathyAuthenticator(this);
    }

    /*
     * When the system binds to this Service to make the RPC call
     * return the authenticator's IBinder.
     */
    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
