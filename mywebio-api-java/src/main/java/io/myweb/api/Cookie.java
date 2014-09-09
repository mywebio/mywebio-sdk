package io.myweb.api;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Cookie {

	private final String name;
	private final String value;
	private int maxAge;
	private String path;
	private String domain;
	private boolean secure;
	private boolean httpOnly;

	public Cookie(String name, String value, int maxAge, String path, String domain, boolean secure, boolean httpOnly) {
		this.name = name;
		this.value = value;
		this.maxAge = maxAge;
		this.path = path;
		this.domain = domain;
		this.secure = secure;
		this.httpOnly = httpOnly;
	}

	public Cookie(String name, String value) {
		this(name, value, -1, null, null, false, false);
	}

	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public int getMaxAge() {
		return maxAge;
	}

	public String getPath() {
		return path;
	}

	public String getDomain() {
		return domain;
	}

	public boolean isSecure() {
		return secure;
	}

	public boolean isHttpOnly() {
		return httpOnly;
	}

	public void setMaxAge(int maxAge) {
		this.maxAge = maxAge;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setSecure(boolean secure) {
		this.secure = secure;
	}

	public void setHttpOnly(boolean httpOnly) {
		this.httpOnly = httpOnly;
	}

	public static Cookie parse(String cookieStr) {
		String[] nv = cookieStr.split("=");
		if (nv.length == 2) {
			String value = nv[1].trim();
			String decodedVal = null;
			try {
				decodedVal = URLDecoder.decode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			return new Cookie(nv[0].trim(), decodedVal);
		} else {
			throw new RuntimeException("invalid cookie: " + cookieStr);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		try {
			sb.append(name + "=" + URLEncoder.encode(value, "UTF-8"));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		if (maxAge >= 0) sb.append("; Max-Age=" + maxAge);
		if (path != null) sb.append("; Path=" + path);
		if (domain != null) sb.append("; Domain=" + domain);
		if (secure) sb.append("; Secure");
		if (httpOnly) sb.append("; HttpOnly");
		return sb.toString();
	}

}
