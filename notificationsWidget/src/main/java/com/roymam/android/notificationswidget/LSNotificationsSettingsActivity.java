package com.roymam.android.notificationswidget;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.view.View;

import com.roymam.android.nils.fragments.ServicePreferencesFragment;

public class LSNotificationsSettingsActivity extends ActionBarActivity  {

    private ViewPager mPager;
    private FragmentStatePagerAdapter mPagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lsnotifications_settings);

        mPager = (ViewPager) findViewById(R.id.vp_main);
        mPagerAdapter = new LSSettingsPagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);
    }

    /**
     * A simple pager adapter that represents 5 ScreenSlidePageFragment objects, in
     * sequence.
     */
    private class LSSettingsPagerAdapter extends FragmentStatePagerAdapter {
        public CharSequence[] mTitles = {getString(R.string.settings), getString(R.string.appearance)};

        public LSSettingsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch(position)
            {
                case 0:
                    return new ServicePreferencesFragment();
                case 1:

                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }
    }
}
