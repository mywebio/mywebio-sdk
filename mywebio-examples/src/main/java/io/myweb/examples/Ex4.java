package io.myweb.examples;

public class Ex4 {

//	@GET("/:id/cos")
	public String paramsFromUrl(int id) {
		return "cos" + id;
	}

	// http://localhost/eu.javart.androidwebmodule/111/cos OK
	// http://localhost/eu.javart.androidwebmodule/string/cos ERROR
}
