package io.myweb.api;

public class Cookie {

	private String name;
	private String value;
	private int maxAge;
	private String domain;
	private boolean secure;
	private boolean httpOnly;

	public Cookie(String name, String value, int maxAge, String domain, boolean secure, boolean httpOnly) {
		this.name = name;
		this.value = value;
		this.maxAge = maxAge;
		this.domain = domain;
		this.secure = secure;
		this.httpOnly = httpOnly;
	}

	public Cookie(String name, String value) {
		this(name, value, -1, null, false, true);
	}

	public String getValue() {
		return value;
	}

	public String getName() {
		return name;
	}
}
