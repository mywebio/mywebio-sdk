package io.myweb.examples;

import io.myweb.api.*;

public class Ex7 {

	@PUT("/put2")
	public void put(HttpRequestBody body) {}

	@PUT("/put3")
	public void put(HttpRequest request) {}

//	@GET("/headers")
	public void headers(HttpRequestHeaders headers) {}

//	@GET("/response")
	public HttpResponse response() {
		return HttpResponse.create().ok();
	}
}
