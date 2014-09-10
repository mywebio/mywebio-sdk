package io.myweb.http;

import org.json.JSONObject;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Response {

	private final StatusCode statusCode;

	private Headers headers;

	private Object body;

	private Response(StatusCode statusCode) {
		this.statusCode = statusCode;
	}

	private static Response newWithStatusCode(StatusCode statusCode) {
		return new Response(statusCode);
	}

	public static Response ok() {
		return Response.newWithStatusCode(StatusCode.OK).withKeepAlive();
	}

	public static Response notFound() {
		return Response.newWithStatusCode(StatusCode.NOT_FOUND).withClose();
	}

	public static Response methodNotAllowed() {
		return Response.newWithStatusCode(StatusCode.METHOD_NOT_ALLOWED).withClose();
	}

	public static Response serviceUnavailable() {
		return Response.newWithStatusCode(StatusCode.SERVICE_UNAVAILABLE).withClose();
	}

	public static Response internalError() {
		return Response.newWithStatusCode(StatusCode.INTERNAL_ERROR).withClose();
	}

	public static Response forbidden() {
		return Response.newWithStatusCode(StatusCode.FORBIDDEN);
	}

	public Object getBody() {
		return body;
	}

	public Headers getHeaders() {
		if (headers == null) headers = new Headers();
		return headers;
	}

	public Response withHeader(String name, String value) {
		getHeaders().add(name, value);
		return this;
	}

	public Response withUpdatedHeader(String name, String value) {
		getHeaders().update(name, value);
		return this;
	}

	public Response withKeepAlive() {
		return withUpdatedHeader(Headers.RESPONSE.CONNECTION, "keep-alive");
	}

	public Response withClose() {
		return withUpdatedHeader(Headers.RESPONSE.CONNECTION, "close");
	}

	public Response withCookie(Cookie cookie) {
		return withHeader(Headers.RESPONSE.SET_COOKIE, cookie.toString());
	}

	public Response withCookies(Cookies cookies) {
		for (Cookie cookie : cookies.all()) withCookie(cookie);
		return this;
	}

	public Response withContentType(String contentType) {
		return withUpdatedHeader(Headers.RESPONSE.CONTENT_TYPE, contentType);
	}

	public Response withContentTypeFrom(String filename) {
		return withContentType(MimeTypes.getMimeType(filename));
	}

	public Response withFile(File file) throws FileNotFoundException {
		return withBody(new FileInputStream(file)).withLength(file.length()).withContentType(file.getName());
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
		return withContentType(MimeTypes.MIME_APPLICATION_JSON);
	}

	public Response withLength(long length) {
		return withUpdatedHeader(Headers.RESPONSE.CONTENT_LEN, Long.toString(length));
	}

	public boolean hasLength() {
		return getHeaders().findFirst(Headers.RESPONSE.CONTENT_LEN) != null;
	}

	public Response withChunks() {
		return withUpdatedHeader(Headers.RESPONSE.TRANSFER_ENC, "chunked");
	}

	public StatusCode getStatusCode() {
		return statusCode;
	}

	public String getContentType() {
		return getHeaders().get(Headers.RESPONSE.CONTENT_TYPE);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(statusCode.toString());
		if (headers != null) sb.append(headers.toString());
		sb.append("\r\n");
		return sb.toString();
	}

}
