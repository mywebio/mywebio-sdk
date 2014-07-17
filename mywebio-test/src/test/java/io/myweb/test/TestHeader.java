package io.myweb.test;

import com.google.common.base.Joiner;
import io.myweb.api.GET;
import io.myweb.api.Headers;

import java.util.List;

public class TestHeader {

	@GET("/headertest")
	public String header(Headers headers) {
		String host = "Host: " + headers.get("Host");
		String firstCookie = "First cookie: " + headers.get("Cookie");
		String allCookies = allCookies(headers.getAll("Cookie"));
		return host + " " + firstCookie + " " + allCookies;
	}

	private String allCookies(List<String> cookies) {
		return "All cookies: " + Joiner.on(", ").join(cookies);
	}
}
