package io.myweb.examples;

import io.myweb.api.GET;
import io.myweb.api.Response;
import org.json.JSONException;
import org.json.JSONObject;

public class Ex5 {

	@GET("/ex5/:name/:id")
	public Response multiparams(String name, int id) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", name);
		result.put("id", id);
		return Response.ok().withBody(result);
	}
}
