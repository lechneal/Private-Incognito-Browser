/*
 * Copyright 2014 A.C.R. Development
 */
package com.lechneralexander.privatebrowser.fragment;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.lechneralexander.privatebrowser.R;
import com.lechneralexander.privatebrowser.download.DownloadHandler;
import com.lechneralexander.privatebrowser.utils.ConfigUtils;
import com.lechneralexander.privatebrowser.utils.ThemeUtils;

public class GeneralSettingsFragment extends LightningPreferenceFragment implements Preference.OnPreferenceClickListener, Preference.OnPreferenceChangeListener {

    private static final String SETTINGS_ADS = "cb_ads";
    private static final String SETTINGS_IMAGES = "cb_images";
    private static final String SETTINGS_JAVASCRIPT = "cb_javascript";
    private static final String SETTINGS_SCREENSHOTS = "cb_screenshots";
    private static final String SETTINGS_COLORMODE = "cb_colormode";
    private static final String SETTINGS_USERAGENT = "agent";
    private static final String SETTINGS_DOWNLOAD = "download";
    private static final String SETTINGS_HOME = "home";
    private static final String SETTINGS_SEARCHENGINE = "search";
    private static final String SETTINGS_DRAWERTABS = "cb_drawertabs";

    private Activity mActivity;
    private static final int API = android.os.Build.VERSION.SDK_INT;
    private Preference proxy, useragent, downloadloc, home, searchengine;
    private String mDownloadLocation;
    private int mAgentChoice;
    private String mHomepage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preference_general);

        mActivity = getActivity();

        initPrefs();
    }

    private void initPrefs() {
        useragent = findPreference(SETTINGS_USERAGENT);
        downloadloc = findPreference(SETTINGS_DOWNLOAD);
        home = findPreference(SETTINGS_HOME);
        searchengine = findPreference(SETTINGS_SEARCHENGINE);

        CheckBoxPreference cbAds = (CheckBoxPreference) findPreference(SETTINGS_ADS);
        CheckBoxPreference cbImages = (CheckBoxPreference) findPreference(SETTINGS_IMAGES);
        CheckBoxPreference cbJsScript = (CheckBoxPreference) findPreference(SETTINGS_JAVASCRIPT);
        CheckBoxPreference cbScreenshots = (CheckBoxPreference) findPreference(SETTINGS_SCREENSHOTS);
        CheckBoxPreference cbColorMode = (CheckBoxPreference) findPreference(SETTINGS_COLORMODE);
        CheckBoxPreference cbDrawerTabs = (CheckBoxPreference) findPreference(SETTINGS_DRAWERTABS);

        useragent.setOnPreferenceClickListener(this);
        downloadloc.setOnPreferenceClickListener(this);
        home.setOnPreferenceClickListener(this);
        searchengine.setOnPreferenceClickListener(this);
        cbAds.setOnPreferenceChangeListener(this);
        cbImages.setOnPreferenceChangeListener(this);
        cbJsScript.setOnPreferenceChangeListener(this);
        cbScreenshots.setOnPreferenceChangeListener(this);
        cbColorMode.setOnPreferenceChangeListener(this);
        cbDrawerTabs.setOnPreferenceChangeListener(this);

        mAgentChoice = mPreferenceManager.getUserAgentChoice(ConfigUtils.getDefaultUserAgent(getActivity()));
        mHomepage = mPreferenceManager.getHomepage();
        mDownloadLocation = mPreferenceManager.getDownloadDirectory();


        setSearchEngineSummary(mPreferenceManager.getSearchChoice());

        downloadloc.setSummary(mDownloadLocation);

        if (mHomepage.contains("about:home")) {
            home.setSummary(getResources().getString(R.string.action_homepage));
        } else if (mHomepage.contains("about:blank")) {
            home.setSummary(getResources().getString(R.string.action_blank));
        } else if (mHomepage.contains("about:bookmarks")) {
            home.setSummary(getResources().getString(R.string.action_bookmarks));
        } else {
            home.setSummary(mHomepage);
        }

        switch (mAgentChoice) {
            case 1:
                useragent.setSummary(getResources().getString(R.string.agent_default));
                break;
            case 2:
                useragent.setSummary(getResources().getString(R.string.agent_desktop));
                break;
            case 3:
                useragent.setSummary(getResources().getString(R.string.agent_mobile));
                break;
            case 4:
                useragent.setSummary(getResources().getString(R.string.agent_custom));
        }

        boolean imagesBool = mPreferenceManager.getBlockImagesEnabled();
        boolean enableJSBool = mPreferenceManager.getJavaScriptEnabled();

        cbAds.setEnabled(true);

        cbImages.setChecked(imagesBool);
        cbJsScript.setChecked(enableJSBool);
        cbScreenshots.setChecked(mPreferenceManager.getAllowScreenshots());
        cbAds.setChecked(mPreferenceManager.getAdBlockEnabled());
        cbColorMode.setChecked(mPreferenceManager.getColorModeEnabled());
        cbDrawerTabs.setChecked(mPreferenceManager.getShowTabsInDrawer(true));
    }

    private void searchUrlPicker() {
        final AlertDialog.Builder urlPicker = new AlertDialog.Builder(mActivity);
        urlPicker.setTitle(getResources().getString(R.string.custom_url));
        final EditText getSearchUrl = new EditText(mActivity);
        String mSearchUrl = mPreferenceManager.getSearchUrl();
        getSearchUrl.setText(mSearchUrl);
        urlPicker.setView(getSearchUrl);
        urlPicker.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = getSearchUrl.getText().toString();
                        mPreferenceManager.setSearchUrl(text);
                        searchengine.setSummary(getResources().getString(R.string.custom_url) + ": "
                                + text);
                    }
                });
        urlPicker.show();
    }



    private void searchDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_search_engine));
        CharSequence[] chars = {getResources().getString(R.string.custom_url), "Google",
                "Ask", "Bing", "Yahoo", "StartPage", "StartPage (Mobile)",
                "DuckDuckGo (Privacy)", "DuckDuckGo Lite (Privacy)", "Baidu (Chinese)",
                "Yandex (Russian)"};

        int n = mPreferenceManager.getSearchChoice();

        picker.setSingleChoiceItems(chars, n, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                mPreferenceManager.setSearchChoice(which);
                setSearchEngineSummary(which);
            }
        });
        picker.setNeutralButton(getResources().getString(R.string.action_ok), null);
        picker.show();
    }

    private void homepageDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.home));
        mHomepage = mPreferenceManager.getHomepage();
        int n;
        if (mHomepage.contains("about:home")) {
            n = 1;
        } else if (mHomepage.contains("about:blank")) {
            n = 2;
        } else if (mHomepage.contains("about:bookmarks")) {
            n = 3;
        } else {
            n = 4;
        }

        picker.setSingleChoiceItems(R.array.homepage, n - 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which + 1) {
                            case 1:
                                mPreferenceManager.setHomepage("about:home");
                                home.setSummary(getResources().getString(R.string.action_homepage));
                                break;
                            case 2:
                                mPreferenceManager.setHomepage("about:blank");
                                home.setSummary(getResources().getString(R.string.action_blank));
                                break;
                            case 3:
                                mPreferenceManager.setHomepage("about:bookmarks");
                                home.setSummary(getResources().getString(R.string.action_bookmarks));
                                break;
                            case 4:
                                homePicker();
                                break;
                        }
                    }
                });
        picker.setNeutralButton(getResources().getString(R.string.action_ok), null);
        picker.show();
    }

    private void homePicker() {
        final AlertDialog.Builder homePicker = new AlertDialog.Builder(mActivity);
        homePicker.setTitle(getResources().getString(R.string.title_custom_homepage));
        final EditText getHome = new EditText(mActivity);
        mHomepage = mPreferenceManager.getHomepage();
        if (!mHomepage.startsWith("about:")) {
            getHome.setText(mHomepage);
        } else {
            String defaultUrl = "https://www.google.com";
            getHome.setText(defaultUrl);
        }
        homePicker.setView(getHome);
        homePicker.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = getHome.getText().toString();
                        mPreferenceManager.setHomepage(text);
                        home.setSummary(text);
                    }
                });
        homePicker.show();
    }

    private void downloadLocDialog() {
        AlertDialog.Builder picker = new AlertDialog.Builder(mActivity);
        picker.setTitle(getResources().getString(R.string.title_download_location));
        mDownloadLocation = mPreferenceManager.getDownloadDirectory();
        int n;
        if (mDownloadLocation.contains(Environment.DIRECTORY_DOWNLOADS)) {
            n = 0;
        } else {
            n = 1;
        }

        picker.setSingleChoiceItems(R.array.download_folder, n,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                mPreferenceManager.setDownloadDirectory(DownloadHandler.DEFAULT_DOWNLOAD_PATH);
                                downloadloc.setSummary(DownloadHandler.DEFAULT_DOWNLOAD_PATH);
                                break;
                            case 1:
                                downPicker();
                                break;
                        }
                    }
                });
        picker.setNeutralButton(getResources().getString(R.string.action_ok), null);
        picker.show();
    }

    private void agentDialog() {
        AlertDialog.Builder agentPicker = new AlertDialog.Builder(mActivity);
        agentPicker.setTitle(getResources().getString(R.string.agent));
        mAgentChoice = mPreferenceManager.getUserAgentChoice(ConfigUtils.getDefaultUserAgent(getActivity()));
        agentPicker.setSingleChoiceItems(R.array.user_agent, mAgentChoice - 1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mPreferenceManager.setUserAgentChoice(which + 1);
                        switch (which + 1) {
                            case 1:
                                useragent.setSummary(getResources().getString(R.string.agent_default));
                                break;
                            case 2:
                                useragent.setSummary(getResources().getString(R.string.agent_desktop));
                                break;
                            case 3:
                                useragent.setSummary(getResources().getString(R.string.agent_mobile));
                                break;
                            case 4:
                                useragent.setSummary(getResources().getString(R.string.agent_custom));
                                agentPicker();
                                break;
                        }
                    }
                });
        agentPicker.setNeutralButton(getResources().getString(R.string.action_ok), null);
        agentPicker.show();
    }

    private void agentPicker() {
        final AlertDialog.Builder agentStringPicker = new AlertDialog.Builder(mActivity);
        agentStringPicker.setTitle(getResources().getString(R.string.agent));
        final EditText getAgent = new EditText(mActivity);
        agentStringPicker.setView(getAgent);
        agentStringPicker.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = getAgent.getText().toString();
                        mPreferenceManager.setUserAgentString(text);
                        useragent.setSummary(getResources().getString(R.string.agent_custom));
                    }
                });
        agentStringPicker.show();
    }

    private void downPicker() {
        final AlertDialog.Builder downLocationPicker = new AlertDialog.Builder(mActivity);
        LinearLayout layout = new LinearLayout(mActivity);
        downLocationPicker.setTitle(getResources().getString(R.string.title_download_location));
        final EditText getDownload = new EditText(mActivity);
        getDownload.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        getDownload.setText(mPreferenceManager.getDownloadDirectory());
        final int errorColor = ContextCompat.getColor(getActivity(), R.color.error_red);
        final int regularColor = ThemeUtils.getTextColor(getActivity());
        getDownload.setTextColor(regularColor);
        getDownload.addTextChangedListener(new DownloadLocationTextWatcher(getDownload, errorColor, regularColor));
        getDownload.setText(mPreferenceManager.getDownloadDirectory());

        layout.addView(getDownload);
        downLocationPicker.setView(layout);
        downLocationPicker.setPositiveButton(getResources().getString(R.string.action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String text = getDownload.getText().toString();
                        text = DownloadHandler.addNecessarySlashes(text);
                        mPreferenceManager.setDownloadDirectory(text);
                        downloadloc.setSummary(text);
                    }
                });
        downLocationPicker.show();
    }

    private void setSearchEngineSummary(int which) {
        switch (which) {
            case 0:
                searchUrlPicker();
                break;
            case 1:
                searchengine.setSummary("Google");
                break;
            case 2:
                searchengine.setSummary("Ask");
                break;
            case 3:
                searchengine.setSummary("Bing");
                break;
            case 4:
                searchengine.setSummary("Yahoo");
                break;
            case 5:
                searchengine.setSummary("StartPage");
                break;
            case 6:
                searchengine.setSummary("StartPage (Mobile)");
                break;
            case 7:
                searchengine.setSummary("DuckDuckGo");
                break;
            case 8:
                searchengine.setSummary("DuckDuckGo Lite");
                break;
            case 9:
                searchengine.setSummary("Baidu");
                break;
            case 10:
                searchengine.setSummary("Yandex");
        }
    }

    @Override
    public boolean onPreferenceClick(@NonNull Preference preference) {
        switch (preference.getKey()) {
            case SETTINGS_USERAGENT:
                agentDialog();
                return true;
            case SETTINGS_DOWNLOAD:
                downloadLocDialog();
                return true;
            case SETTINGS_HOME:
                homepageDialog();
                return true;
            case SETTINGS_SEARCHENGINE:
                searchDialog();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean onPreferenceChange(@NonNull Preference preference, Object newValue) {
        boolean checked = false;
        if (newValue instanceof Boolean) {
            checked = (Boolean) newValue;
        }
        switch (preference.getKey()) {
            case SETTINGS_ADS:
                mPreferenceManager.setAdBlockEnabled(checked);
                return true;
            case SETTINGS_IMAGES:
                mPreferenceManager.setBlockImagesEnabled(checked);
                return true;
            case SETTINGS_JAVASCRIPT:
                mPreferenceManager.setJavaScriptEnabled(checked);
                return true;
            case SETTINGS_SCREENSHOTS:
                mPreferenceManager.setScreenshotsAllowed(checked);
                return true;
            case SETTINGS_COLORMODE:
                mPreferenceManager.setColorModeEnabled(checked);
                return true;
            case SETTINGS_DRAWERTABS:
                mPreferenceManager.setShowTabsInDrawer(checked);
                return true;
            default:
                return false;
        }
    }

    private static class DownloadLocationTextWatcher implements TextWatcher {
        private final EditText getDownload;
        private final int errorColor;
        private final int regularColor;

        public DownloadLocationTextWatcher(EditText getDownload, int errorColor, int regularColor) {
            this.getDownload = getDownload;
            this.errorColor = errorColor;
            this.regularColor = regularColor;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(@NonNull Editable s) {
            if (!DownloadHandler.isWriteAccessAvailable(s.toString())) {
                this.getDownload.setTextColor(this.errorColor);
            } else {
                this.getDownload.setTextColor(this.regularColor);
            }
        }
    }
}
