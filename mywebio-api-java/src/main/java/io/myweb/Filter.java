package io.myweb;

import io.myweb.http.Request;
import io.myweb.http.Response;

public class Filter {

	public boolean matchBefore(String uri) {
		return false;
	}

	public boolean matchAfter(String uri) {
		return false;
	}

	public Request before(Request request) {
		return request;
	}

	public Response after(Response response) {
		return response;
	}
}
