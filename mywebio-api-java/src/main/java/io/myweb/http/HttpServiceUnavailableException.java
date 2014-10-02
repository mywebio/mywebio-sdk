package io.myweb.http;

public class HttpServiceUnavailableException extends HttpException {
	private static final StatusCode mStatus = StatusCode.SERVICE_UNAVAILABLE;

	public HttpServiceUnavailableException() {
		super(mStatus);
	}

	public HttpServiceUnavailableException(String message) {
		super(mStatus, message);
	}

	public HttpServiceUnavailableException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
