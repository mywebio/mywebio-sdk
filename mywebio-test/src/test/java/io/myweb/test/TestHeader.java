package io.myweb.test;

import com.google.common.base.Joiner;

import io.myweb.http.Cookies;
import io.myweb.api.GET;
import io.myweb.http.Headers;

public class TestHeader {

	@GET("/headertest")
	public String header(Headers headers, Cookies cookies) {
		String host = headers.get(Headers.REQUEST.HOST);
		return host+" "+Joiner.on(" ").join(cookies.all());
	}

}
