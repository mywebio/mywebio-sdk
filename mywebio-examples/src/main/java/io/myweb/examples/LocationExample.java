package io.myweb.examples;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;

import org.json.JSONException;
import org.json.JSONObject;

import io.myweb.api.GET;

public class LocationExample {
	private LocationManager lm;

	@GET("/location/*provider")
	public JSONObject location(Context context, String provider) throws JSONException {
		lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (provider.length()==0) {
			JSONObject loc = new JSONObject();
			for (String p : lm.getAllProviders()) {
				loc.put(p, getLocationFromProvider(p));
			}
			return loc;
		}
		return getLocationFromProvider(provider);
	}

	private JSONObject getLocationFromProvider(String provider) throws JSONException {
		Location l = lm.getLastKnownLocation(provider);
		JSONObject jl = new JSONObject();
		if (l!=null) {
			jl.put("provider", provider);
			jl.put("longitude", l.getLongitude());
			jl.put("latitude", l.getLongitude());
			jl.put("time", l.getTime());
			if (l.hasAccuracy()) jl.put("accuracy", l.getAccuracy());
			if (l.hasAltitude()) jl.put("altitude", l.getAltitude());
			if (l.hasBearing()) jl.put("bearing", l.getBearing());
			if (l.hasSpeed()) jl.put("speed", l.getSpeed());
		}
		return jl;
	}
}
