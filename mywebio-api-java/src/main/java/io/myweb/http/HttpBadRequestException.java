package io.myweb.http;

public class HttpBadRequestException extends HttpException {
	private static final StatusCode mStatus = StatusCode.BAD_REQUEST;

	public HttpBadRequestException() {
		super(mStatus);
	}

	public HttpBadRequestException(String message) {
		super(mStatus, message);
	}

	public HttpBadRequestException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
