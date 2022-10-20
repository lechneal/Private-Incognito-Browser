package com.lechneralexander.privatebrowser.activity;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import javax.inject.Inject;

import com.lechneralexander.privatebrowser.R;
import com.lechneralexander.privatebrowser.app.AppComponent;
import com.lechneralexander.privatebrowser.app.BrowserApp;
import com.lechneralexander.privatebrowser.preference.PreferenceManager;

public abstract class ThemableBrowserActivity extends AppCompatActivity {

    @Inject PreferenceManager mPreferences;

    private int mTheme;
    private boolean mShowTabsInDrawer;
    private boolean mShouldRunOnResumeActions = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppComponent appComponent = BrowserApp.getAppComponent();
        if (appComponent != null) {
            BrowserApp.getAppComponent().inject(this);
        }
        if (mPreferences != null) {
            mTheme = mPreferences.getUseTheme();
            mShowTabsInDrawer = mPreferences.getShowTabsInDrawer(!isTablet());

            // set the theme
            if (mTheme == 1) {
                setTheme(R.style.Theme_DarkTheme);
            } else if (mTheme == 2) {
                setTheme(R.style.Theme_BlackTheme);
            }
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && mShouldRunOnResumeActions) {
            mShouldRunOnResumeActions = false;
            onWindowVisibleToUserAfterResume();
        }
    }

    /**
     * Called after the activity is resumed
     * and the UI becomes visible to the user.
     * Called by onWindowFocusChanged only if
     * onResume has been called.
     */
    public void onWindowVisibleToUserAfterResume() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        mShouldRunOnResumeActions = true;
        int theme = mPreferences.getUseTheme();
        boolean drawerTabs = mPreferences.getShowTabsInDrawer(!isTablet());
        if (theme != mTheme || mShowTabsInDrawer != drawerTabs) {
            restart();
        }
    }

    boolean isTablet() {
        return (getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    private void restart() {
        finish();
        startActivity(new Intent(this, getClass()));
    }
}
