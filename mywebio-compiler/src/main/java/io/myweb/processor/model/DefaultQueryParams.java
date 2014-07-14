package io.myweb.processor.model;

public class DefaultQueryParams {

	private final String name;

	private final String val;

	public DefaultQueryParams(String name, String val) {
		this.name = name;
		this.val = val;
	}

	public String getVal() {
		return val;
	}

	public String getName() {
		return name;
	}
}
