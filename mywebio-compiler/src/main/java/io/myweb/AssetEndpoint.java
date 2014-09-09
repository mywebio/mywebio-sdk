package io.myweb;

import android.content.Context;
import android.content.res.AssetManager;
import android.net.LocalSocket;
import android.util.Log;

import io.myweb.api.Method;
import io.myweb.api.MimeTypes;
import io.myweb.api.Request;
import io.myweb.api.Response;

import java.io.*;
import java.util.regex.Pattern;

public class AssetEndpoint extends Endpoint {

	public static final String MYWEB_ASSETS_DIR = "myweb";

	public AssetEndpoint(Context context) {
		super(context);
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
	public boolean match(String method, String uri) {
		Log.d("AssetEndpoint", "trying to match: " + uri);
		if ("GET".equals(method)) {
			AssetManager assetManager = getContext().getAssets();
			try {
				assetManager.open(MYWEB_ASSETS_DIR + uri).close();
				Log.d("AssetEndpoint", "matched: " + uri);
				return true;
			} catch (IOException e) {
				Log.d("AssetEndpoint", "not matched: " + uri + " (" + e + ")");
				return false;
			}
		}
		return false;
	}

	@Override
	public void invoke(String uri, Request request, LocalSocket localSocket) {
		AssetManager assetManager = getContext().getAssets();
		try {
            ResponseWriter rw = new ResponseWriter(MimeTypes.getMimeType(uri), localSocket);
            // TODO old behaviour
            rw.writeRequestId(request.getId());
            InputStream is = assetManager.open(MYWEB_ASSETS_DIR + uri);
			long length = AssetInfo.getAssetLengths().get(uri);
			rw.write(Response.ok().withLength(length).withBody(is));
		} catch (IOException e) {
			Log.e("AssetEndpoint", "error during invoke", e);
		}
	}
}
