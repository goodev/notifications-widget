package com.roymam.android.nils.activities;

import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.roymam.android.nils.fragments.CardPreferenceFragment;
import com.roymam.android.notificationswidget.R;

public class SpecificSettingsPreferencesActivity extends ActionBarActivity
{
    public static final String EXTRA_PACKAGE_NAME = "com.roymam.android.notificationswidget.packagename";
    protected String packageName;
    protected PreferenceFragment fragment;

    protected void onCreate(Bundle savedInstanceState, int titleRes, int layout, int preferencesXml)
    {
        super.onCreate(savedInstanceState);

        packageName = getIntent().getStringExtra(EXTRA_PACKAGE_NAME);

        // get package title
        try
        {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(packageName, 0);
            String appName = getPackageManager().getApplicationLabel(ai).toString();
            if (appName == null) appName = packageName;
            setTitle(appName + " - " + getString(titleRes));
        }
        catch (PackageManager.NameNotFoundException e)
        {
            setTitle(packageName + " - " + getString(titleRes));
        }


        LayoutInflater inflater = getLayoutInflater();
        View v = inflater.inflate(layout, null);
        setContentView(v);

        fragment = new SpecificSettingsPreferenceFragment();
        Bundle args = new Bundle();
        args.putInt("layout_id", layout);
        args.putInt("prefs", preferencesXml);
        args.putString("package", packageName);

        fragment.setArguments(args);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction()
                .replace(R.id.content_frame, fragment)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle other action bar items...
        switch (item.getItemId())
        {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class SpecificSettingsPreferenceFragment extends PreferenceFragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            int layoutId = getArguments().getInt("layout_id");
            int preferencesXml = getArguments().getInt("prefs");
            String packageName = getArguments().getString("package");

            View view = super.onCreateView(inflater, container, savedInstanceState);

            addPreferencesFromResource(preferencesXml);

            PreferenceScreen prefScreen = getPreferenceScreen();
            SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

            for(int i=0; i<prefScreen.getPreferenceCount();i++)
            {
                PreferenceGroup prefGroup = (PreferenceGroup) prefScreen.getPreference(i);

                for (int j=0; j<prefGroup.getPreferenceCount(); j++) {
                    Preference pref = prefGroup.getPreference(j);
                    String key = packageName + "." + pref.getKey();

                    if (pref instanceof ListPreference)
                    {
                        ListPreference listPref = ((ListPreference) pref);
                        String globalValue = listPref.getValue();
                        String currValue = sharedPrefs.getString(key, globalValue);

                        listPref.setKey(key);
                        listPref.setValue(currValue);
                    }
                    else if (pref instanceof CheckBoxPreference)
                    {
                        CheckBoxPreference checkPref = (CheckBoxPreference) pref;
                        boolean globalValue = checkPref.isChecked();
                        boolean currValue = sharedPrefs.getBoolean(key, globalValue);
                        checkPref.setKey(key);
                        checkPref.setChecked(currValue);
                    }
                }
            }

            // apply card layout
            CardPreferenceFragment.applyLayoutToPreferences(prefScreen);

            return view;
        }
    }
}
