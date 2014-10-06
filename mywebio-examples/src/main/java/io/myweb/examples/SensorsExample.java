package io.myweb.examples;

import android.content.Context;

import com.google.gson.Gson;

import io.myweb.api.BindService;
import io.myweb.api.GET;
import io.myweb.api.Produces;
import io.myweb.http.MimeTypes;

public class SensorsExample {
	private final static String VAL_KEYWORD = "/value";

	@GET("/sensors/*type")
	@Produces(MimeTypes.MIME_APPLICATION_JSON)
	@BindService("SensorService")
	public String sensors(Context context, String type, SensorService sensorService) {
		Gson gson = new Gson();
		if(type.endsWith("/")) type = type.substring(0,type.length()-1);
		boolean value = type.contains(VAL_KEYWORD);
		if (value) type = type.split(VAL_KEYWORD)[0];
		int sensorType = -1;
		try {
			sensorType = Integer.parseInt(type);
		} catch (NumberFormatException e) {
			// ignore
		}
		if (sensorType != -1) {
			if (value) return gson.toJson(sensorService.getLastSensorEvent(sensorType));
			return gson.toJson(sensorService.getDefaultSensor(sensorType));
		}
		return gson.toJson(sensorService.getAvailableSensorTypes());
	}
}
