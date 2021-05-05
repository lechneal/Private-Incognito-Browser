package com.lechneralexander.privatebrowser.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;

import com.lechneralexander.privatebrowser.R;
import com.lechneralexander.privatebrowser.constant.Constants;
import com.lechneralexander.privatebrowser.preference.PreferenceManager;

@SuppressWarnings("deprecation")
public class MainActivity extends IncognitoActivity {
    public static final String PREFS_SHARED_FILE = "SharedPrefsFile";
    public static final String PREF_SEARCH_ENGINE_DIALOG_SHOWN = "SharedPrefsFile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            SharedPreferences settings = getSharedPreferences(PREFS_SHARED_FILE, 0);
            boolean dialogShown = settings.getBoolean(PREF_SEARCH_ENGINE_DIALOG_SHOWN, false);
            if (!dialogShown) {
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean(PREF_SEARCH_ENGINE_DIALOG_SHOWN, true);
                editor.commit();
                showSelectSearchEngineDialog(settings);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "error showing search engine dialog on startup", e);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (isPanicTrigger(intent)) {
            panicClean();
        } else {
            handleNewIntent(intent);
            super.onNewIntent(intent);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveOpenTabs();
    }

    private void showSelectSearchEngineDialog(final SharedPreferences settings) {
        AlertDialog.Builder picker = new AlertDialog.Builder(this);
        picker.setTitle(getResources().getString(R.string.title_search_engine));
        CharSequence[] chars = {"Google",
                "Ask", "Bing", "Yahoo", "StartPage", "StartPage (Mobile)",
                "DuckDuckGo (Privacy)", "DuckDuckGo Lite (Privacy)", "Baidu (Chinese)",
                "Yandex (Russian)"};

        final SharedPreferences mPreferenceManager = PreferenceManager.getSharedPreferences(this);
        int n = PreferenceManager.getSearchChoice(mPreferenceManager);

        picker.setSingleChoiceItems(chars, n - 1, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    PreferenceManager.setSearchChoice(which + 1, mPreferenceManager);
                } catch (Exception e) {
                    Log.e(Constants.TAG, "error updating selected search engine on startup", e);
                }
            }
        });
        picker.setNeutralButton(getResources().getString(R.string.action_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    initializeSearchEnginePreferences();
                    mTabsManager.getCurrentTab().loadHomepage();
                }catch (Exception e) {
                    Log.e(Constants.TAG, "error reinitializing after selecting search engine on startup", e);
                }
            }
        });
        picker.show();
    }

}
