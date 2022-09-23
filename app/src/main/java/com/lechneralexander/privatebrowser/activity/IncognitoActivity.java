package com.lechneralexander.privatebrowser.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.view.menu.MenuBuilder;

import android.util.Log;
import android.view.Menu;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.lechneralexander.privatebrowser.R;
import com.lechneralexander.privatebrowser.react.Action;
import com.lechneralexander.privatebrowser.react.Observable;
import com.lechneralexander.privatebrowser.react.Subscriber;

@SuppressWarnings("deprecation")
public class IncognitoActivity extends BrowserActivity {

    private static final String TAG = IncognitoActivity.class.getSimpleName();

    @Override
    public Observable<Void> updateCookiePreference() {
        return Observable.create(new Action<Void>() {
            @Override
            public void onSubscribe(@NonNull Subscriber<Void> subscriber) {
                CookieManager cookieManager = CookieManager.getInstance();
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    CookieSyncManager.createInstance(IncognitoActivity.this);
                }
                cookieManager.setAcceptCookie(mPreferences.getCookiesEnabled());
                subscriber.onComplete();
            }
        });
    }

    @SuppressLint("RestrictedApi")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        try {
            if (menu instanceof MenuBuilder)
                ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }catch (Exception e) {
            Log.e(TAG, "Error", e);
        }
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (mPreferences.getAllowScreenshots() == false) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mPreferences.getAllowScreenshots() == false) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // handleNewIntent(intent);
        super.onNewIntent(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // saveOpenTabs();
    }

    @Override
    public void updateHistory(@Nullable String title, @NonNull String url) {
        // addItemToHistory(title, url);
    }

    @Override
    public boolean isIncognito() {
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeActivity();
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (mPreferences.getFinishOnPause()){
            closeActivity();
        }
    }

    @Override
    public void closeActivity() {
        closeDrawers(new Runnable() {
            @Override
            public void run() {
                closeBrowser();
            }
        });
    }
}
