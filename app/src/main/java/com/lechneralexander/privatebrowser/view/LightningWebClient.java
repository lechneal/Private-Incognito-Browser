package com.lechneralexander.privatebrowser.view;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.MailTo;
import android.net.http.SslError;
import android.os.Build;
import android.os.Message;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;

import java.io.ByteArrayInputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.lechneralexander.privatebrowser.R;
import com.lechneralexander.privatebrowser.app.BrowserApp;
import com.lechneralexander.privatebrowser.constant.Constants;
import com.lechneralexander.privatebrowser.controller.UIController;
import com.lechneralexander.privatebrowser.preference.PreferenceManager;
import com.lechneralexander.privatebrowser.utils.AdBlock;
import com.lechneralexander.privatebrowser.utils.IntentUtils;
import com.lechneralexander.privatebrowser.utils.Preconditions;
import com.lechneralexander.privatebrowser.utils.Utils;

public class LightningWebClient extends WebViewClient {

    @NonNull private final Activity mActivity;
    @NonNull private final LightningView mLightningView;
    @NonNull private final UIController mUIController;
    @NonNull private final IntentUtils mIntentUtils;

    @Inject AdBlock mAdBlock;
    @Inject PreferenceManager mPreferences;

    LightningWebClient(@NonNull Activity activity, @NonNull LightningView lightningView) {
        BrowserApp.getAppComponent().inject(this);
        Preconditions.checkNonNull(activity);
        Preconditions.checkNonNull(lightningView);
        mActivity = activity;
        mUIController = (UIController) activity;
        mLightningView = lightningView;
        mAdBlock.updatePreference();
        mIntentUtils = new IntentUtils(activity);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, @NonNull WebResourceRequest request) {
        if (mAdBlock.isAd(request.getUrl().toString())) {
            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            return new WebResourceResponse("text/plain", "utf-8", EMPTY);
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Nullable
    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (mAdBlock.isAd(url)) {
            ByteArrayInputStream EMPTY = new ByteArrayInputStream("".getBytes());
            return new WebResourceResponse("text/plain", "utf-8", EMPTY);
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onPageFinished(@NonNull WebView view, String url) {
        if (view.isShown()) {
            mUIController.updateUrl(url, true);
            mUIController.setBackButtonEnabled(view.canGoBack());
            mUIController.setForwardButtonEnabled(view.canGoForward());
            view.postInvalidate();
        }
        if (view.getTitle() == null || view.getTitle().isEmpty()) {
            mLightningView.getTitleInfo().setTitle(mActivity.getString(R.string.untitled));
        } else {
            mLightningView.getTitleInfo().setTitle(view.getTitle());
        }
        if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT &&
                mLightningView.getInvertePage()) {
            view.evaluateJavascript(Constants.JAVASCRIPT_INVERT_PAGE, null);
        }
        mUIController.tabChanged(mLightningView);
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        mLightningView.getTitleInfo().setFavicon(null);
        if (mLightningView.isShown()) {
            mUIController.updateUrl(url, false);
            mUIController.showActionBar();
        }
        mUIController.tabChanged(mLightningView);
    }

    private static boolean isGoogleDoc(String url) {
        if (
                (url.endsWith(".pdf") ||
                        url.endsWith(".doc") ||
                        url.endsWith(".docx") ||
                        url.endsWith(".xls") ||
                        url.endsWith(".xlsx") ||
                        url.endsWith(".ppt") ||
                        url.endsWith(".pptx") ||
                        url.endsWith(".eps")
                )
                && !url.startsWith("http://docs.google.com")
                && !url.startsWith("https://docs.google.com")
                && !url.startsWith("http://drive.google.com")
                && !url.startsWith("https://drive.google.com")
                ){
            return true;
        }
        return false;
    }

    @Override
    public void onReceivedHttpAuthRequest(final WebView view, @NonNull final HttpAuthHandler handler,
                                          final String host, final String realm) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        final EditText name = new EditText(mActivity);
        final EditText password = new EditText(mActivity);
        LinearLayout passLayout = new LinearLayout(mActivity);
        passLayout.setOrientation(LinearLayout.VERTICAL);

        passLayout.addView(name);
        passLayout.addView(password);

        name.setHint(mActivity.getString(R.string.hint_username));
        name.setSingleLine();
        password.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
        password.setSingleLine();
        password.setTransformationMethod(new PasswordTransformationMethod());
        password.setHint(mActivity.getString(R.string.hint_password));
        builder.setTitle(mActivity.getString(R.string.title_sign_in));
        builder.setView(passLayout);
        builder.setCancelable(true)
                .setPositiveButton(mActivity.getString(R.string.title_sign_in),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                String user = name.getText().toString();
                                String pass = password.getText().toString();
                                handler.proceed(user.trim(), pass.trim());
                                Log.d(Constants.TAG, "Request Login");

                            }
                        })
                .setNegativeButton(mActivity.getString(R.string.action_cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                handler.cancel();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();

    }

    private boolean mIsRunning = false;
    private float mZoomScale = 0.0f;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onScaleChanged(@NonNull final WebView view, final float oldScale, final float newScale) {
        if (view.isShown() && mLightningView.mPreferences.getTextReflowEnabled() &&
                Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (mIsRunning)
                return;
            if (Math.abs(mZoomScale - newScale) > 0.01f) {
                mIsRunning = view.postDelayed(new Runnable() {

                    @Override
                    public void run() {
                        mZoomScale = newScale;
                        view.evaluateJavascript(Constants.JAVASCRIPT_TEXT_REFLOW, null);
                        mIsRunning = false;
                    }

                }, 100);
            }

        }
    }

    @NonNull
    private static List<Integer> getAllSslErrorMessageCodes(@NonNull SslError error) {
        List<Integer> errorCodeMessageCodes = new ArrayList<>(1);

        if (error.hasError(SslError.SSL_DATE_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_date_invalid);
        }
        if (error.hasError(SslError.SSL_EXPIRED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_expired);
        }
        if (error.hasError(SslError.SSL_IDMISMATCH)) {
            errorCodeMessageCodes.add(R.string.message_certificate_domain_mismatch);
        }
        if (error.hasError(SslError.SSL_NOTYETVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_not_yet_valid);
        }
        if (error.hasError(SslError.SSL_UNTRUSTED)) {
            errorCodeMessageCodes.add(R.string.message_certificate_untrusted);
        }
        if (error.hasError(SslError.SSL_INVALID)) {
            errorCodeMessageCodes.add(R.string.message_certificate_invalid);
        }

        return errorCodeMessageCodes;
    }

    @Override
    public void onReceivedSslError(WebView view, @NonNull final SslErrorHandler handler, @NonNull SslError error) {
        List<Integer> errorCodeMessageCodes = getAllSslErrorMessageCodes(error);

        StringBuilder stringBuilder = new StringBuilder();
        for (Integer messageCode : errorCodeMessageCodes) {
            stringBuilder.append(" - ").append(mActivity.getString(messageCode)).append('\n');
        }
        String alertMessage =
                mActivity.getString(R.string.message_insecure_connection, stringBuilder.toString());

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getString(R.string.title_warning));
        builder.setMessage(alertMessage)
                .setCancelable(true)
                .setPositiveButton(mActivity.getString(R.string.action_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                handler.proceed();
                            }
                        })
                .setNegativeButton(mActivity.getString(R.string.action_no),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                handler.cancel();
                            }
                        });
        builder.create().show();
    }

    @Override
    public void onFormResubmission(WebView view, @NonNull final Message dontResend, @NonNull final Message resend) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(mActivity.getString(R.string.title_form_resubmission));
        builder.setMessage(mActivity.getString(R.string.message_form_resubmission))
                .setCancelable(true)
                .setPositiveButton(mActivity.getString(R.string.action_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                resend.sendToTarget();
                            }
                        })
                .setNegativeButton(mActivity.getString(R.string.action_no),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                dontResend.sendToTarget();
                            }
                        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private String getOriginalUA(@NonNull WebView view) {
        // Overridden UA string
        String alreadySetUA = view.getSettings().getUserAgentString();

        // Next call to getUserAgentString() will get us the default
        view.getSettings().setUserAgentString(null);

        // Devise a method for parsing the UA string
        String webViewVersion = view.getSettings().getUserAgentString();

        // Revert to overriden UA string
        view.getSettings().setUserAgentString(alreadySetUA);

        return webViewVersion != null ? webViewVersion : "";
    }

    private boolean usingChrome63(@NonNull WebView view) {
        return getOriginalUA(view).contains("Chrome/63");
    }

    @Override
    public boolean shouldOverrideUrlLoading(@NonNull WebView view, @NonNull String url) {

        Map<String, String> headers = mLightningView.getRequestHeaders();

        if (isGoogleDoc(url) && mPreferences.getOpenPdfInGoogleDocs()) {
            view.loadUrl("http://docs.google.com/viewer?embedded=true&url=" + url, headers);
            return true;
        }
        if (mLightningView.isIncognito() && Utils.doesSupportHeaders()) {
            if (usingChrome63(view)) {
                return false;
            }
            else {
                view.loadUrl(url, headers);
                return true;
            }
        }
        if (url.startsWith("about:") && Utils.doesSupportHeaders()) {
            view.loadUrl(url, headers);
            return true;
        }
        if (url.startsWith("mailto:")) {
            MailTo mailTo = MailTo.parse(url);
            Intent i = Utils.newEmailIntent(mailTo.getTo(), mailTo.getSubject(),
                    mailTo.getBody(), mailTo.getCc());
            mActivity.startActivity(i);
            view.reload();
            return true;
        } else if (url.startsWith("intent://")) {
            Intent intent;
            try {
                intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME);
            } catch (URISyntaxException ignored) {
                intent = null;
            }
            if (intent != null) {
                intent.addCategory(Intent.CATEGORY_BROWSABLE);
                intent.setComponent(null);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                    intent.setSelector(null);
                }
                try {
                    mActivity.startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Log.e(Constants.TAG, "ActivityNotFoundException");
                }
                return true;
            }
        }

        if (!mIntentUtils.startActivityForUrl(view, url) && Utils.doesSupportHeaders()) {
            view.loadUrl(url, headers);
        }
        return Utils.doesSupportHeaders() || super.shouldOverrideUrlLoading(view, url);
    }
}
