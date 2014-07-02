package io.myweb.examples;

import android.content.Context;
import io.myweb.api.GET;
import io.myweb.api.Produces;

import java.io.*;

//@Path("/root-path")
public class Ex1 {

	@GET("/cos/:id")
	public String cos(String id, Context ctx) {
		return "cos" + id;
	}

	@GET("/img/1.jpeg")
	@Produces("image/jpeg")
	public InputStream inputStream(Context ctx) throws IOException {
		return ctx.getAssets().open("webio/thumbs/IMG_20140503_073604.jpg");
	}

	@GET("/")
	public String slash() {
		return "served from @GET(\"/\")";
	}

	@GET("/multi/:id/:name/get")
	public String multiparams(String name, String id) {
		return "name=" + name + " id=" + id;
	}
}
