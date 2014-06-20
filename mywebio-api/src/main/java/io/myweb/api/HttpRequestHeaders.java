package io.myweb.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpRequestHeaders {
	private final Map<String, String> headers = new HashMap<String, String>();

	public List<String> getRequestHeader(String name) {
		List<String> result = new ArrayList<String>();
		result.add(headers.get(name));
		return result;
	}
}
