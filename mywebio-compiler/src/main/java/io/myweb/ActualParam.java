package io.myweb;

public class ActualParam {

	private final Class<?> clazz;

	private final Object val;

	public ActualParam(Class<?> clazz, Object val) {
		this.clazz = clazz;
		this.val = val;
	}

	public Class<?> getClazz() {
		return clazz;
	}

	public Object getVal() {
		return val;
	}
}
