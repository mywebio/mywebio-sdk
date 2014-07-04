package io.myweb.api;

public interface HttpRequest {
	HttpRequestHeaders getHttpRequestHeaders();

	HttpRequestBody getHttpRequestBody();
}
