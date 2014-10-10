package io.myweb.http;

public class HttpException extends RuntimeException {
	private final StatusCode statusCode;

	public HttpException(StatusCode statusCode) {
		this(statusCode, statusCode.getMessage());
	}

	public HttpException(StatusCode statusCode, String message) {
		super(message);
		this.statusCode = statusCode;
	}

	public HttpException(StatusCode statusCode, String message, Throwable cause) {
		super(message, cause);
		this.statusCode = statusCode;
	}

	public StatusCode getStatusCode() {
		return statusCode;
	}
}
