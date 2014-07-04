package io.myweb.examples;

import android.content.Context;
import io.myweb.api.GET;
import io.myweb.api.Produces;

import java.io.IOException;
import java.io.InputStream;

public class Ex3 {

	@GET("/ex3/:filename")
	public InputStream file(Context ctx, String filename) throws IOException {
		return ctx.getAssets().open(filename);
	}
}
