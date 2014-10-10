package io.myweb.processor;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
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

	protected void error(String msg, Element ee, AnnotationMirror am) {
		messager.printMessage(Diagnostic.Kind.ERROR, msg, ee, am);
	}

	protected void error(String msg, Element ee) {
		messager.printMessage(Diagnostic.Kind.ERROR, msg, ee);
	}

	protected void warning(String msg) {
		messager.printMessage(Diagnostic.Kind.WARNING, msg);
	}

	protected void warning(String msg, Element ee, AnnotationMirror am) {
		messager.printMessage(Diagnostic.Kind.WARNING, msg, ee, am);
	}
}
