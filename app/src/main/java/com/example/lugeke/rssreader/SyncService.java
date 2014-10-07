package com.example.lugeke.rssreader;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class SyncService extends Service {


    private static final String TAG="SyncService";
    private static final Object sSyncAdapterLock=new Object();
    private static SyncAdapter syncAdapter=null;
    public SyncService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG,"Service created");
        synchronized (sSyncAdapterLock){
            if(syncAdapter==null)
                syncAdapter=new SyncAdapter(getApplicationContext(),true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG,"Service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return syncAdapter.getSyncAdapterBinder();
    }
}
