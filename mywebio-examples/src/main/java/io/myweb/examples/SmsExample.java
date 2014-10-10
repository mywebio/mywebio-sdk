package io.myweb.examples;


import io.myweb.api.After;
import io.myweb.api.Before;
import io.myweb.api.ContentProvider;
import io.myweb.http.Method;
import io.myweb.http.Request;
import io.myweb.http.Response;

@ContentProvider(value = "sms", methods = {Method.GET, Method.POST})
public class SmsExample {

	@Before("/sms.*")
	public Request before(Request r) {
		System.out.println("Before: "+r.getURI().toString());
		return r;
	}

	@After("/sms.*")
	public Response after(Response r) {
		System.out.println("After: "+r.getContentType());
		return r;
	}
}
