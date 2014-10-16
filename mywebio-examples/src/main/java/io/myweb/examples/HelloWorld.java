package io.myweb.examples;

import io.myweb.api.GET;

public class HelloWorld {

	@GET("/hello")
	public String hello() {
		return "Hello, world!";
	}
}
