package io.myweb.http;

public class HttpForbiddenException extends HttpException {
	private static final StatusCode mStatus = StatusCode.FORBIDDEN;

	public HttpForbiddenException() {
		super(mStatus);
	}

	public HttpForbiddenException(String message) {
		super(mStatus, message);
	}

	public HttpForbiddenException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
