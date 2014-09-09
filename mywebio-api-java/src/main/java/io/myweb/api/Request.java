package io.myweb.api;

import java.io.InputStream;
import java.net.URI;

import org.json.JSONObject;

public class Request {
	private final Method method;
	private final URI uri;
	private final String protocolVersion;
	private final Headers headers;
	private final Cookies cookies;
	private Object body;
	private String id;

	private Request(Method method, URI uri, String protocolVersion, Headers headers, Cookies cookies) {
		this.method = method;
		this.uri = uri;
		this.protocolVersion = protocolVersion;
		this.headers = headers;
		this.cookies = cookies;
	}

	public long getContentLenght() {
		if (headers == null) return -1;
		String lenStr = headers.get(Headers.REQUEST.CONTENT_LEN);
		if (lenStr == null) return -1;
		return Long.parseLong(lenStr);
	}

	public Method getMethod() {
		return method;
	}

	public URI getURI() {
		return uri;
	}

	public String getProtocolVersion() {
		return protocolVersion;
	}

	public Headers getHeaders() {
		return headers;
	}

	public Cookies getCookies() {
		return cookies;
	}

	public InputStream getBodyAsInputStream() {
		if (body instanceof InputStream) return (InputStream) body;
		return null;
	}

	public String getBodyAsString() {
		if (body instanceof String) return (String) body;
		// TODO convert from InputStream
		return null;
	}

	public JSONObject getBodyAsJSON() {
		if (body instanceof JSONObject) return (JSONObject) body;
		return null;
	}

	public String getId() {
		return id;
	}

	public Request withBody(InputStream is) {
		body = is;
		return this;
	}

	public Request withBody(String s) {
		body = s;
		return this;
	}

	public Request withBody(JSONObject json) {
		body = json;
		return this;
	}

	public Request withId(String id) {
		this.id = id;
		return this;
	}

	public static Request parse(String req) {
		if (req == null) return null;
		String[] lines = req.split("\\r\\n", 2);
		String[] segments = lines[0].split(" ");
		Method method = Method.findByName(segments[0]);
		if (method == null) throw new RuntimeException("Invalid HTTP method: " + segments[0]);
		URI uri = URI.create(segments[1]);
		String protocolVersion = segments[2];
		Headers headers = Headers.parse(lines[1]);
		Cookies cookies = Cookies.parse(headers);
		return new Request(method, uri, protocolVersion, headers, cookies);
	}
}
