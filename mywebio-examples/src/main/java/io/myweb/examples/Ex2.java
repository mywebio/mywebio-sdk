package io.myweb.examples;


import android.content.Context;
import org.json.JSONException;
import org.json.JSONObject;

public class Ex2 {

//	@GET("/path/json")
	public JSONObject json(Context context) throws JSONException {
		return new JSONObject("{count : 1}");
	}

//	@GET("/path/json2")
//	@Produces("application/json")
	public String json2() {
		return "{name : \"John\"}";
	}

	// http://localhost/eu.javart.androidwebmodule/path/json
	// http://localhost/eu.javart.androidwebmodule/path/json2
}
