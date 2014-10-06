package io.myweb.processor.model;

import java.util.List;

import io.myweb.http.Method;

public class Provider {
	private final String name;
	private final String className;
	private final List<Method> methods;
	private final String generatedClassName;

	public Provider(String name, String className, List<Method> methods) {
		this.name = name;
		this.className = className;
		this.methods = methods;
		this.generatedClassName = generateClassName();
	}

	private String generateClassName() {
		// Uppercase first letter
		String capitalName = name.substring(0,1).toUpperCase()+name.substring(1);
		// replace all non-literal characters with underscore
		return className+capitalName.replaceAll("[[^a-z]&&[^A-Z]&&[^0-9]&&[^_]]","_");
	}

	public String getName() {
		return name;
	}

	public String getClassName() {
		return className;
	}

	public String getGeneratedClassName() {
		return generatedClassName;
	}

	public boolean hasGetMethod() {
		if (methods == null) return true;
		return methods.contains(Method.GET);
	}

	public boolean hasPostMethod() {
		if (methods == null) return true;
		return methods.contains(Method.POST);
	}

	public boolean hasPutMethod() {
		if (methods == null) return true;
		return methods.contains(Method.PUT);
	}

	public boolean hasDeleteMethod() {
		if (methods == null) return true;
		return methods.contains(Method.DELETE);
	}

}
