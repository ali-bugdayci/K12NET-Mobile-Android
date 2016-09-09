package com.atlas.k12net;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import com.atlas.k12net.async_tasks.AsistoAsyncCompleteListener;
import com.atlas.k12net.async_tasks.AsistoAsyncTask;
import com.atlas.k12net.utils.userSelection.AsistoUserReferences;
import com.atlas.k12net.utils.webConnection.AsistoHttpClient;

import org.apache.http.cookie.Cookie;

import java.util.List;

public class WebViewerActivity extends AsistoActivity implements AsistoAsyncCompleteListener {

	public static String startUrl = "";
    WebView webview = null;

    @Override
	protected AsistoAsyncTask getAsyncTask() {
		return null;
	}

	@Override
	protected int getToolbarIcon() {
		return R.drawable.asisto_icon;
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
            }

        });

        List<Cookie> cookies = AsistoHttpClient.getCookieList();
        Cookie sessionInfo = null;

        if (! cookies.isEmpty()){
            CookieSyncManager.createInstance(getApplicationContext());
            CookieManager cookieManager = CookieManager.getInstance();

            for(Cookie cookie : cookies){
                sessionInfo = cookie;
                String cookieString = sessionInfo.getName() + "=" + sessionInfo.getValue() + "; domain=" + sessionInfo.getDomain();
                cookieManager.setCookie(AsistoUserReferences.getConnectionAddress(), cookieString);
                CookieSyncManager.getInstance().sync();
            }

            String cookieString = "UICulture" + "=" + webview.getContext().getString(R.string.localString) + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(AsistoUserReferences.getConnectionAddress(), cookieString);

            cookieString = "Culture" + "=" + webview.getContext().getString(R.string.localString) + "; domain=" + sessionInfo.getDomain();
            cookieManager.setCookie(AsistoUserReferences.getConnectionAddress(), cookieString);
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
        webview.loadUrl(startUrl);
		mainLayout.removeAllViews();
		mainLayout.addView(webview);		
		
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
