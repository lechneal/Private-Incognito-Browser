/*
 * Copyright 2014 A.C.R. Development
 */
package com.lechneralexander.privatebrowser.download;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;

import com.lechneralexander.privatebrowser.R;
import com.lechneralexander.privatebrowser.app.BrowserApp;
import com.lechneralexander.privatebrowser.constant.Constants;
import com.lechneralexander.privatebrowser.preference.PreferenceManager;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;

import javax.inject.Inject;

import static androidx.core.app.ActivityCompat.requestPermissions;
import static androidx.core.content.ContextCompat.startActivity;

public class LightningDownloadListener implements DownloadListener {

    private final Activity mActivity;

    @Inject PreferenceManager mPreferenceManager;

    public LightningDownloadListener(Activity context) {
        BrowserApp.getAppComponent().inject(this);
        mActivity = context;
    }

    @Override
    public void onDownloadStart(final String url, final String userAgent,
                                final String contentDisposition, final String mimetype, long contentLength) {
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(mActivity,
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                new PermissionsResultAction() {
                    @Override
                    public void onGranted() {
                        String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        DownloadHandler.onDownloadStart(mActivity, mPreferenceManager, url, userAgent,
                                                contentDisposition, mimetype);
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        break;
                                }
                            }
                        };

                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog
                        builder.setTitle(fileName)
                                .setMessage(mActivity.getResources().getString(R.string.dialog_download))
                                .setPositiveButton(mActivity.getResources().getString(R.string.action_download),
                                        dialogClickListener)
                                .setNegativeButton(mActivity.getResources().getString(R.string.action_cancel),
                                        dialogClickListener).show();
                        Log.i(Constants.TAG, "Downloading" + fileName);
                    }

                    @Override
                    public void onDenied(String permission) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity); // dialog

                        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                switch (which) {
                                    case DialogInterface.BUTTON_POSITIVE:
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                Uri.fromParts("package", mActivity.getApplicationContext().getPackageName(), null));
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                        mActivity.getApplicationContext().startActivity(intent);
                                        break;

                                    case DialogInterface.BUTTON_NEGATIVE:
                                        break;
                                }
                            }
                        };

                        builder.setTitle(mActivity.getResources().getString(R.string.dialog_download_permission_denied_header))
                                .setMessage(mActivity.getResources().getString(R.string.dialog_download_permission_denied_text))
                                .setPositiveButton(
                                        mActivity.getResources().getString(R.string.action_change_permission),
                                        dialogClickListener)
                                .setNegativeButton(
                                        mActivity.getResources().getString(R.string.action_cancel),
                                        dialogClickListener
                                )
                                .show();
                    }
                });
    }
}
