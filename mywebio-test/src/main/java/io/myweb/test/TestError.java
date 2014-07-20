package io.myweb.test;

import io.myweb.api.GET;

public class TestError {

	@GET("/path/:id/:name")
	public String error(String id) {
		return "";
	}
}
