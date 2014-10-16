package io.myweb.http;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

public class Request {
	public static final int BUFFER_LENGTH = 32 * 1024;
	public static final int IN_MEMORY_LIMIT = 2 * 1024 * 1024; // 2 MB
	private final Method method;
	private URI uri;
	private final String protocolVersion;
	private final Headers headers;
	private final Cookies cookies;
	private String stringBody;
	private JSONObject jsonBody;
	private byte[] cachedBody;
	private File fileBody;
	private InputStream body;
	private Map<String,String> parameterMap = null;
	private long contentLength = -1;
	private boolean redirected = false;

	private Request(Method method, URI uri, String protocolVersion, Headers headers, Cookies cookies) {
		this.method = method;
		this.uri = uri;
		this.protocolVersion = protocolVersion;
		this.headers = headers;
		this.cookies = cookies;
	}

	public boolean isCached() {
		return (cachedBody != null);
	}

	public long getContentLenght() {
		if (contentLength < 0 && headers != null) {
			String lenStr = headers.get(Headers.REQUEST.CONTENT_LEN);
			if (lenStr != null) contentLength = Long.parseLong(lenStr);
		}
		return contentLength;
	}

	public boolean isKeptAlive() {
		Headers.Header keepAliveHeader = getHeaders().findFirst(Headers.REQUEST.CONNECTION);
		return keepAliveHeader!=null && "keep-alive".equalsIgnoreCase(keepAliveHeader.getValue());
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

	public InputStream getBody() {
		if (cachedBody != null) {
			return new ByteArrayInputStream(cachedBody);
		}
		// As for now make sure we will give out body as InputStream only once
		// TODO create filter for input stream to monitor how many bytes has been read
		InputStream is = body;
		body = null;
		return is;
	}

	public String getBodyAsString() {
		if (stringBody != null) return stringBody;
		if (jsonBody != null) {
			stringBody = jsonBody.toString();
		} else {
			if (cachedBody != null) stringBody = new String(cachedBody);
		}
		return stringBody;
	}

	public JSONObject getBodyAsJSON() {
		if (jsonBody != null) return jsonBody;
		try {
			if (stringBody != null) {
				jsonBody = new JSONObject(stringBody);
			} else {
				if (cachedBody != null)
					jsonBody = new JSONObject(getBodyAsString());
			}
		} catch (JSONException ex) {
			ex.printStackTrace();
		}
		return jsonBody;
	}

	public String getId() {
		Headers.Header myHeader = getHeaders().findFirst(Headers.X.MYWEB_ID);
		if (myHeader!=null) return myHeader.getValue();
		return null;
	}

	public Request withBody(InputStream is) throws IOException {
		// always read body
		if (getContentLenght() > 0) {
			body = is;
			if (getContentLenght() <= IN_MEMORY_LIMIT) {
				cachedBody = new byte[(int) getContentLenght()];
				readBody(cachedBody);
			}
			body = null; // TODO think how to handle chunked requests
		}
		return this;
	}

	public Request withBody(String s) {
		stringBody = s;
		return this;
	}

	public Request withBody(JSONObject json) {
		jsonBody = json;
		return this;
	}

	public Request withId(String id) {
		if (id != null) getHeaders().update(Headers.X.MYWEB_ID, id);
		return this;
	}

	public Request withRedirection(URI u) {
		if (u != null) {
			getHeaders().update(Headers.REQUEST.ORIGIN, uri.toString());
			redirected = true;
			uri = u;
		} else {
			redirected = false;
		}
		return this;
	}

	public boolean isRedirected() {
		return redirected;
	}

	public Map<String, String> getParameterMap() {
		if (parameterMap == null) {
			if (Method.POST.equals(method)) {
				parameterMap = decodeQueryString(getBodyAsString());
			} else {
				parameterMap = decodeQueryString(queryParams(uri.toString()));
			}
		}
		return parameterMap;
	}

	private static Map<String, String> decodeQueryString(String queryParamsStr) {
		String[] nameAndValues = queryParamsStr.split("&");
		Map<String, String> result = new HashMap<String, String>();
		for (String nameAndVal : nameAndValues) {
			if (!"".equals(nameAndVal)) {
				int idx = nameAndVal.indexOf("=");
				try {
					if(idx>0)
						result.put(nameAndVal.substring(0,idx),
								URLDecoder.decode(nameAndVal.substring(idx + 1), "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	private static String queryParams(String url) {
		int i = url.indexOf("?");
		String queryParams;
		if (i == -1) {
			queryParams = "";
		} else {
			queryParams = url.substring(i + 1);
		}
		return queryParams;
	}

	public static Request parse(String req) throws HttpBadRequestException {
		if (req == null || req.length() == 0) return null;
		String[] lines = req.split("\\r\\n", 2);
		String[] segments = lines[0].split(" ");
		Method method = Method.findByName(segments[0]);
		if (method == null) throw new HttpBadRequestException("Invalid HTTP method: " + segments[0]);
		URI uri = URI.create(segments[1]);
		String protocolVersion = segments[2];
		Headers headers = Headers.parse(lines[1]);
		Cookies cookies = Cookies.parse(headers);
		return new Request(method, uri, protocolVersion, headers, cookies);
	}

	public long readBody() throws IOException {
		return readBody(null);
	}

	public long readBody(final byte[] target) throws IOException {
		long len = getContentLenght();
		if (target != null && target.length < len) len = target.length;
		return readBody(target, len);
	}

	public long readBody(final byte[] target, long maxLength) throws IOException {
		if (maxLength <= 0 || body == null) return -1;
		long totalRead = 0;
		while (totalRead < maxLength) {
			long bytesToRead = maxLength - totalRead;
			int len = BUFFER_LENGTH;
			if (len > bytesToRead) len = (int) bytesToRead;
			if (target != null) {
				int bytesRead = body.read(target, (int) totalRead, len);
				if (bytesRead < 0) break;
				totalRead += bytesRead;
			} else {
				long bytesSkipped = body.skip(maxLength);
				totalRead += bytesSkipped;
			}
		}
		return totalRead;
	}
}
