package com.example.lugeke.rssreader;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.example.lugeke.rssreader.common.accounts.GenericAccountService;
import com.example.lugeke.rssreader.provider.FeedContract;

/**
 * Created by lugeke on 2014/10/1.
 */
public class SyncUtils {
    private static final long SYNC_FREQUENCY=60*60*2;//2 hours
    private static final String CONTENT_AUTHORITY= FeedContract.AUTHORITY;
    private static final String ACCOUNT_TYPE="com.example.lugeke.rssreader.account";
    private static final String PREF_SETUP_COMPLETE="setup_complete";

    public static void CreateSyncAccount(Context context){
        boolean newAccount=false;
        boolean setupComplete = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_SETUP_COMPLETE,false);

        Account account= GenericAccountService.GetAccount(ACCOUNT_TYPE);
        AccountManager accountManager=(AccountManager)context.getSystemService(Context.ACCOUNT_SERVICE);
        if(accountManager.addAccountExplicitly(account,null,null)){
            // Inform the system that this account supports sync
            ContentResolver.setIsSyncable(account, CONTENT_AUTHORITY, 1);
            // Inform the system that this account is eligible for auto sync when the network is up
            ContentResolver.setSyncAutomatically(account, CONTENT_AUTHORITY, true);
            // Recommend a schedule for automatic synchronization. The system may modify this based
            // on other scheduled syncs and network utilization.
            ContentResolver.addPeriodicSync(
                    account, CONTENT_AUTHORITY, new Bundle(),SYNC_FREQUENCY);
            newAccount = true;
        }
        // Schedule an initial sync if we detect problems with either our account or our local
        // data has been deleted. (Note that it's possible to clear app data WITHOUT affecting
        // the account list, so wee need to check both.)
        if (newAccount || !setupComplete) {
            TriggerRefresh();
            PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putBoolean(PREF_SETUP_COMPLETE, true).commit();
        }
    }

    public static void TriggerRefresh() {
        Bundle b = new Bundle();
        // Disable sync backoff and ignore sync preferences. In other words...perform sync NOW!
        b.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        b.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        ContentResolver.requestSync(
                GenericAccountService.GetAccount(ACCOUNT_TYPE), // Sync account
                FeedContract.AUTHORITY,                 // Content authority
                b);                                             // Extras
    }
}
