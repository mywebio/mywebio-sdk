package io.myweb;

public class FormalParam {

	private final int id;

	private final String typeName;

	private final String name;

	public FormalParam(int id, String typeName, String name) {
		this.id = id;
		this.typeName = typeName;
		this.name = name;
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getTypeName() {
		return typeName;
	}
}
