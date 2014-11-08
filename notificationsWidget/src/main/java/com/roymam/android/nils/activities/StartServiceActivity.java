package com.roymam.android.nils.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.roymam.android.nils.services.NotificationsService;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.nils.common.SettingsManager;

public class StartServiceActivity extends Activity
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_enable_service);
    }

    public void openAndroidSettings(View view)
    {
        Intent intent = SettingsManager.getNotificationsServiesIntent();
        startActivity(intent);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        // start settings if the service has started
        if (NotificationsService.getSharedInstance() != null)
        {
            finish();
            startActivity(new Intent(getApplicationContext(), SettingsManager.class));
        }
    }

}