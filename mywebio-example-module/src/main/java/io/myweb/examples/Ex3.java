package io.myweb.examples;

public class Ex3 {

//	@GET("/param")
	public String requestParams(int val) {
		return "cos" + val;
	}

	// http://localhost/eu.javart.androidwebmodule/param?val=1
	// http://localhost/eu.javart.androidwebmodule/param?val=a    // HTTP 400

	// http://localhost/eu.javart.androidwebmodule/param?other=1  // HTTP 400
	// http://localhost/eu.javart.androidwebmodule/param?val=1&other=1  // HTTP 200 OK
}
