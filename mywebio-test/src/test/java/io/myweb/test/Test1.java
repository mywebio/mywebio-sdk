package io.myweb.test;

import io.myweb.api.After;
import io.myweb.api.Before;
import io.myweb.api.GET;
import io.myweb.http.Request;
import io.myweb.http.Response;

public class Test1 {

	@GET("/test")
	public String test() {
		return "test 1 result";
	}

	@Before("/test")
	public Request precondition(Request r) {
		return r.withId("Aqq");
	}

	@After("/test")
	public Response postcondition(Response r) {
		return r.withKeepAlive();
	}

}
