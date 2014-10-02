package io.myweb;

import android.content.Context;
import android.content.res.AssetManager;

import io.myweb.http.Method;
import io.myweb.http.MimeTypes;
import io.myweb.http.Request;
import io.myweb.http.Response;

import java.io.*;
import java.util.regex.Pattern;

public class AssetEndpoint extends Endpoint {

	public static final String MYWEB_ASSETS_DIR = "myweb";

	public AssetEndpoint(Server srv) {
		super(srv);
	}

	@Override
	protected Method httpMethod() {
		return Method.GET;
	}

	@Override
	protected String originalPath() {
		return "/"; // doesn't matter actually
	}

	@Override
	protected Pattern getPattern() {
		return null; // doesn't matter actually
	}

	@Override
	public boolean match(Method method, String uri) {
//		Log.d("AssetEndpoint", "trying to match: " + uri);
		if (Method.GET == method) {
			AssetManager assetManager = getContext().getAssets();
			try {
				assetManager.open(MYWEB_ASSETS_DIR + uri).close();
//				Log.d("AssetEndpoint", "matched: " + uri);
				return true;
			} catch (IOException e) {
//				Log.d("AssetEndpoint", "not matched: " + uri + " (" + e + ")");
				return false;
			}
		}
		return false;
	}

	@Override
	public void invoke(String uri, Request request, OutputStream os) {
		AssetManager assetManager = getContext().getAssets();
		try {
			ResponseWriter rw = new ResponseWriter(MimeTypes.getMimeType(uri), os);
			InputStream is = assetManager.open(MYWEB_ASSETS_DIR + uri);
			long length = getServer().getAssetLength(uri);
			rw.write(Response.ok().withId(request.getId()).withLength(length).withBody(is));
			rw.close();
		} catch (IOException e) {
//			Log.e("AssetEndpoint", "error during invoke", e);
		}
	}
}