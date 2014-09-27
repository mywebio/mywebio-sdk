package io.myweb.processor;

import io.myweb.api.*;
import io.myweb.http.Method;
import io.myweb.processor.model.ParsedMethod;
import io.myweb.processor.model.Provider;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

import java.util.*;

import static javax.lang.model.util.ElementFilter.methodsIn;
import static javax.lang.model.util.ElementFilter.typesIn;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class MyAnnotationProcessor extends AbstractProcessor {

	private List<ParsedMethod> parsedMethods = new LinkedList<ParsedMethod>();
	private Set<ExecutableElement> processed = new HashSet<ExecutableElement>();


	private void error(String msg) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
	}

	private void warning(String msg) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg);
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> annotationTypes = new HashSet<String>();
		annotationTypes.add(GET.class.getName());
		annotationTypes.add(PUT.class.getName());
		annotationTypes.add(DELETE.class.getName());
		annotationTypes.add(POST.class.getName());
		annotationTypes.add(Produces.class.getName());
		annotationTypes.add(ContentProvider.class.getName());
		return annotationTypes;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.isEmpty()) return false;
		MyValidator mValidator = new MyValidator(processingEnv.getMessager());
		MyParser mParser = new MyParser(processingEnv.getMessager(), mValidator);
		MyCodeGenerator mCodeGenerator = new MyCodeGenerator(processingEnv);
		try {
			List<Provider> providers = new LinkedList<Provider>();
			for (TypeElement annotation : annotations) {
//				System.out.println("Processing annotation: " + annotation.getQualifiedName());
				for (Element el : roundEnv.getElementsAnnotatedWith(annotation)) {
					if (el.getKind().equals(ElementKind.CLASS)) {
						processProviders(annotation, (TypeElement) el, providers);
					} else if (el.getKind().equals(ElementKind.METHOD)) {
						ExecutableElement ee = (ExecutableElement) el;
						if (!processed.contains(ee)) {
							processed.add(ee);
							ParsedMethod parsedMethod = mParser.parse(ee);
							parsedMethods.add(parsedMethod);
						}
					} else
						warning("Improper use of annotation @" + annotation.getSimpleName() + " for " + el.getKind());
				}
			}
			mCodeGenerator.generateCode(parsedMethods, providers);
			return true;
		} catch (Exception e) {
			error("error(s) found - details below: " + e);
		}
		return false;
	}

	private void processProviders(TypeElement annotation, TypeElement ce, List<Provider> providers) {
		if (!ContentProvider.class.getName().equals(annotation.getQualifiedName().toString()))
			return;
		if (ce.getQualifiedName().toString().equals(""))
			error("Annotation @" + annotation.getSimpleName() + " is not allowed for local or anonymous class!");
		for (AnnotationMirror am : ce.getAnnotationMirrors()) {
			String value = null;
			List<Method> methods = null;
			for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
				if ("value".equals(entry.getKey().getSimpleName().toString())) {
					value = (String) entry.getValue().getValue();
				}
				if ("methods".equals(entry.getKey().getSimpleName().toString())) {
					methods = convertMethods((List<Object>) entry.getValue().getValue());
				}
			}
			if (value != null && value.length() > 0) {
				Provider p = new Provider(value, ce.getSimpleName().toString(), methods);
				providers.add(p);
			}

		}
	}

	private static List<Method> convertMethods(List<Object> attrs) {
		LinkedList<Method> lm = new LinkedList<Method>();
		for (Object attr : attrs) {
			String name = attr.toString();
			lm.add(Method.findByName(name.substring(name.lastIndexOf(".") + 1)));
		}
		return lm;
	}

}
