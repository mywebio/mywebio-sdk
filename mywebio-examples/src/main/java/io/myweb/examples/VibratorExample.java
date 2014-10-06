package io.myweb.examples;

import android.content.Context;
import android.os.Vibrator;

import org.json.JSONArray;
import org.json.JSONException;

import io.myweb.api.GET;
import io.myweb.http.Response;

public class VibratorExample {
	@GET("/vibrator/*toggle?:pattern=[500,1000]&:repeat=0")
	public Response vibrator(Context context, String toggle, JSONArray pattern, int repeat) throws JSONException {
		Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		if (!v.hasVibrator()) return Response.serviceUnavailable();
		if (toggle.equals("on")) {
			v.vibrate(jsonArrayToLongArray(pattern), repeat);
		} else if (toggle.equals("off")) {
			v.cancel();
		} else return Response.notFound();
		return Response.ok();
	}

	private long[] jsonArrayToLongArray(JSONArray ja) throws JSONException {
		long[] la = new long[ja.length()];
		for (int i=0; i<la.length; i++) {
			la[i] = ja.getLong(i);
		}
		return la;
	}
}
