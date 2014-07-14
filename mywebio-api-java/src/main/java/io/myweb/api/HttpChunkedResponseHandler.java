package io.myweb.api;

public interface HttpChunkedResponseHandler {

	void onStart(ChunkedResponder responder);

	void onNextChunk(ChunkedResponder responder);
}
