package io.myweb.processor.model;

public class ServiceParam {
	private final String parameterName;
	private final String componentName;

	public ServiceParam(String parameterName, String componentName) {
		this.parameterName = parameterName;
		this.componentName = componentName;
	}

	public String getParameterName() {
		return parameterName;
	}

	public String getComponentName() {
		return componentName;
	}

	public boolean isRemote() {
		return componentName.contains("/");
	}
}
