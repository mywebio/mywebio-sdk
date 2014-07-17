package io.myweb.api;

import org.json.JSONObject;

import java.io.InputStream;

public class Response {

	private String mimeType;

	private Object body;

	private int statusCode;

	private long contentLength;

	private Cookie[] cookies;

	private Response(int statusCode) {
		this.statusCode = statusCode;
	}

	private static Response newWithStatusCode(int statusCode) {
		return new Response(statusCode);
	}

	public static Response ok() {
		return Response.newWithStatusCode(200);
	}

	public static Response notFound() {
		return Response.newWithStatusCode(404);
	}

	public String getMimeType() {
		return mimeType;
	}

	public Object getBody() {
		return body;
	}

	public Response withMimeType(String mimeType) {
		this.mimeType = mimeType;
		return this;
	}

	public Response withMimeTypeFromFilename(String filename) {
		return withMimeType(MimeTypes.getMimeType(filename));
	}

	public Response withBody(String s) {
		this.body = s;
		return this;
	}

	public Response withBody(InputStream is) {
		this.body = is;
		return this;
	}

	public Response withBody(JSONObject jsonObject) {
		this.body = jsonObject;
		return withMimeType(MimeTypes.MIME_APPLICATION_JSON);
	}

	public Response withContentLength(long length) {
		this.contentLength = length;
		return this;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public long getContentLength() {
		return contentLength;
	}

	public Cookie[] getCookies() {
		return cookies;
	}

	public Response withCookies(Cookie... cookies) {
		this.cookies = cookies;
		return this;
	}
}
