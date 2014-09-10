package io.myweb.examples;

import android.content.Context;
import io.myweb.api.GET;
import io.myweb.http.Response;

import java.io.IOException;
import java.io.InputStream;

public class Ex4 {

	@GET("/ex4/*filename")
	public Response file(Context ctx, String filename) throws IOException {
		InputStream is = ctx.getAssets().open(filename);
		return Response.ok()
				.withBody(is)
				.withContentTypeFrom(filename);
	}
}
