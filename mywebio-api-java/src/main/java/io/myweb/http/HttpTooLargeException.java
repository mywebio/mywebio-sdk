package io.myweb.http;

public class HttpTooLargeException extends HttpException {
	private static final StatusCode mStatus = StatusCode.TOO_LARGE;

	public HttpTooLargeException() {
		super(mStatus);
	}

	public HttpTooLargeException(String message) {
		super(mStatus, message);
	}

	public HttpTooLargeException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
