package io.myweb.http;

public class HttpNotImplementedException extends HttpException {
	private static final StatusCode mStatus = StatusCode.NOT_IMPLEMENTED;

	public HttpNotImplementedException() {
		super(mStatus);
	}

	public HttpNotImplementedException(String message) {
		super(mStatus, message);
	}

	public HttpNotImplementedException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
