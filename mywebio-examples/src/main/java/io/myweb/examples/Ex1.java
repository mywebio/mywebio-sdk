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

	@GET("/multi/:id/:id2/:name/get")
	public String multiparams(String name, int id, Integer id2) {
		return "name=" + name + " id=" + id + " id2=" + id2;
	}
}
