package com.roymam.android.nils.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import com.roymam.android.notificationswidget.R;
import com.roymam.android.nils.common.SettingsManager;

public class AppSettingsActivity extends SpecificSettingsPreferencesActivity implements OnSharedPreferenceChangeListener
{
	public static final String IGNORE_APP = "ignoreapp";
    public static final String USE_EXPANDED_TEXT = "useexpandedtext";
    public static final String APP_PRIORITY = "apppriority";
    public static final String RETRANSMIT = "retransmit";
    public static final String IGNORE_REPEATING_NOTIFICATIONS = "ignore_repeating_notifications";

    public static final String[] APP_SPECIFIC_SETTINGS_KEYS = {
            SettingsManager.WAKEUP_MODE,
            SettingsManager.NOTIFICATION_MODE,
            SettingsManager.NOTIFICATION_ICON,
            SettingsManager.NOTIFICATION_PRIVACY,
            IGNORE_APP,
            USE_EXPANDED_TEXT,
            APP_PRIORITY,
            RETRANSMIT,
            IGNORE_REPEATING_NOTIFICATIONS
    };

    public static final boolean DEFAULT_RETRANSMIT = false;
    public static final boolean DEFAULT_IGNORE_REPEATING_NOTIFICATIONS = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) 
	{
        onCreate(savedInstanceState, R.string.app_specific_settings_title, R.layout.activity_app_settings, R.xml.app_specific_settings);
	}

    public void resetAppSettings(View v)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppSettingsActivity.this);

        // remove all app specific settings
        SharedPreferences.Editor prefsEdit = prefs.edit();
        for(String key : APP_SPECIFIC_SETTINGS_KEYS)
            prefsEdit.remove(packageName + "." + key);

        prefsEdit.commit();

        removeAppFromAppSpecificSettings(packageName, AppSettingsActivity.this);
        finish();
        Intent intent = new Intent(getApplicationContext(), this.getClass());
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivity(intent);
        overridePendingTransition(0,0);
    }
    public void ignoreThisApp(View v)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AppSettingsActivity.this);
        prefs.edit().putBoolean(packageName+"."+IGNORE_APP, true).commit();
        addAppToAppSpecificSettings(packageName, getApplicationContext());
        finish();
    }
	
	public static void removeAppFromAppSpecificSettings(String packageName, Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
		
		String specficApps = prefs.getString(SettingsManager.APPS_SETTINGS, "");
		String updatedSpecificApps = "";
		for(String app:specficApps.split(","))
		{
			if (!app.equals(packageName))
			{
				if (updatedSpecificApps.isEmpty())
					updatedSpecificApps = app;
				else
					updatedSpecificApps+=","+app;
			}
		}
		prefs.edit().putString(SettingsManager.APPS_SETTINGS, updatedSpecificApps).commit();
	}
	
	public static void addAppToAppSpecificSettings(String packageName, Context ctx)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
	
		String specificApps = prefs.getString(SettingsManager.APPS_SETTINGS, "");
		boolean hasApp = false;
		for (String token : specificApps.split(",")) 
		{
		     if (token.equals(packageName)) hasApp = true;
		}
        // remove it and re-add to the start of the list
        if (hasApp)
            removeAppFromAppSpecificSettings(packageName, ctx);

        // add this app to the list of specific apps
        specificApps = prefs.getString(SettingsManager.APPS_SETTINGS, "");
        if (!specificApps.equals(""))
            specificApps = packageName + "," + specificApps;
        else
            specificApps = packageName;
		
		prefs.edit().putString("specificapps", specificApps).commit();		
	}
	
	@Override
	protected void onResume() 
	{
	    super.onResume();	   
	    PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	protected void onPause() 
	{
	    super.onPause();
	    PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
	}
		
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key)
	{
		if (key.startsWith(packageName))
		{
			String specificApps = sharedPreferences.getString(SettingsManager.APPS_SETTINGS, "");
			boolean hasApp = false;
			for (String token : specificApps.split(",")) 
			{
			     if (token.equals(packageName)) hasApp = true;
			}
			if (!hasApp)
			{
				// add this app to the list of specific apps
				if (!specificApps.equals(""))
					specificApps+= ",";
				specificApps+= packageName; 
			}
			
			sharedPreferences.edit().putString("specificapps", specificApps).commit();
		}
	}
}