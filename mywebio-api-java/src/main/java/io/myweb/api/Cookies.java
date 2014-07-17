package io.myweb.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cookies {

	private Map<String, Cookie> cookies = new HashMap<String, Cookie>();

	public Cookies(Map<String, Cookie> cookies) {
		this.cookies = Collections.unmodifiableMap(cookies);;
	}

	public Cookie getCookie(String name) {
		return cookies.get(name);
	}
}
