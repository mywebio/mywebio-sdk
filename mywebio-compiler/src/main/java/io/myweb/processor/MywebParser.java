package io.myweb.processor;

import io.myweb.api.*;
import io.myweb.processor.model.ParsedMethod;
import io.myweb.processor.model.ParsedParam;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MywebParser extends AnnotationMessagerAware {

	private final MywebValidator mywebValidator;

	public MywebParser(Messager messager, MywebValidator mywebValidator) {
		super(messager);
		this.mywebValidator = mywebValidator;
	}

	public ParsedMethod parse(ExecutableElement ee) {
		String destClass = ee.getEnclosingElement().toString();
		String destMethod = ee.getSimpleName().toString();
		String destMethodRetType = ee.getReturnType().toString();
		List<ParsedParam> params = new LinkedList<ParsedParam>();
		String httpMethod = "GET";
		String httpUri = "/";
		String produces = MimeTypes.MIME_TEXT_PLAIN;
		List<? extends VariableElement> parameters = ee.getParameters();
		int i = 0;
		for (VariableElement p : parameters) {
			ParsedParam pp = null;
			try {
				String type = p.asType().toString().replaceFirst("class ", "");
				pp = new ParsedParam(i++, type, p.getSimpleName().toString());
			} catch (ClassNotFoundException e) {
				error("cannot load class: " + e.getMessage());
			}
			params.add(pp);
		}
		for (AnnotationMirror am : ee.getAnnotationMirrors()) {
			// TODO verify if annotation aren't duplicated
			String annotationName = am.getAnnotationType().toString();
			if (GET.class.getName().equals(annotationName) || POST.class.getName().equals(annotationName)
					|| DELETE.class.getName().equals(annotationName) || PUT.class.getName().equals(annotationName)) {
				httpMethod = annotationName.substring(annotationName.lastIndexOf(".") + 1).trim();
				for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
					if ("value".equals(entry.getKey().getSimpleName().toString())) {
						httpUri = entry.getValue().getValue().toString();
						mywebValidator.validateAnnotation(httpMethod, destMethodRetType, destMethod, params, httpUri, ee, am);
					}
				}
			}
			if (Produces.class.getName().equals(annotationName)) {
				for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
					if ("value".equals(entry.getKey().getSimpleName().toString())) {
						produces = entry.getValue().getValue().toString();
					}
				}
			}
		}
		return new ParsedMethod(destClass, destMethod, destMethodRetType, params, httpMethod, httpUri, produces);
	}
}
