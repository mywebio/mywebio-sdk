package io.myweb.api;

import java.util.*;

public class Headers {

	private final Map<String, List<String>> headersMap;

	public Headers(Map<String, List<String>> headersMap) {
		this.headersMap = headersMap;
	}

	public String get(String name) {
		List<String> vals = headersMap.get(name);
		if (vals != null && vals.size() > 0) {
			return vals.get(0);
		}
		return null;
	}

	public List<String> getAll(String name) {
		List<String> vals = headersMap.get(name);
		if (vals == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(vals);
	}

	public Set<String> getKeys() {
		Set<String> keySet = headersMap.keySet();
		return Collections.unmodifiableSet(keySet);
	}
}
