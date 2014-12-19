package com.roymam.android.notificationswidget;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.util.Log;


@TargetApi(18)
public class NotificationsListener extends NotificationListenerService
{
    private final String TAG = this.getClass().getSimpleName();
    private static NotificationsListener instance = null;



    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG,"NotificationsListener:onStartCommand");
        instance = this;

        /*if (intent != null && intent.getAction() != null)
        {
            if (intent.getAction().equals(NotificationsService.CANCEL_NOTIFICATION))
            {

            }
            else if (intent.getAction().equals(NotificationsService.RELOAD_ACTIVE_NOTIFICATIONS))
                reloadActiveNotifications();
        }*/
        return super.onStartCommand(intent, flags, startId);
    }

    /*
    @Override
    public void onListenerConnected() {
        instance = this;
        reloadActiveNotifications();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            super.onListenerConnected();
    }*/

    public static NotificationsListener getInstance()
    {
        return instance;
    }

    public void cancelNotification(String packageName, int id, String tag, String key)
    {
        Log.d(TAG,"cancel notification #" + id);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                cancelNotification(key);
            else
                //noinspection deprecation
                cancelNotification(packageName, tag, id);
        }
        catch(java.lang.SecurityException exp)
        {
            Log.e(TAG, "security exception - cannot cancel notification.");
        }
    }

    private void reloadActiveNotifications() {
        try {
            StatusBarNotification[] notifications = getActiveNotifications();
            Log.d(TAG, "reloadActiveNotifications: active notifications:" + notifications.length);
            for (StatusBarNotification sbn : notifications)
                onNotificationPosted(sbn);
        }
        catch (Exception exp)
        {
            Log.w(TAG, "cannot reload active notifications");
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "NotificationsListener:onCreate");

        // Bind to NotificationsService
        Intent intent = new Intent(this, NotificationsService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

        instance = this;

        super.onCreate();
    }

    private NotificationsService mService;
    boolean mBound = false;

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service)
        {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            NotificationsService.LocalBinder binder = (NotificationsService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
            reloadActiveNotifications();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0)
        {
            mBound = false;
        }
    };


    @Override
    public void onDestroy()
    {
        Log.d(TAG, "NotificationsListener:onDestroy");

        // Unbind from the service
        if (mBound)
        {
            unbindService(mConnection);
            mBound = false;
        }

        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn)
    {
        boolean sideloaded = false;
        Log.d(TAG,"onNotificationPosted package:"+sbn.getPackageName()+" id:" + sbn.getId() + " tag:" + sbn.getTag());

        if (!mBound)
            Log.e(TAG, "Notifications Service is not bounded. stop and restart NotificationsListener to rebind it");
        else
        {
            String key = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                key = sbn.getKey();
                if (sbn.getGroupKey() != null && !NotificationCompat.isGroupSummary(sbn.getNotification()))
                    sideloaded = true;
            }
            mService.onNotificationPosted(sbn.getNotification(), sbn.getPackageName(), sbn.getId(), sbn.getTag(), key, sideloaded);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn)
    {
        Log.d(TAG,"onNotificationRemoved package:"+sbn.getPackageName()+" id:" + sbn.getId()+ " tag:" + sbn.getTag());

        if (!mBound)
            Log.e(TAG, "Notifications Service is not bounded. stop and restart NotificationsListener to rebind it");
        else
        {
            mService.onNotificationRemoved(sbn.getNotification(), sbn.getPackageName(), sbn.getId(), sbn.getTag());
        }
    }
}
