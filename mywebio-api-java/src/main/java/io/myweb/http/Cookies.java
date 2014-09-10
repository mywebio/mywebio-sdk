package io.myweb.http;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cookies {

	private Map<String, Cookie> cookies = new HashMap<String, Cookie>();

	public Cookies(Map<String, Cookie> cookies) {
		this.cookies = cookies;
	}

	public Cookie getCookie(String name) {
		return cookies.get(name);
	}

    public Collection<Cookie> all() {
        return cookies.values();
    }

    public void setCookie(Cookie cookie) {
        cookies.put(cookie.getName(), cookie);
    }

    public static Cookies parse(Headers headers) {
        List<Headers.Header> cookieHeaders = headers.findAll(Headers.REQUEST.COOKIE);
        Map<String, Cookie> cookieMap = new HashMap<String, Cookie>();
        for (Headers.Header cookieHeader : cookieHeaders) {
            for(String cookieStr: cookieHeader.getValue().split(";")) {
                Cookie cookie = Cookie.parse(cookieStr.trim());
                cookieMap.put(cookie.getName(), cookie);
            }
        }
        return new Cookies(cookieMap);
    }

}
