package io.myweb.http;

public class HttpMovedPermanentlyException extends HttpException {
	private static final StatusCode mStatus = StatusCode.MOVED_PERMANENTLY;

	public HttpMovedPermanentlyException() {
		super(mStatus);
	}

	public HttpMovedPermanentlyException(String message) {
		super(mStatus, message);
	}

	public HttpMovedPermanentlyException(String message, Throwable cause) {
		super(mStatus, message, cause);
	}
}
