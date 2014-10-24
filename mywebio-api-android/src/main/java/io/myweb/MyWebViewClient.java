package io.myweb;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.HttpAuthHandler;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.myweb.http.Headers;
import io.myweb.http.HttpException;
import io.myweb.http.Request;
import io.myweb.http.Response;

public class MyWebViewClient extends WebViewClient {
	private static final String LOG_TAG = MyWebViewClient.class.getName();
	private static final String PREFIX = "http://myweb/";
	private final WebContext webContext;
	private final String root;
	private WebViewClient delegate = new WebViewClient();
	private CookieManager cookieManager;

	public MyWebViewClient(WebContext ec, String packageName) {
		super();
		webContext = ec;
		root = PREFIX + packageName;
	}

	public String getRootUrl() {
		return root + "/";
	}

	public CookieManager getCookieManager(Context ctx) {
		if (cookieManager == null) {
			CookieSyncManager.createInstance(ctx);
			cookieManager = CookieManager.getInstance();
			cookieManager.removeAllCookie();
		}
		return cookieManager;
	}

	private WebResourceResponse createWebResponse(Response response) {
		String contentType = response.getContentType();
		int idx = contentType.indexOf(";");
		if (idx > 0) contentType = contentType.substring(0, idx);
		return new WebResourceResponse(contentType,	"utf-8",
				response.getBodyAsInputStream());
	}

	@Override
	public WebResourceResponse shouldInterceptRequest(final WebView view, final String url) {
//		Log.d(LOG_TAG, "Url: " + url);
		if (url.startsWith(root) && webContext != null) {
			CookieManager cm = getCookieManager(view.getContext());
			Request request = new Request(url.substring(root.length()));

			// Issue #65786 KitKat 4.2 (calling getCookie() on IO thread blocks WebView)
			// normally we would write: String cookies = cm.getCookie(url);
			// but this has to be called from the main thread in KitKat
			// -----------------------------------------------------------
			Handler handler = new Handler(Looper.getMainLooper());
			final CountDownLatch latch = new CountDownLatch(1);
			final AtomicReference<String> cookies = new AtomicReference<String>();
			handler.post( new Runnable() {
				@Override
				public void run() {
					cookies.set(CookieManager.getInstance().getCookie(url));
					latch.countDown();
				}
			});

			try {
				latch.await();
			} catch (InterruptedException e) {}
			// -----------------------------------------------------------

			request.getHeaders().update(Headers.REQUEST.COOKIE, cookies.get());
			try {
//				Log.d(LOG_TAG, "Request: "+request.getURI().toString());
				final Response response = webContext.getRequestProcessor().processRequest(request);
//				Log.d(LOG_TAG, "Response: "+response.toString());
				handler.post( new Runnable() {
					@Override
					public void run() {
						CookieManager cm = getCookieManager(view.getContext());
						for (Headers.Header h: response.getHeaders().findAll(Headers.RESPONSE.SET_COOKIE)) {
							cm.setCookie(h.getName(), h.getValue());
						}
						CookieSyncManager.getInstance().sync();
					}
				});

				return createWebResponse(response.withIdentity());
			} catch (IOException e) {
				Log.e(LOG_TAG, e.getMessage(), e);
			} catch (HttpException e) {
				Log.e(LOG_TAG, e.getMessage(), e);
			}
		}
		return delegate.shouldInterceptRequest(view, url);
	}

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		return delegate.shouldOverrideUrlLoading(view, url);
	}

	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		delegate.onPageStarted(view, url, favicon);
	}

	@Override
	public void onPageFinished(WebView view, String url) {
		delegate.onPageFinished(view, url);
	}

	@Override
	public void onLoadResource(WebView view, String url) {
		delegate.onLoadResource(view, url);
	}

	@Override
	public void onTooManyRedirects(WebView view, Message cancelMsg, Message continueMsg) {
		delegate.onTooManyRedirects(view, cancelMsg, continueMsg);
	}

	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
		delegate.onReceivedError(view, errorCode, description, failingUrl);
	}

	@Override
	public void onFormResubmission(WebView view, Message dontResend, Message resend) {
		delegate.onFormResubmission(view, dontResend, resend);
	}

	@Override
	public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
		delegate.doUpdateVisitedHistory(view, url, isReload);
	}

	@Override
	public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
		delegate.onReceivedSslError(view, handler, error);
	}

	@Override
	public void onReceivedHttpAuthRequest(WebView view, HttpAuthHandler handler, String host, String realm) {
		delegate.onReceivedHttpAuthRequest(view, handler, host, realm);
	}

	@Override
	public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
		return delegate.shouldOverrideKeyEvent(view, event);
	}

	@Override
	public void onUnhandledKeyEvent(WebView view, KeyEvent event) {
		delegate.onUnhandledKeyEvent(view, event);
	}

	@Override
	public void onScaleChanged(WebView view, float oldScale, float newScale) {
		delegate.onScaleChanged(view, oldScale, newScale);
	}

	@Override
	public void onReceivedLoginRequest(WebView view, String realm, String account, String args) {
		delegate.onReceivedLoginRequest(view, realm, account, args);
	}

	public void setWebViewClient(WebViewClient client) {
		if (client != null) delegate = client;
	}
}
