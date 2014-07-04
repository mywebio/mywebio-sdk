package io.myweb.processor.model;

public class ParsedParam {

	private final int id;
	private final String typeName;
	private final String name;

	public ParsedParam(int id, String type, String name) throws ClassNotFoundException {
		this.id = id;
		this.typeName = type;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public int getId() {
		return id;
	}

	public String getTypeName() {
		return typeName;
	}
}
