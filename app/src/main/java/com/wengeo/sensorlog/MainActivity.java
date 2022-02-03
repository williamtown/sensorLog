package com.wengeo.sensorlog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int NUMBER_OF_FRAGMENTS = 2;
    private static final int FRAGMENT_INDEX_SETTING = 0;
    private static final int FRAGMENT_INDEX_LOGGER = 1;
    private Fragment[] mFragments;
    private UiLogger mUiLogger;
    private FileLogger mFileLogger;

    private static final int LOCATION_REQUEST_ID = 1;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private MeasurementProvider mMeasurementProvider;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissionAndSetupFragments(this);
    }

    private boolean hasPermissions(Activity activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            // Permissions granted at install time.
            return true;
        }
        for (String p : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(activity, p) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void requestPermissionAndSetupFragments(final Activity activity) {
        if (hasPermissions(activity)) {
            setupFragments();
        } else {
            ActivityCompat.requestPermissions(activity, REQUIRED_PERMISSIONS, LOCATION_REQUEST_ID);
        }
    }

    private void setupFragments() {
        mUiLogger = new UiLogger();
        mFileLogger = new FileLogger(getApplicationContext());

        mMeasurementProvider = new MeasurementProvider(getApplicationContext(),mUiLogger,mFileLogger);
//                mGoogleApiClient,
//                mRealTimePositionVelocityCalculator,
//                mAgnssUiLogger);

        mFragments=new Fragment[NUMBER_OF_FRAGMENTS];
        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setGpsContainer(mMeasurementProvider);

        LoggerFragment loggerFragment = new LoggerFragment();
        loggerFragment.setUILogger(mUiLogger);
        loggerFragment.setFileLogger(mFileLogger);

        mFragments[FRAGMENT_INDEX_SETTING]=settingsFragment;
        mFragments[FRAGMENT_INDEX_LOGGER]=loggerFragment;
        FragmentManager fragmentManager=getSupportFragmentManager();

        // The viewpager that will host the section contents.
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(1);  // ***********
        ViewPagerAdapter adapter = new ViewPagerAdapter(fragmentManager,80);
        viewPager.setAdapter(adapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tab_layout);
        tabLayout.setTabsFromPagerAdapter(adapter);
    } // private void setupFragments()

    private class ViewPagerAdapter extends FragmentStatePagerAdapter {
        public ViewPagerAdapter(@NonNull FragmentManager fm, int behavior) {
            super(fm, behavior);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case FRAGMENT_INDEX_SETTING:
                    return mFragments[FRAGMENT_INDEX_SETTING];
                case FRAGMENT_INDEX_LOGGER:
                    return mFragments[FRAGMENT_INDEX_LOGGER];
                default:
                    throw new IllegalArgumentException("Invalid section: " + position);
            }
        }

        @Override
        public int getCount() {
            return NUMBER_OF_FRAGMENTS;
        }

        @Nullable
        @Override
        public CharSequence getPageTitle(int position) {
            Locale locale = Locale.getDefault();
            switch (position) {
                case FRAGMENT_INDEX_SETTING:
                    return "SETTINGS";
                case FRAGMENT_INDEX_LOGGER:
                    return "LOG";
                default:
                    return super.getPageTitle(position);
            }
        }
    } // private class ViewPagerAdapter extends FragmentStatePagerAdapter

} // public class MainActivity extends AppCompatActivity