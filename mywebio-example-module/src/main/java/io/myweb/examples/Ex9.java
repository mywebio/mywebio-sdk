package io.myweb.examples;

import io.myweb.api.ChunkedResponder;
import io.myweb.api.HttpChunkedResponseHandler;

public class Ex9 {

//	@GET("/chunked-response")
//	@Produces("text/plain")
	public HttpChunkedResponseHandler chunkedResponse() {
		return new HttpChunkedResponseHandler() {

			private int n = 3;

			@Override
			public void onStart(ChunkedResponder responder) {
				responder.sendChunk("chunk");
				n--;
			}

			@Override
			public void onNextChunk(ChunkedResponder responder) {
				if (n > 0) {
					responder.sendChunk("chunk");
					n--;
				} else {
					responder.end();
				}
			}
		};
	}

	/*

	http://localhost/eu.javart.androidwebmodule/chunked-response

	HTTP/1.1 200 OK
    Content-Type: text/plain; charset=utf-8
    Transfer-Encoding: chunked

    5
    chunk
    5
    chunk
    5
    chunk
    0

	*/
}










