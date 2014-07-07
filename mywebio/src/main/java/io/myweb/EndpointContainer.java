package io.myweb;

import android.content.Context;

import java.util.LinkedList;
import java.util.List;

public class EndpointContainer {

	public static List<? extends Endpoint> instantiateEndpoints(Context ctx) {
		return new LinkedList<Endpoint>();
	}
}
