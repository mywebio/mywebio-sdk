package io.myweb.processor;

import io.myweb.api.*;
import io.myweb.http.Method;
import io.myweb.http.MimeTypes;
import io.myweb.processor.model.ParsedMethod;
import io.myweb.processor.model.ParsedParam;
import io.myweb.processor.model.ServiceParam;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MyParser extends AnnotationMessagerAware {

	private final MyValidator myValidator;

	public MyParser(Messager messager, MyValidator myValidator) {
		super(messager);
		this.myValidator = myValidator;
	}

	public ParsedMethod parse(ExecutableElement ee) throws Exception {
        List<ParsedParam> parasedParams = extractMethodParams(ee);
        return extractParsedMethod(ee, parasedParams);
	}

    private ParsedMethod extractParsedMethod(ExecutableElement ee, List<ParsedParam> params) throws Exception {
        String destClass = ee.getEnclosingElement().toString();
        String destMethod = ee.getSimpleName().toString();
        String destMethodRetType = ee.getReturnType().toString();
        Method httpMethod = null;
        String httpUri = "/";
        String produces = MimeTypes.MIME_TEXT_PLAIN;
	    ServiceParam service = null;
        List<? extends AnnotationMirror> annotationMirrors = ee.getAnnotationMirrors();
        for (AnnotationMirror am : annotationMirrors) {
	        Method m = extractHttpMethod(am);
	        if (httpMethod==null) {
		        httpMethod = m;
		        httpUri = validateAndExtractHttpUri(am, httpMethod, destMethodRetType, destMethod, params, ee);
	        }
	        else if (m!=null) throw new IllegalArgumentException("Duplicate annotations @"+httpMethod.toString()+" and @"+m.toString()+" for method "+destClass+"."+destMethod+"()");
            produces = extractProducesAnnotation(am);
	        if (service==null) service = validateAndExtractServiceAnnotation(params, am, ee);
        }
        return new ParsedMethod(destClass, destMethod, destMethodRetType, params, httpMethod, httpUri, produces, service);
    }

	private ServiceParam validateAndExtractServiceAnnotation(List<ParsedParam> params, AnnotationMirror am, ExecutableElement ee) {
		ServiceParam service = null;
		String annotationName = am.getAnnotationType().toString();
		if (BindService.class.getName().equals(annotationName)) {
			String value = getAnnotationValue(am, "value");
			service = myValidator.validateBindServiceAnnotation(value, params, ee, am);
		}
		return service;
	}

	private String validateAndExtractHttpUri(AnnotationMirror am, Method httpMethod, String destMethodRetType, String destMethod, List<ParsedParam> params, ExecutableElement ee) {
        String annotationName = am.getAnnotationType().toString();
        String httpUri = "/";
        if (isHttpMethodAnnotation(annotationName)) {
            httpUri = getAnnotationValue (am, "value");
            myValidator.validateAnnotation(httpMethod, destMethodRetType, destMethod, params, httpUri, ee, am);
        }
        return httpUri;
    }

	private String getAnnotationValue(AnnotationMirror am, String name) {
		for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
			if (name.equals(entry.getKey().getSimpleName().toString())) {
				return entry.getValue().getValue().toString();
			}
		}
		return null;
	}

    private boolean isHttpMethodAnnotation(String annotationName) {
        return GET.class.getName().equals(annotationName) || POST.class.getName().equals(annotationName)
                || DELETE.class.getName().equals(annotationName) || PUT.class.getName().equals(annotationName);
    }

    private Method extractHttpMethod(AnnotationMirror am) {
        String annotationName = am.getAnnotationType().toString();
        Method httpMethod = null;
        if (isHttpMethodAnnotation(annotationName)) {
            httpMethod = Method.findByName(annotationName.substring(annotationName.lastIndexOf(".") + 1).trim());
        }
        return httpMethod;
    }

    private String extractProducesAnnotation(AnnotationMirror am) {
        String produces = MimeTypes.MIME_TEXT_HTML;
        String annotationName = am.getAnnotationType().toString();
        if (Produces.class.getName().equals(annotationName)) {
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                if ("value".equals(entry.getKey().getSimpleName().toString())) {
                    produces = entry.getValue().getValue().toString();
                }
            }
        }
        return produces;
    }

    private List<ParsedParam> extractMethodParams(ExecutableElement ee) {
        List<? extends VariableElement> parameters = ee.getParameters();
        List<ParsedParam> params = new LinkedList<ParsedParam>();
        int i = 0;
        for (VariableElement p : parameters) {
            ParsedParam pp = extractMethodParam(i++, p);
            params.add(pp);
        }
        return params;
    }

    private ParsedParam extractMethodParam(int paramNumber, VariableElement variableElement) {
        ParsedParam pp = null;
        try {
            String type = variableElement.asType().toString().replaceFirst("class ", "");
            pp = new ParsedParam(paramNumber, type, variableElement.getSimpleName().toString());
        } catch (ClassNotFoundException e) {
            error("cannot load class: " + e.getMessage());
        }
        return pp;
    }
}
