package io.myweb.processor;

import com.google.common.base.Joiner;

import io.myweb.api.*;
import io.myweb.processor.model.ParsedMethod;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class MywebioAnnotationProcessor extends AbstractProcessor {

	private static final boolean quiet = false;

	private void log(String msg) {
		if (!quiet) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
		}
	}

	private void error(String msg) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> annotationTypes = new HashSet<String>();
		annotationTypes.add(GET.class.getName());
		annotationTypes.add(PUT.class.getName());
		annotationTypes.add(DELETE.class.getName());
		annotationTypes.add(POST.class.getName());
		annotationTypes.add(Produces.class.getName());
		return annotationTypes;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.isEmpty()) {
			return false;
		}
		List<ParsedMethod> parsedMethods = new LinkedList<ParsedMethod>();
		Set<ExecutableElement> processed = new HashSet<ExecutableElement>();
		MywebValidator mywebValidator = new MywebValidator(processingEnv.getMessager());
		MywebParser mywebParser = new MywebParser(processingEnv.getMessager(), mywebValidator);
		MywebCodeGenerator mywebCodeGenerator = new MywebCodeGenerator(processingEnv);
		try {
			for (TypeElement te : annotations) {
				Set<? extends Element> elementsAnnotated = roundEnv.getElementsAnnotatedWith(te);
				Set<ExecutableElement> executableElements = ElementFilter.methodsIn(elementsAnnotated);
				for (ExecutableElement ee : executableElements) {
					if (!processed.contains(ee)) {
						processed.add(ee);
						ParsedMethod parsedMethod = mywebParser.parse(ee);
						parsedMethods.add(parsedMethod);
					}
				}
			}
			mywebCodeGenerator.generateCode(parsedMethods);
		} catch (Exception e) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "after exception " + e);
		}
		return false;
	}
}
