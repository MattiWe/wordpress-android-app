package com.leavesified.android.wordpressfeeds;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.leavesified.android.wordpressfeeds.feedservice.FeedService;

public class MainActivity extends AppCompatActivity {
    WebView mWebView = null;

    /*
    * Create Activity
    * Set Fullscreen Webview, allow JS
    *
    * Resources:
    *   R.string.website_url => initial uri at activity creation
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mWebView = new WebView(this);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(mWebView);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new MyWebViewClient());

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Activity activity = this;
        mWebView.setWebChromeClient(new WebChromeClient(){
            public void onProgressChanged(WebView view, int progress){
                activity.setProgress(progress * 1000);
            }
        });

        mWebView.loadUrl(getString(R.string.website_url));
        startService(new Intent(this, FeedService.class));

        onNewIntent(getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        //stopService(new Intent(this, FeedService.class));
    }

    /*
    * handle Android back button
    * if navigation was used: go back to previous page
    * else go back in stack
     */
    @Override
    public void onBackPressed() {
        if(mWebView.canGoBack()) mWebView.goBack();
        else super.onBackPressed();
    }

    /*
    * keep all navigation to same domain in in app
    * open all external uris in external app
    *
    * Resources:
    *   R.string.app_nav_domain
     */
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url){
            if(Uri.parse(url).getHost().endsWith(getString(R.string.app_nav_domain))) {
                return false;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            view.getContext().startActivity(intent);
            return true;
        }
    }

    @Override
    public void onNewIntent(Intent intent){
        Bundle extras = intent.getExtras();
        if(extras != null){
            if(extras.containsKey(FeedService.ULR_FROM_NOTIFICATION_EXTRA)){
                mWebView.loadUrl(extras.getString(FeedService.ULR_FROM_NOTIFICATION_EXTRA));
            }
        }
    }
}
