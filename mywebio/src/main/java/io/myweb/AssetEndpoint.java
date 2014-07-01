package io.myweb;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.LocalSocket;
import android.util.Log;
import io.myweb.api.MimeTypes;

import java.io.*;
import java.util.regex.Pattern;

public class AssetEndpoint extends Endpoint {

	private Context ctx;

	public AssetEndpoint(Context ctx) {
		this.ctx = ctx;
	}

	public String httpMethod() {
		return "GET";
	}

	public String originalPath() {
		return "/";
	}

	public Pattern matcher() {
		return null;
	}

	@Override
	public boolean match(String method, String uri) {
		Log.d("AssetEndpoint", "trying to match: " + uri);
		if ("GET".equals(method)) {
			AssetManager assetManager = ctx.getAssets();
			try {
				assetManager.open("webio" + uri).close();
				Log.d("AssetEndpoint", "matched: " + uri);
				return true;
			} catch (IOException e) {
				Log.d("AssetEndpoint", "not matched: " + uri + " (" + e + ")");
				return false;
			}
		}
		return false;
	}

	public FormalParam[] formalParams() {
		return new FormalParam[0];
	}

	public ActualParam[] actualParams(String uri, String request) {
		return new ActualParam[0];
	}

	@Override
	public void invoke(String uri, String request, LocalSocket localSocket, String reqId) {
		AssetManager assetManager = ctx.getAssets();
		try {
			String contentType = MimeTypes.getMimeType(uri);
			OutputStream os = new BufferedOutputStream(localSocket.getOutputStream(), 32 * 1024);
			writeResponseHeaders(os, reqId);
			InputStream is = assetManager.open("webio" + uri);
			ResponseBuilder responseBuilder = new ResponseBuilder();
			responseBuilder.writeResponse(contentType, is, os);
		} catch (IOException e) {
			Log.e("AssetEndpoint", "error during invoke", e);
		}
	}

}
