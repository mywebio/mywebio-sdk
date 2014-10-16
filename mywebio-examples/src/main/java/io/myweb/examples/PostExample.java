package io.myweb.examples;

import io.myweb.api.POST;
import io.myweb.http.HttpBadRequestException;
import io.myweb.http.Request;
import io.myweb.http.Response;

public class PostExample {

	@POST("/echo")
	public Response echo(Request request) {
		if (request.getBodyAsJSON()==null) throw new HttpBadRequestException("JSON expected!");
		return Response.ok().withBody(request.getBodyAsJSON());
	}
}
