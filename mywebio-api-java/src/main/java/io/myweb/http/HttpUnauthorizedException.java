package io.myweb.http;


public class HttpUnauthorizedException extends HttpException {
	private static final StatusCode mStatus = StatusCode.UNAUTHORIZED;

	public HttpUnauthorizedException() {
		super(mStatus);
	}

	public HttpUnauthorizedException(String message) {
		super(mStatus, message);
	}

	public HttpUnauthorizedException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
