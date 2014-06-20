package io.myweb.model;

public class ParsedParam {

	private final int id;
	private final String type;
	private final String name;

	public ParsedParam(int id, String type, String name) {
		this.id = id;
		this.type = type;
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}
}
