/*
 * Copyright 2014 A.C.R. Development
 */
package com.lechneralexander.privatebrowser.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import android.webkit.WebView;

import com.lechneralexander.privatebrowser.R;
import com.lechneralexander.privatebrowser.app.BrowserApp;
import com.lechneralexander.privatebrowser.utils.Utils;
import com.lechneralexander.privatebrowser.utils.WebUtils;
import com.lechneralexander.privatebrowser.view.LightningView;

public class PrivacySettingsFragment extends LightningPreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_CACHEEXIT = "clear_cache_exit";
    private static final String SETTINGS_PRIVATEDATAEXIT = "clear_private_data_on_exit";
    private static final String SETTINGS_CLEARPRIVATEDATA = "clear_private_data";
    private static final String SETTINGS_CLEARCACHE = "clear_cache";
    private static final String SETTINGS_CLEARCOOKIES = "clear_cookies";
    private static final String SETTINGS_CLEARWEBSTORAGE = "clear_webstorage";
    private static final String SETTINGS_COOKIESENABLED = "cookies_enabled";
    private static final String SETTINGS_IDENTIFYINGHEADERS = "remove_identifying_headers";
    private static final String SETTINGS_OPEN_PDF_IN_GOOGLE_DOCS = "open_pdf_with_google_docs";
    private static final String SETTINGS_FINISH_ON_PAUSE = "finish_on_pause";

    private Activity mActivity;
    private Handler mMessageHandler;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BrowserApp.getAppComponent().inject(this);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_privacy);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {
        Preference clearprivatedata = findPreference(SETTINGS_CLEARPRIVATEDATA);

        CheckBoxPreference cbcacheexit = (CheckBoxPreference) findPreference(SETTINGS_CACHEEXIT);
        CheckBoxPreference cbprivatedataexit = (CheckBoxPreference) findPreference(SETTINGS_PRIVATEDATAEXIT);
        CheckBoxPreference cbFinishOnPause = (CheckBoxPreference) findPreference(SETTINGS_FINISH_ON_PAUSE);
        CheckBoxPreference cbCookiesEnabled = (CheckBoxPreference) findPreference(SETTINGS_COOKIESENABLED);
        CheckBoxPreference cbIdentifyingHeaders = (CheckBoxPreference) findPreference(SETTINGS_IDENTIFYINGHEADERS);
        CheckBoxPreference cbGoogleDocs = (CheckBoxPreference) findPreference(SETTINGS_OPEN_PDF_IN_GOOGLE_DOCS);

        clearprivatedata.setOnPreferenceClickListener(this);

        cbcacheexit.setOnPreferenceChangeListener(this);
        cbprivatedataexit.setOnPreferenceChangeListener(this);
        cbFinishOnPause.setOnPreferenceChangeListener(this);
        cbCookiesEnabled.setOnPreferenceChangeListener(this);
        cbIdentifyingHeaders.setOnPreferenceChangeListener(this);
        cbGoogleDocs.setOnPreferenceChangeListener(this);

        cbcacheexit.setChecked(mPreferenceManager.getClearCacheExit());
        cbprivatedataexit.setChecked(mPreferenceManager.getClearPrivateDataExit());
        cbFinishOnPause.setChecked(mPreferenceManager.getFinishOnPause());
        cbCookiesEnabled.setChecked(mPreferenceManager.getCookiesEnabled());
        cbIdentifyingHeaders.setChecked(mPreferenceManager.getRemoveIdentifyingHeadersEnabled() && Utils.doesSupportHeaders());
        cbGoogleDocs.setChecked(mPreferenceManager.getOpenPdfInGoogleDocs());

        cbIdentifyingHeaders.setEnabled(Utils.doesSupportHeaders());

        String identifyingHeadersSummary = LightningView.HEADER_REQUESTED_WITH + ", " + LightningView.HEADER_WAP_PROFILE;
        cbIdentifyingHeaders.setSummary(identifyingHeadersSummary);


        mMessageHandler = new MessageHandler(mActivity);
    }

    private static class MessageHandler extends Handler {

        final Activity mHandlerContext;

        public MessageHandler(Activity context) {
            this.mHandlerContext = context;
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case 1:
                    Utils.showSnackbar(mHandlerContext, R.string.message_clear_history);
                    break;
                case 2:
                    Utils.showSnackbar(mHandlerContext, R.string.message_cookies_cleared);
                    break;
            }
            super.handleMessage(msg);
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_CLEARPRIVATEDATA:
                clearAllPrivateData();
                return true;
            case SETTINGS_CLEARCACHE:
                clearCache();
                return true;
            case SETTINGS_CLEARCOOKIES:
                clearCookiesDialog();
                return true;
            case SETTINGS_CLEARWEBSTORAGE:
                clearWebStorage();
                return true;
            default:
                return false;
        }
    }

    private void clearAllPrivateData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.title_clear_all));
        builder.setMessage(getResources().getString(R.string.dialog_clear_all))
                .setPositiveButton(getResources().getString(R.string.action_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                WebUtils.clearCache(getActivity());
                                WebUtils.clearWebStorage();
                                WebUtils.clearFormData(getActivity());

                                BrowserApp.getTaskThread().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        clearCookies();
                                    }
                                });
                                Utils.trimCache(getActivity());
                                Utils.showSnackbar(getActivity(), R.string.message_clear_privatedata);
                                Utils.clearAppCache(getActivity());
                                Utils.clearDatabases(getActivity());
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.action_no), null).show();
    }

    private void clearCookiesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(getResources().getString(R.string.title_clear_cookies));
        builder.setMessage(getResources().getString(R.string.dialog_cookies))
                .setPositiveButton(getResources().getString(R.string.action_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface arg0, int arg1) {
                                BrowserApp.getTaskThread().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        clearCookies();
                                    }
                                });
                            }
                        })
                .setNegativeButton(getResources().getString(R.string.action_no), null).show();
    }

    private void clearCache() {
        WebView webView = new WebView(mActivity);
        webView.clearCache(true);
        webView.destroy();
        Utils.showSnackbar(mActivity, R.string.message_cache_cleared);
    }


    private void clearCookies() {
        WebUtils.clearCookies(getActivity());
        mMessageHandler.sendEmptyMessage(2);
    }

    private void clearWebStorage() {
        WebUtils.clearWebStorage();
        Utils.showSnackbar(getActivity(), R.string.message_web_storage_cleared);
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        switch (preference.getKey()) {
            case SETTINGS_CACHEEXIT:
                mPreferenceManager.setClearCacheExit((Boolean) newValue);
                return true;
            case SETTINGS_PRIVATEDATAEXIT:
                mPreferenceManager.setClearPrivateDataExit((Boolean) newValue);
                return true;
            case SETTINGS_COOKIESENABLED:
                mPreferenceManager.setCookiesEnabled((Boolean) newValue);
                return true;
            case SETTINGS_IDENTIFYINGHEADERS:
                mPreferenceManager.setRemoveIdentifyingHeadersEnabled((Boolean) newValue);
                return true;
            case SETTINGS_OPEN_PDF_IN_GOOGLE_DOCS:
                mPreferenceManager.setOpenPdfInGoogleDocs((Boolean) newValue);
                return true;
            case SETTINGS_FINISH_ON_PAUSE:
                mPreferenceManager.setFinishOnPause((Boolean) newValue);
                return true;
            default:
                return false;
        }
    }
}
