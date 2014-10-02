package io.myweb.http;

public class HttpInternalErrorException extends HttpException {
	private static final StatusCode mStatus = StatusCode.INTERNAL_ERROR;

	public HttpInternalErrorException() {
		super(mStatus);
	}

	public HttpInternalErrorException(String message) {
		super(mStatus, message);
	}

	public HttpInternalErrorException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
