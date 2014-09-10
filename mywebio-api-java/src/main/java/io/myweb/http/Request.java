package io.myweb.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class Request {
	public static final int BUFFER_LENGTH = 32 * 1024;
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
		if (body instanceof InputStream) {
			InputStream is = (InputStream) body;
			try {
				is.reset();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return is;
		} else {
			return new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.UTF_8));
		}
	}

	public String getBodyAsString() {
		if (body instanceof String) return (String) body;
		else if (body instanceof InputStream) {
			body = inputToString((InputStream) body);
		}
		if (body!=null) return body.toString();
		return null;
	}

	public JSONObject getBodyAsJSON() {
		if (body instanceof JSONObject) return (JSONObject) body;
		else if (body instanceof InputStream) {
			try {
				body = new JSONObject(new JSONTokener(new InputStreamReader((InputStream) body, StandardCharsets.UTF_8)));
				return (JSONObject) body;
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public String getId() {
		return id;
	}

	public Request withBody(InputStream is) {
		if(is.markSupported()) {
			is.mark(BUFFER_LENGTH);
		}
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

	private static String inputToString(final InputStream is) {
		final char[] buffer = new char[BUFFER_LENGTH];
		final StringBuilder out = new StringBuilder();
		try {
			final Reader in = new InputStreamReader(is, StandardCharsets.UTF_8);
			try {
				while (true) {
					int bytesRead = in.read(buffer, 0, buffer.length);
					if (bytesRead < 0) break;
					out.append(buffer, 0, bytesRead);
				}
			}
			finally {
				in.close();
			}
		}
		catch (IOException ex) {
			ex.printStackTrace();
		}
		return out.toString();
	}
}
