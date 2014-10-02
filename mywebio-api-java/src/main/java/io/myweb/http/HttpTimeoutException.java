package io.myweb.http;

public class HttpTimeoutException extends HttpException {
	private static final StatusCode mStatus = StatusCode.TIMEOUT;

	public HttpTimeoutException() {
		super(mStatus);
	}

	public HttpTimeoutException(String message) {
		super(mStatus, message);
	}

	public HttpTimeoutException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
