package io.myweb.test;

import io.myweb.http.Cookie;
import io.myweb.http.Cookies;
import io.myweb.api.GET;
import io.myweb.http.Response;

public class TestCookie {

	@GET("/setCookie?:name=name&:value=value")
	public Response setCookie(String name, String value) {
		return Response.ok().withCookie(new Cookie(name, value + value));
	}

	@GET("/acceptCookie?:cookieName=name")
	public String acceptCookie(String cookieName, Cookie COOKIE_NAME, Cookies cookies) {
		return cookies.getCookie(cookieName).getValue() + COOKIE_NAME.getValue();
	}
}
