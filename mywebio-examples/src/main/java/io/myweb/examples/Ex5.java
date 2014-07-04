package io.myweb.examples;

import io.myweb.api.GET;
import io.myweb.api.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

public class Ex5 {

	@GET("/ex5/:name/:id")
	public HttpResponse multiparams(String name, int id) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", name);
		result.put("id", id);
		return HttpResponse.ok().withBody(result);
	}
}
