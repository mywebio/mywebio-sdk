package io.myweb.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class HttpResponse {

	private String mimeType;

	private InputStream inputStream;

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

	public InputStream getInputStream() {
		return inputStream;
	}

	public HttpResponse withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public HttpResponse withMimeTypeFromFilename(String filename) {
		return withMimeType(MimeTypes.getMimeType(filename));
	}

	public HttpResponse ok(InputStream inputStream) {
		this.inputStream = inputStream;
		return this;
	}

	public HttpResponse withStatusCode(int code) {
		this.statusCode = code;
		return this;
	}

	public HttpResponse withBody(String s) {
		this.inputStream = new ByteArrayInputStream(s.getBytes());
		return this;
	}

	public HttpResponse withBody(InputStream is) {
		this.inputStream = is;
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

	// ok(String)

	//withCustomHeader()
	//withContentLength()
}
