package com.k12nt.k12netframe;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.Toast;

import com.k12nt.k12netframe.async_tasks.K12NetAsyncCompleteListener;
import com.k12nt.k12netframe.async_tasks.AsistoAsyncTask;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.apache.http.cookie.Cookie;

import java.io.File;
import java.util.List;

import me.leolin.shortcutbadger.ShortcutBadger;

public class WebViewerActivity extends K12NetActivity implements K12NetAsyncCompleteListener {

	public static String startUrl = "";
    public static Context ctx = null;
    WebView webview = null;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFileUploadCallbackSecond;
    private final static int FILECHOOSER_RESULTCODE=1;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        if(requestCode==FILECHOOSER_RESULTCODE)
        {
            if (null == mUploadMessage ) return;
            Uri result = intent == null || resultCode != RESULT_OK ? null
                    : intent.getData();

            if (null == result ) return;
            String wholeID = DocumentsContract.getDocumentId(result);

// Split at colon, use second item in the array
            String id = wholeID.split(":")[1];

            String[] column = { MediaStore.Images.Media.DATA };

// where id is equal to
            String sel = MediaStore.Images.Media._ID + "=?";

            Cursor cursor = getContentResolver().
                    query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            column, sel, new String[]{ id }, null);

            String filePath = "";

            int columnIndex = cursor.getColumnIndex(column[0]);

            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex);
            }

            File file = new File(filePath);

            Uri uri = Uri.fromFile(file);

            mUploadMessage.onReceiveValue(uri);
            mUploadMessage = null;
        }
    }

    @Override
	protected AsistoAsyncTask getAsyncTask() {
		return null;
	}

	@Override
	protected int getToolbarIcon() {
		return R.drawable.k12net_logo;
	}

	@Override
	protected int getToolbarTitle() {
		return R.string.webViewer;
	}

	@Override
	public void asyncTaskCompleted() {

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

        K12NetUserReferences.resetBadgeNumber();
        ShortcutBadger.applyCount(this, K12NetUserReferences.getBadgeCount());

		webview = new WebView(WebViewerActivity.this);
		webview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		webview.setWebViewClient(new WebViewClient(){

            public void onPageFinished(WebView view, String url) {
                Log.i("WEB", "Finished loading URL: " + url);
                if (url.toLowerCase().contains("login.aspx")) {
                    finish();
                }
                if (url.toLowerCase().contains("logout.aspx")) {
                    finish();
                }
                startUrl = url;
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.contains("tel:")) {
                    if (url.startsWith("tel:")) {
                        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
                        startActivity(intent);
                        return true;
                    }
                }
                return false;
            }

        });

        webview.setWebChromeClient(new WebChromeClient()
        {
            //The undocumented magic method override
            //Eclipse will swear at you if you try to put @Override here
            // For Android 3.0+
            public void openFileChooser(ValueCallback<Uri> uploadMsg) {

                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                WebViewerActivity.this.startActivityForResult(Intent.createChooser(i,"File Chooser"), FILECHOOSER_RESULTCODE);

            }

            // For Android 3.0+
            public void openFileChooser( ValueCallback uploadMsg, String acceptType ) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                WebViewerActivity.this.startActivityForResult(
                        Intent.createChooser(i, "File Browser"),
                        FILECHOOSER_RESULTCODE);
            }

            //For Android 4.1
            public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture){
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                WebViewerActivity.this.startActivityForResult( Intent.createChooser( i, "File Chooser" ), WebViewerActivity.FILECHOOSER_RESULTCODE );

            }

            // file upload callback (Android 5.0 (API level 21) -- current) (public method)
            @SuppressWarnings("all")
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                openFileInput(null, filePathCallback);
                return true;
            }

        });

        List<Cookie> cookies = K12NetHttpClient.getCookieList();
        Cookie sessionInfo = null;

        if (! cookies.isEmpty()){
            CookieSyncManager.createInstance(getApplicationContext());
            CookieManager cookieManager = CookieManager.getInstance();

            for(Cookie cookie : cookies){
                sessionInfo = cookie;
                String cookieString = sessionInfo.getName() + "=" + sessionInfo.getValue() + "; domain=" + sessionInfo.getDomain();
                cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);
                CookieSyncManager.getInstance().sync();
            }

            Log.d("LNG",webview.getContext().getString(R.string.localString) );

            String cookieString = "UICulture" + "=" + K12NetUserReferences.getLanguageCode() + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);

            cookieString = "Culture" + "=" + K12NetUserReferences.getLanguageCode() + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);

            cookieString = "AppID" + "=" + K12NetStaticDefinition.ASISTO_ANDROID_APPLICATION_ID + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(K12NetUserReferences.getConnectionAddress(), cookieString);
        }

        View back_button = (View) findViewById(R.id.lyt_back);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        View next_button = (View) findViewById(R.id.lyt_next);
        next_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(webview.canGoForward()) {
                    webview.goForward();
                }
            }
        });

        View refresh_button = (View) findViewById(R.id.lyt_refresh);
        refresh_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                webview.reload();
            }
        });

        webview.getSettings().setJavaScriptEnabled(true);
        webview.getSettings().setBuiltInZoomControls(true);
        webview.getSettings().setDisplayZoomControls(false);
        webview.getSettings().setAllowFileAccess(true);
        webview.getSettings().setLoadWithOverviewMode(true);
        webview.getSettings().setUseWideViewPort(true);
        webview.setDownloadListener(new DownloadListener() {
                                   @Override
                                   public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength){
                                       DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));

                                       request.setDescription("Download file...");
                                       request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
                                       request.allowScanningByMediaScanner();
                                       request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED); //Notify client once download is completed!
                                       request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype));
                                       DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                                       dm.enqueue(request);
                                       Toast.makeText(getApplicationContext(), "Downloading File", Toast.LENGTH_LONG).show();
                                   }
                               });

        // Enable Caching
        enableHTML5AppCache(webview);

        webview.loadUrl(startUrl);
		mainLayout.removeAllViews();
		mainLayout.addView(webview);

        ctx = this;
		
	}

    @SuppressLint("NewApi")
    protected void openFileInput(final ValueCallback<Uri> fileUploadCallbackFirst, final ValueCallback<Uri[]> fileUploadCallbackSecond) {
        if (mUploadMessage != null) {
            mUploadMessage.onReceiveValue(null);
        }
        mUploadMessage = fileUploadCallbackFirst;

        if (mFileUploadCallbackSecond != null) {
            mFileUploadCallbackSecond.onReceiveValue(null);
        }
        mFileUploadCallbackSecond = fileUploadCallbackSecond;

        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
//        i.setType(mUploadableImageFileTypes);

        startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
    }

    private void enableHTML5AppCache(WebView webView) {

        webView.getSettings().setDomStorageEnabled(true);

        // Set cache size to 8 mb by default. should be more than enough
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
            webView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        }

        webView.getSettings().setAppCachePath(getCacheDir().getAbsolutePath());
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);

        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
    }



    @Override
	public void buildCustomView() {

        LinearLayout back_button = (LinearLayout) findViewById(R.id.lyt_back);
        back_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        LinearLayout lyt_toolbar = (LinearLayout) findViewById(R.id.lyt_option_buttons);

      /*  ImageView image_view = (ImageView) inflater.inflate(R.layout.asisto_toolbar_button_layout, null);
        image_view.setImageResource(R.drawable.false_icon_white);
        lyt_toolbar.addView(image_view);

        image_view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });*/
	}

    @Override
    public void onBackPressed() {
        if(webview.canGoBack()) {
            webview.goBack();
        }
        else {
            super.onBackPressed();
            finish();
        }
    }





}
