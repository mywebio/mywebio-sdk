package io.myweb.processor.model;

public class ParsedParam {

	private final int id;
	private final Class<?> clazz;
	private final String typeName;
	private final String name;

	public ParsedParam(int id, String type, String name) throws ClassNotFoundException {
		this.id = id;
		this.typeName = type;
		this.clazz = convertToClass(type);
		this.name = name;
	}

	private Class<?> convertToClass(String type) throws ClassNotFoundException {
		// TODO support simple types
		return Class.forName(type);
	}

	public Class<?> getClazz() {
		return clazz;
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
