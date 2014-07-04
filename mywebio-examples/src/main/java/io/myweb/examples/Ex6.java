package io.myweb.examples;

import io.myweb.api.GET;
import io.myweb.api.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;
import io.myweb.api.HttpRequest;

public class Ex6 {

	@GET("/ex6/:name/:id?:start=0&:end=10")
	public HttpResponse multiparamsAndQueryString(String name, int id, int start, int end) throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", name);
		result.put("id", id);
		result.put("start", start);
		result.put("end", end);
		return HttpResponse.ok().withBody(result);
	}
}
