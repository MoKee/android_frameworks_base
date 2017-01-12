package android.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebHistoryItem;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.R;

import java.net.URISyntaxException;
import java.util.ArrayList;

/**
 * Created by tianzuohui.
 *
 * @hide
 */
public class SearchActivity extends Activity {

    private static final String TAG = "SearchPopup";

    private static final int REQUEST_SELECT_FILE = 100;
    private ValueCallback<Uri[]> uploadMessage;

    protected Context mContext;
    protected ImageView mGoBack;
    protected ImageView mGoForward;
    protected View mProgess;
    protected WebView mWebView;
    protected View mBrowser;
    protected TextView mTitle;
    protected View mColse;
    protected String mSearchText;
    protected boolean mFirstPage;

    private boolean mHasRetried;
    private boolean mIsReloading;
    private String mUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initContentView();

        setupViews();
        setupWebView();

        if (savedInstanceState == null) {
            loadUrl();
        }

        Display display = ((WindowManager) getSystemService(WINDOW_SERVICE))
                .getDefaultDisplay();
        lockOrientation(display.getRotation());
    }

    protected void loadUrl() {
        mWebView.loadUrl(getOriUrl());
    }

    protected void initContentView() {
        mContext = this;

        setContentView(R.layout.search_activity);
        mTitle = (TextView) findViewById(R.id.search_title);
        mProgess = findViewById(R.id.search_progress);
        mColse = findViewById(R.id.search_close);
        mWebView = (WebView) findViewById(R.id.search_webview);
        mGoBack = (ImageView) findViewById(R.id.go_back);
        mGoForward = (ImageView) findViewById(R.id.go_forward);
        mBrowser = findViewById(R.id.goto_browser);
    }

    protected void setupViews() {
        mFirstPage = true;
        mSearchText = getIntent().getStringExtra(Intent.EXTRA_TEXT);
        mTitle.setText(mSearchText);
        mColse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mGoBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mWebView.canGoBack()) {
                    mWebView.goBack();
                }
            }
        });
        if (mGoForward != null) {
            mGoForward.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mWebView.canGoForward()) {
                        mWebView.goForward();
                    }
                }
            });
        }
        mBrowser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gotoBrowser();
            }
        });
    }

    protected void gotoBrowser() {
        finish();
        String url = mUrl;
        if (TextUtils.isEmpty(url)) {
            final String webViewOriUrl = mWebView.getOriginalUrl();
            if (TextUtils.isEmpty(webViewOriUrl)) {
                url = getOriUrl();
            } else {
                url = webViewOriUrl;
            }
        }
        Uri uri = Uri.parse(url);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setData(uri);
        WebBackForwardList historyList = mWebView.copyBackForwardList();
        int size = historyList == null ? 0 : historyList.getSize();
        if (size > 1) {
            ArrayList<String> history = new ArrayList<String>();
            for (int i = 0; i < size; i++) {
                WebHistoryItem historyItem = historyList.getItemAtIndex(i);
                history.add(historyItem.getUrl());
            }
            intent.putExtra("currentItemIndex", historyList.getCurrentIndex());
            intent.putStringArrayListExtra("shareHistory", history);
        }
        mContext.startActivity(intent);
    }

    protected String getOriUrl() {
        return "https://www.baidu.com/s?wd=" + mSearchText;
    }

    private void lockOrientation(int rotation) {
        int orientation = ActivityInfo.SCREEN_ORIENTATION_BEHIND;
        switch (rotation) {
            case Surface.ROTATION_0:
            case Surface.ROTATION_180:
                orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                break;
            case Surface.ROTATION_90:
                orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                break;
            case Surface.ROTATION_270:
                orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                break;
        }
        setRequestedOrientation(orientation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebView.destroy();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mWebView.restoreState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mWebView.saveState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == REQUEST_SELECT_FILE) {
            if (uploadMessage == null) {
                return;
            }
            uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
            uploadMessage = null;
        }
    }

    private void setupWebView() {
        // init web settings
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setBlockNetworkImage(false);
        settings.setDomStorageEnabled(true);

        settings.supportZoom();
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (mFirstPage) {
                    mGoBack.setEnabled(false);
                    if (mGoForward != null) mGoForward.setEnabled(false);
                } else {
                    mGoBack.setEnabled(mWebView.canGoBack());
                    if (mGoForward != null) mGoForward.setEnabled(mWebView.canGoForward());
                }
                mIsReloading = false;
                mUrl = url;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                if (mFirstPage) {
                    mFirstPage = false;
                    mWebView.goBackOrForward(Integer.MIN_VALUE);
                    mWebView.clearHistory();
                }
                super.onPageFinished(view, url);
                if (!mIsReloading) {
                    mHasRetried = false;
                }
                mGoBack.setEnabled(mWebView.canGoBack());
                if (mGoForward != null) mGoForward.setEnabled(mWebView.canGoForward());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    try {
                        Intent intent = new Intent().parseUri(url, Intent.URI_INTENT_SCHEME);
                        if (intent != null) {
                            view.stopLoading();
                            ResolveInfo info = getPackageManager().resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
                            if (info != null) {
                                startActivity(intent);
                            }
                            return true;
                        }
                    } catch (URISyntaxException e) {
                        Log.e(TAG, "Can't resolve url:", e);
                    }
                }
                return false;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (!mHasRetried) {
                    mHasRetried = true;
                    if (error.getErrorCode() == WebViewClient.ERROR_UNKNOWN) {
                        mIsReloading = true;
                        mWebView.reload();
                    }
                    Log.w(TAG, "Fail to load page, error code=" + error.getErrorCode() + " des=" + error.getDescription());
                } else {
                    super.onReceivedError(view, request, error);
                }
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
        mWebView.setWebChromeClient(new WebChromeClient(){
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                mProgess.setVisibility(newProgress < 100 ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                if (mFirstPage) {
                    mTitle.setText(mSearchText);
                } else {
                    mTitle.setText(title);
                }
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

                Intent intent = fileChooserParams.createIntent();
                try {
                    startActivityForResult(intent, SearchActivity.REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    return false;
                }

                return true;
            }
        });
        mWebView.setDownloadListener(new DownloadListener() {

            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                Uri uri = Uri.parse(url);
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
    }
}
