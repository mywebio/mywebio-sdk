package io.myweb.api;

import org.json.JSONObject;

import java.io.InputStream;

public class HttpResponse {

	private String mimeType;

	private Object body;

	private int statusCode;

	private long contentLength;

	private HttpResponse() {}

	public static HttpResponse create() {
		return new HttpResponse();
	}

	public HttpResponse ok() {
		return withStatusCode(200);
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

	public HttpResponse withStatusCode(int code) {
		this.statusCode = code;
		return this;
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
		return this;
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
