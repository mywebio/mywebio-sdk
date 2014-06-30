package io.myweb.examples;

import io.myweb.api.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// always "/" at the beginning
// default Path("/")
@Path("/example")
public class ExampleWebModuleService {


//	@GET("/path/cos")
//	@Produces(ContentType.TEXT_PLAIN) // default when returning String
	public String cos() {
		return "cos";
	}

//	@GET("/path/json")
//	@Produces(ContentType.APPLICATION_JSON) // default when returning JSONObject
	public JSONObject json() throws JSONException {
		return new JSONObject("{count : 1}");
	}

//	@GET("/path/json2")
//	@Produces("application/json")
	public String json2() {
		return "{name : \"John\"}";
	}

//	@GET("/param")
	// /example/param/?param=1
	// /example/param/?error=1 (doesn't work, we expect param named "param")
	// /example/param/?param=a (error, "a" is not an int)
	public String requestParams(int param, HttpRequestHeaders headers) {
		return "cos";
	}

//	@GET("/:param/img")
	// /example/something/img
	// /example/123/img
	public String paramsFromUrl(String param) {
		return "cos";
	}

	@PUT("/put")
	// implicitly expects JSON in request body
	public void put(JSONObject json) {}

	@PUT("/put2")
	public void put(HttpRequestBody body) {}

	@PUT("/put3")
	public void put(HttpRequest request) {}

//	@GET("/headers")
	public void headers(HttpRequestHeaders headers) {}

//	@GET("/response")
	public HttpResponse response() {
		return HttpResponse.ok();
	}

//	@GET("/chunked-response")
//	@Produces(MimeTypes.MIME_TEXT_PLAIN)
	public HttpChunkedResponseHandler chunkedResponse() {
		return new HttpChunkedResponseHandler() {

			private int n = 10;

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

	@WebSocket("/ws")
	public WsHandler webSocket() {
		return new WsHandler() {

			private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

			@Override
			public void onConnected(final WsConnection conn) {
				executor.schedule(new Runnable() {
					@Override
					public void run() {
						conn.push(System.currentTimeMillis());
					}
				}, 1, TimeUnit.SECONDS);
			}

			@Override
			public void onDisconnected(WsConnection conn) {
				executor.shutdown();
			}

			@Override
			public void onMessage(WsConnection conn, Object msg) {
				conn.push(msg); // send message back
			}
		};
	}

//	@GET("/example/assets")
//	@Produces(ContentType.TEXT_PLAIN) // default when returning String
//	public Assets("/webio") cos();

//	@GET("/path/sth")
	private String sth = "sth";
}
