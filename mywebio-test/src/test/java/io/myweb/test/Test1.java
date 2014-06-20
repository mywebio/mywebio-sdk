package io.myweb.test;

import io.myweb.api.GET;

public class Test1 {

    @GET("/test")
	public String test() {
		return "test 1 result";
	}
}
