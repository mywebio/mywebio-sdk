package io.myweb.examples;


import android.content.Context;
import io.myweb.api.GET;

import java.io.IOException;
import java.io.InputStream;

public class Ex2 {

	@GET("/ex2/assets.txt")
	public InputStream file(Context ctx) throws IOException {
		return ctx.getAssets().open("assets.txt");
	}
}
