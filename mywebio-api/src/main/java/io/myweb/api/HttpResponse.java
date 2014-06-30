package io.myweb.api;

import org.json.JSONObject;

import java.io.InputStream;

public class HttpResponse {

	private String mimeType;

	private Object body;

	private int statusCode;

	private long contentLength;

	private HttpResponse(int statusCode) {
		this.statusCode = statusCode;
	}

	private static HttpResponse newWithStatusCode(int statusCode) {
		return new HttpResponse(statusCode);
	}

	public static HttpResponse ok() {
		return HttpResponse.newWithStatusCode(200);
	}

	public static HttpResponse notFound() {
		return HttpResponse.newWithStatusCode(404);
	}

	public String getMimeType() {
		return mimeType;
	}

	public Object getBody() {
		return body;
	}

	public HttpResponse withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public HttpResponse withMimeTypeFromFilename(String filename) {
		return withMimeType(MimeTypes.getMimeType(filename));
	}

	public HttpResponse withBody(String s) {
		this.body = s;
		return this;
	}

	public HttpResponse withBody(InputStream is) {
		this.body = is;
		return this;
	}

	public HttpResponse withBody(JSONObject jsonObject) {
		this.body = jsonObject;
		return withMimeType(MimeTypes.MIME_APPLICATION_JSON);
	}

	public HttpResponse withContentLength(long length) {
		this.contentLength = length;
		return this;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public long getContentLength() {
		return contentLength;
	}
}
