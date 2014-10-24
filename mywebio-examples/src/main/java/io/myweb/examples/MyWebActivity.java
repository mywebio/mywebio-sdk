package io.myweb.examples;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import io.myweb.WebContext;
import io.myweb.LocalService;
import io.myweb.MyWebViewClient;
import io.myweb.Service;

public class MyWebActivity extends Activity implements LocalService.ConnectionListener<WebContext> {
	LocalService.Connection<WebContext> myServiceConnection;
	private WebView webView;
	private MyWebViewClient myWebViewClient;

	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_web);
	    webView = (WebView) findViewById(R.id.webView);
	    webView.getSettings().setLoadWithOverviewMode(true);
	    webView.getSettings().setBuiltInZoomControls(false);
	    webView.getSettings().setJavaScriptEnabled(true);
	    webView.getSettings().setDatabaseEnabled(true);
	    webView.getSettings().setDomStorageEnabled(true);
	    webView.getSettings().setAllowContentAccess(true);
	    webView.getSettings().setAppCacheEnabled(true);
	    myServiceConnection = new LocalService.Connection<WebContext>(this, Service.class).withListener(this);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (myServiceConnection!= null) {
			myServiceConnection.close();
			myServiceConnection = null;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		webView.resumeTimers();
	}

	@Override
	protected void onStop() {
		super.onStop();
		webView.pauseTimers();
	}

	@Override
	public void onServiceConnected(WebContext service) {
		if (myWebViewClient == null) {
			myWebViewClient = new MyWebViewClient(service, getPackageName());
		}
		webView.setWebViewClient(myWebViewClient);
		webView.loadUrl(myWebViewClient.getRootUrl());
	}

	@Override
	public void onServiceDisconnected(WebContext service) {
	}

	@Override
	public void finish() {
		webView.clearHistory();
		webView.clearCache(true);
		webView.loadUrl("about:blank");
		super.finish();
	}
}
