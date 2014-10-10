package io.myweb.processor.model;

public class ParsedFilter {
	private final String value;

	private final String destClass;
	private final String destMethod;
	private boolean before;
	public ParsedFilter(String value, String destClass, String destMethod, boolean before) {
		this.value = value;
		this.destClass = destClass;
		this.destMethod = destMethod;
		this.before = before;
	}

	public String getDestClassSimple() {
		return getDestClass().substring(getDestClass().lastIndexOf(".") + 1).trim();
	}

	public String getGeneratedClassName() {
		return getDestClassSimple() + "_" + getDestMethod();
	}

	public String getValue() {
		return value;
	}

	public String getDestClass() {
		return destClass;
	}

	public String getDestMethod() {
		return destMethod;
	}

	public boolean isBefore() {
		return before;
	}
}
