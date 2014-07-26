package io.myweb.processor;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

public class AnnotationMessagerAware {

	private static final boolean quiet = false;

	private final Messager messager;

	public AnnotationMessagerAware(Messager messager) {
		this.messager = messager;
	}

	protected Messager getMessager() {
		return messager;
	}

	protected void log(String msg) {
		if (!quiet) {
			messager.printMessage(Diagnostic.Kind.NOTE, msg);
		}
	}

	protected void error(String msg) {
		messager.printMessage(Diagnostic.Kind.ERROR, msg);
	}
}
