package io.myweb.api;

public enum ContentType {
	APPLICATION_JSON("application/json"),
	TEXT_HTML("text/html"),
	TEXT_PLAIN("text/plain"),
	IMAGE_JPEG("image/jpeg");

	private String type;

	private ContentType(String type) {
		this.type = type;
	}
}
