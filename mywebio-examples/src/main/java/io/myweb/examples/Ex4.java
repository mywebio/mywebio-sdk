package io.myweb.examples;

import android.content.Context;
import io.myweb.api.GET;
import io.myweb.api.HttpResponse;

import java.io.IOException;
import java.io.InputStream;

public class Ex4 {

	@GET("/ex4/*filename")
	public HttpResponse file(Context ctx, String filename) throws IOException {
		InputStream is = ctx.getAssets().open(filename);
		return HttpResponse.ok()
				.withBody(is)
				.withMimeTypeFromFilename(filename);
	}
}
