package io.myweb.http;

public class HttpMethodNotAllowedException extends HttpException {
	private static final StatusCode mStatus = StatusCode.METHOD_NOT_ALLOWED;

	public HttpMethodNotAllowedException() {
		super(mStatus);
	}

	public HttpMethodNotAllowedException(String message) {
		super(mStatus, message);
	}

	public HttpMethodNotAllowedException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
