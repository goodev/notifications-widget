package com.roymam.android.nils.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;

import com.roymam.android.common.SysUtils;
import com.roymam.android.nils.ui.NiLSActivity;
import com.roymam.android.notificationswidget.LSNotificationsSettingsActivity;
import com.roymam.android.notificationswidget.NiLSAccessibilityService;
import com.roymam.android.notificationswidget.R;
import com.roymam.android.nils.common.SettingsManager;

public class MainPrefsFragment extends Fragment
{
    private Context mContext;

    private SwitchCompat mEnableFPSwitch;
    private SwitchCompat mEnableHUSwitch;
    private ImageButton mFPSettingsButton;
    private ImageButton mFPAppearanceButton;
    private ImageButton mHUSettingsButton;
    private ImageButton mHUApperanceButton;
    private View mFPSettingsView;
    private View mHUSettingsView;
    private View mNiLSAccessibilityView;
    private Button mEnableNiLSAccessibilityButton;

    @Override
    public void onResume()
    {
        super.onResume();

        // NiLS Auto Hide Service Status
        if (NiLSAccessibilityService.isServiceRunning(mContext))
            mNiLSAccessibilityView.setVisibility(View.GONE);
        else
            mNiLSAccessibilityView.setVisibility(View.VISIBLE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.activity_home, null, false);
        mEnableFPSwitch = (SwitchCompat) v.findViewById(R.id.enable_fp_switch);
        mEnableHUSwitch = (SwitchCompat) v.findViewById(R.id.headsup_switch);
        mFPSettingsButton = (ImageButton) v.findViewById(R.id.fp_settings_button);
        mFPAppearanceButton = (ImageButton) v.findViewById(R.id.fp_appearance_button);
        mHUSettingsButton = (ImageButton) v.findViewById(R.id.headsup_settings_button);
        mHUApperanceButton = (ImageButton) v.findViewById(R.id.headsup_appearance_button);
        mFPSettingsView = v.findViewById(R.id.fp_settings_view);
        mHUSettingsView = v.findViewById(R.id.headsup_settings_view);
        mNiLSAccessibilityView = v.findViewById(R.id.nils_accessibility_is_not_enabled_view);
        mEnableNiLSAccessibilityButton = (Button) v.findViewById(R.id.nils_accessibility_button);

        return v;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getActivity();
    }

    @Override
    public void onStart() {
        super.onStart();

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        // Lock Screen Notifications Switch
        boolean fpEnabled = prefs.getBoolean(SettingsManager.FP_ENABLED, SettingsManager.DEFAULT_FP_ENABLED);
        mEnableFPSwitch.setChecked(fpEnabled);
        if (!fpEnabled) mFPSettingsView.setVisibility(View.GONE);
        mEnableFPSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(SettingsManager.FP_ENABLED, isChecked).commit();
                if (!isChecked)
                    mFPSettingsView.setVisibility(View.GONE);
                else
                    mFPSettingsView.setVisibility(View.VISIBLE);
            }
        });

        // Heads Up Switch
        boolean huEnabled = prefs.getBoolean(SettingsManager.POPUP_ENABLED, SettingsManager.DEFAULT_POPUP_ENABLED);
        mEnableHUSwitch.setChecked(huEnabled);
        if (!huEnabled) mHUSettingsView.setVisibility(View.GONE);
        mEnableHUSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean(SettingsManager.POPUP_ENABLED, isChecked).commit();
                if (!isChecked)
                    mHUSettingsView.setVisibility(View.GONE);
                else
                    mHUSettingsView.setVisibility(View.VISIBLE);
            }
        });

        // Notifications Panel settings button
        mFPSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              startActivity(new Intent(mContext, LSNotificationsSettingsActivity.class));
              //  ((NiLSActivity) getActivity()).switchFragment(NiLSActivity.INTERACTION_PAGE_INDEX);
            }
        });

        // Notifications Panel appearance button
        mFPAppearanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((NiLSActivity) getActivity()).switchFragment(NiLSActivity.APPEARANCE_PAGE_INDEX);
            }
        });

        // Notifications Panel settings button
        mHUSettingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: add Headsup settings fragment
                //((NiLSActivity) getActivity()).switchFragment(NiLSActivity.INTERACTION_PAGE_INDEX);
            }
        });

        // Heads Up appearance button
        mHUApperanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: add Headsup appearance fragment
                //((NiLSActivity) getActivity()).switchFragment(NiLSActivity.APPEARANCE_PAGE_INDEX);
            }
        });

        // Accessibility Service enable button
        mEnableNiLSAccessibilityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                Toast.makeText(getActivity(), R.string.enable_auto_hide_service_tip, Toast.LENGTH_LONG).show();
            }
        });
    }
}
