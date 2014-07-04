package io.myweb.examples;

import io.myweb.api.GET;

public class Ex1 {

	@GET("/ex1/minget")
	public String minget() {
		return "minimal GET, result as plain/text\n";
	}
}
