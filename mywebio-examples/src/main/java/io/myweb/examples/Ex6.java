package io.myweb.examples;

import io.myweb.api.GET;
import io.myweb.api.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class Ex6 {

	@GET("/ex6/:name/:id?:start=0&:end=10")
	public Response multiparamsAndQueryString(String name, int id, int start, int end) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", name);
		result.put("id", id);
		result.put("start", start);
		result.put("end", end);
		return Response.ok().withBody(result);
	}
}
