package io.myweb.http;

public class HttpNotFoundException extends HttpException {
	private static final StatusCode mStatus = StatusCode.NOT_FOUND;

	public HttpNotFoundException() {
		super(mStatus);
	}

	public HttpNotFoundException(String message) {
		super(mStatus, message);
	}

	public HttpNotFoundException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
