package io.myweb.api;

import org.json.JSONObject;

public interface ChunkedResponder {

	void sendChunk(byte[] bytes);

	void sendChunk(String string);

	void sendChunk(JSONObject json);

	void end();
}
