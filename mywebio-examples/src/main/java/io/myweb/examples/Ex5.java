package io.myweb.examples;

import io.myweb.api.GET;
import io.myweb.api.POST;

public class Ex5 {

	@GET("/:id/cos?:param=p&:aa=0")
//	public String paramsFromUrl(int id, String param, int aa, Context context) {
	public String paramsFromUrl(int id, String param, String aa) {
		return "paramsFromUrl";
	}

	@POST("/post")
	public String post() {
		return "";
	}

	@GET("/:id/cos?:param=p&:aa=0&:d=qwer")
	public String paramsFromUrl2(int id, String param, String aa, String d) {
		return "cos";// + id + param;
	}

	// http://localhost/eu.javart.androidwebmodule/111/cos ERROR
	// http://localhost/eu.javart.androidwebmodule/string/cos ERROR

	// http://localhost/eu.javart.androidwebmodule/111/cos?param=val&aa=11
}
