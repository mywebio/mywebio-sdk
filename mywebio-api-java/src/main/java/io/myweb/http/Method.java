package io.myweb.http;

public enum Method {
	OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT;

	public static Method findByName(String name) {
		for (Method method : Method.values()) {
			if (method.toString().equals(name)) return method;
		}
		return null;
	}
}
