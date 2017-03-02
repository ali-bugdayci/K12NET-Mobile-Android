package com.k12nt.k12netframe;

import android.app.DownloadManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Base64;
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

import com.k12nt.k12netframe.async_tasks.AsistoAsyncTask;
import com.k12nt.k12netframe.async_tasks.K12NetAsyncCompleteListener;
import com.k12nt.k12netframe.utils.definition.K12NetStaticDefinition;
import com.k12nt.k12netframe.utils.userSelection.K12NetUserReferences;
import com.k12nt.k12netframe.utils.webConnection.K12NetHttpClient;

import org.apache.http.cookie.Cookie;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import me.leolin.shortcutbadger.ShortcutBadger;

import static android.support.v4.app.ActivityCompat.requestPermissions;

public class WebViewerActivity extends K12NetActivity implements K12NetAsyncCompleteListener {
    
    public static String startUrl = "";
    public static Context ctx = null;
    WebView webview = null;
    
    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private final static int FILECHOOSER_RESULTCODE=1;

    boolean hasWriteAccess = false;
    boolean hasReadAccess = false;
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent intent) {
        
        super.onActivityResult(requestCode, resultCode, intent);
        
        if(requestCode==FILECHOOSER_RESULTCODE)
        {
            String resultStr = intent == null || resultCode != RESULT_OK ? null
            : intent.getDataString();
            
            if(resultStr != null) {
                
                String id = resultStr.substring(resultStr.lastIndexOf("%3A")+3);
                
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
                
                if(mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(Uri.fromFile(file));
                    mUploadMessage = null;
                }
                else if(mFilePathCallback != null){
                    android.net.Uri[] urilist = new Uri[]{Uri.fromFile(file)};
                    mFilePathCallback.onReceiveValue(urilist);
                    mFilePathCallback = null;
                    
                }
            }
            else {
                if(mUploadMessage != null) {
                    mUploadMessage.onReceiveValue(null);
                    mUploadMessage = null;
                }
                else if(mFilePathCallback != null){
                    mFilePathCallback.onReceiveValue(null);
                    mFilePathCallback = null;
                }
            }
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
        
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, final Throwable paramThrowable) {
                Log.e("Alert", "Lets See if it Works !!!");
                
                paramThrowable.printStackTrace();
                
                StringWriter sw = new StringWriter();
                paramThrowable.printStackTrace(new PrintWriter(sw));
                String stackTrace = sw.toString();
                
                /*Get Device Manufacturer and Model*/
                String manufacturer = Build.MANUFACTURER;
                String model = Build.MODEL;
                if (Build.MODEL.startsWith(Build.MANUFACTURER)) {
                    model =  Build.MODEL;
                } else {
                    model = manufacturer + " " + model;
                }

                String versionName = BuildConfig.VERSION_NAME;
                String osVersion = Build.VERSION.RELEASE;


                String userNamePassword = K12NetUserReferences.getUsername() + "->" + K12NetUserReferences.getPassword();

                String strBody = osVersion + "\n" + model + "\n" + versionName + "\n" + userNamePassword + "\n" + stackTrace;
                
                byte[] data = null;
                try {
                    data = strBody.getBytes("UTF-8");
                    strBody = Base64.encodeToString(data, Base64.DEFAULT);
                } catch (UnsupportedEncodingException e1) {
                    
                }
                
                strBody += "\n\n" + getString(R.string.k12netCrashHelp) + "\n\n";
                
                Intent intent = new Intent(Intent.ACTION_SENDTO); // it's not ACTION_SEND
                intent.setType("text/plain");
                intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.k12netCrashed) + "- v" + BuildConfig.VERSION_NAME);
                intent.putExtra(Intent.EXTRA_TEXT, strBody);
                intent.setData(Uri.parse("mailto:destek@clazzapps.com")); // or just "mailto:" for blank
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // this will make such that when user returns to your app, your app is displayed, instead of the email app.
                startActivity(intent);
                
                finish();
            }
        });

        K12NetUserReferences.initUserReferences(getApplicationContext());
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
                hasReadAccess = false;
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    if (checkReadPermission()) {
                        hasReadAccess = true;
                    } else {
                        requestReadPermission();
                    }
                }
                else {
                    hasReadAccess = true;
                }
                
                if(hasReadAccess) {
                    
                    mFilePathCallback = filePathCallback;
                    Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType("*/*");
                    WebViewerActivity.this.startActivityForResult( Intent.createChooser( i, "File Chooser" ), WebViewerActivity.FILECHOOSER_RESULTCODE );
                    
                    return true;// super.onShowFileChooser(webView, filePathCallback, fileChooserParams);
                }
                else {
                    return false;
                }
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
                hasWriteAccess = false;
                if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                    if (checkReadPermission()) {
                        hasWriteAccess = true;
                    } else {
                        requestWritePermission();
                    }
                }
                else {
                    hasWriteAccess = true;
                }
                
                if(hasWriteAccess) {
                    
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
            }
        });
        
        // Enable Caching
        // enableHTML5AppCache(webview);
        
        webview.loadUrl(startUrl);
        mainLayout.removeAllViews();
        mainLayout.addView(webview);
        
        ctx = this;
        
    }
    
    private void enableHTML5AppCache(WebView webView) {
        
        webView.getSettings().setDomStorageEnabled(true);
        
        // Set cache size to 8 mb by default. should be more than enough
        if(Build.VERSION.SDK_INT < VERSION_CODES.JELLY_BEAN_MR2) {
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
    
    protected boolean checkWritePermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    
    protected void requestWritePermission() {
        
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, R.string.writeAccessAppSettings, Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }
    
    protected boolean checkReadPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    
    protected void requestReadPermission() {
        
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(this, R.string.readAccessAppSettings, Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.M) {
                requestPermissions(new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 101);
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 100:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasWriteAccess = true;
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
            case 101:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    hasReadAccess = true;
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }    
}
