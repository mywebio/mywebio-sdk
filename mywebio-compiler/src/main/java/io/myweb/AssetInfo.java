package io.myweb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AssetInfo {

	private final static Map<String, Long> assetLengths;

	static {
		Map<String, Long> lengths = new HashMap<String, Long>();
		assetLengths = Collections.unmodifiableMap(lengths);
	}

	public static Map<String, Long> getAssetLengths() {
		return assetLengths;
	}
}
