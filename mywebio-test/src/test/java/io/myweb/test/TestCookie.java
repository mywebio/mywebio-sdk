package io.myweb.test;

import io.myweb.api.Cookie;
import io.myweb.api.Cookies;
import io.myweb.api.GET;
import io.myweb.api.Response;

public class TestCookie {

	@GET("/setCookie?:name=name&:value=value")
	public Response setCookie(String name, String value) {
		return Response.ok().withCookies(new Cookie(name, value, 24 * 3600, "localhost", false, true));
	}

	@GET("/acceptCookie?:cookieName=name")
	public String acceptCookie(String cookieName, Cookie COOKIE_NAME, Cookies cookies) {
		return cookies.getCookie(cookieName).getValue() + COOKIE_NAME.getValue();
	}
}
