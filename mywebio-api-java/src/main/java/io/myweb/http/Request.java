package io.myweb.http;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

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
	private String stringBody;
	private JSONObject jsonBody;
	private InputStream body;
	private Map<String,String> parameterMap = null;

	private Request(Method method, URI uri, String protocolVersion, Headers headers, Cookies cookies) {
		this.method = method;
		this.uri = uri;
		this.protocolVersion = protocolVersion;
		this.headers = headers;
		this.cookies = cookies;
	}

	public boolean hasBeenProcessed() {
		return (stringBody != null || jsonBody != null);
	}

	public long getContentLenght() {
		if (headers == null) return -1;
		String lenStr = headers.get(Headers.REQUEST.CONTENT_LEN);
		if (lenStr == null) return -1;
		return Long.parseLong(lenStr);
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
			return new ByteArrayInputStream(body.toString().getBytes());
		}
	}

	public String getBodyAsString() {
		if (stringBody != null) return stringBody;
		if (jsonBody !=null) return jsonBody.toString();
		stringBody = inputToString(body, getContentLenght());
		return stringBody;
	}

	public JSONObject getBodyAsJSON() {
		if (jsonBody != null) return jsonBody;
		try {
			if (stringBody != null) return new JSONObject(stringBody);
			jsonBody = new JSONObject(inputToString(body, getContentLenght()));
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

	public Request withBody(InputStream is) {
		if(is.markSupported()) {
			is.mark(BUFFER_LENGTH);
		}
		body = is;
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

	private static String inputToString(final InputStream is, long maxLength) {
		if (maxLength <= 0) return "";
		final char[] buffer = new char[BUFFER_LENGTH];
		final StringBuilder out = new StringBuilder();
		try {
			long totalRead = 0;
			final Reader in = new InputStreamReader(is);
			try {
				while (totalRead < maxLength) {
					long bytesToRead = maxLength - totalRead;
					int len = BUFFER_LENGTH;
					if (len > bytesToRead) len = (int) bytesToRead;
					int bytesRead = in.read(buffer, 0, len);
					if (bytesRead < 0) break;
					out.append(buffer, 0, bytesRead);
					totalRead += bytesRead;
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
