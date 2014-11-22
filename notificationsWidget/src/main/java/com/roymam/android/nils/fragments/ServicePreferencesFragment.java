package com.roymam.android.nils.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.util.Log;

import com.roymam.android.common.SysUtils;
import com.roymam.android.nils.common.SettingsManager;
import com.roymam.android.notificationswidget.R;

public class ServicePreferencesFragment extends NiLSPreferenceFragment
{
    private static final String TAG = ServicePreferencesFragment.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Load the global_settings from an XML resource
        addPreferencesFromResource(R.xml.service_preferences);
        unlockFeatures();

        // auto lock screen detection
        CheckBoxPreference autoDetectLockScreenAppPref = (CheckBoxPreference) findPreference(SettingsManager.AUTO_DETECT_LOCKSCREEN_APP);
        String currentLockScreenApp = SysUtils.getCurrentLockScreenAppName(getActivity());
        String currentLockScreenAppString = getResources().getString(R.string.current_lock_screen_app, currentLockScreenApp);
        autoDetectLockScreenAppPref.setSummary(currentLockScreenAppString);
        autoDetectLockScreenAppPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean)newValue)
                    // mark that the user requested to auto detect
                    getPreferenceScreen().getSharedPreferences().edit().putBoolean("user_defined_auto_detect", true).commit();
                return true;
            }
        });
    }

}
