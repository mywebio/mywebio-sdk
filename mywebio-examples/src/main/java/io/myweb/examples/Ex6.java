package io.myweb.examples;

import org.json.JSONException;
import org.json.JSONObject;
import io.myweb.api.HttpRequest;

public class Ex6 {

//	@PUT("/put")
	public void put(JSONObject json, HttpRequest request) throws JSONException {
		String sth = json.getString("sth");
	}

	//$ curl \
	//    -H 'Content-Type: application/json' \
	//    -H 'Accept: application/json' \
	//    -X PUT
	//    -d '{"sth": "val"}'
	//    http://localhost/eu.javart.androidwebmodule/put
}
