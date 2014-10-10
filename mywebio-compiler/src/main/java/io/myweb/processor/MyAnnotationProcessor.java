package io.myweb.processor;

import io.myweb.api.*;
import io.myweb.http.Method;
import io.myweb.http.Request;
import io.myweb.http.Response;
import io.myweb.processor.model.ParsedFilter;
import io.myweb.processor.model.ParsedMethod;
import io.myweb.processor.model.Provider;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.tools.Diagnostic;

import java.io.InvalidObjectException;
import java.security.InvalidParameterException;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class MyAnnotationProcessor extends AbstractProcessor {
	private final List<InternalProcessor> processors;
	private List<ParsedMethod> parsedMethods = new LinkedList<ParsedMethod>();
	private List<ParsedFilter> parsedFilters = new LinkedList<ParsedFilter>();
	private Set<ExecutableElement> processed = new HashSet<ExecutableElement>();
	private List<Provider> providers;

	public MyAnnotationProcessor() {
		ArrayList<InternalProcessor> p = new ArrayList<InternalProcessor>();
		p.add(new HttpMethodProcessor());
		p.add(new ContentProviderProcessor());
		p.add(new FilterProcessor());
		processors = Collections.unmodifiableList(p);
	}

	private void error(String msg) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
	}

	private void warning(String msg) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg);
	}

	@Override
	public Set<String> getSupportedAnnotationTypes() {
		Set<String> annotationTypes = new HashSet<String>();
		for (InternalProcessor ip : processors) {
			annotationTypes.addAll(ip.supportedTypes());
		}
		return annotationTypes;
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		if (annotations.isEmpty()) return false;
		MyCodeGenerator mCodeGenerator = new MyCodeGenerator(processingEnv);
		try {
			providers = new LinkedList<Provider>();
			for (TypeElement annotation : annotations) {
//				System.out.println("Processing: " + annotation.getQualifiedName().toString());
				for(InternalProcessor ip: processors) {
					if (ip.consume(annotation, roundEnv)) break;
				}
			}
			mCodeGenerator.generateCode(parsedMethods, providers, parsedFilters);
			return true;
		} catch (Exception e) {
//			e.printStackTrace();
			error("error(s) found - details above: " + e.getMessage());
		}
		return false;
	}

	private static List<Method> convertMethods(List<Object> attrs) {
		LinkedList<Method> lm = new LinkedList<Method>();
		if (attrs == null) {
			return Arrays.asList(Method.GET, Method.PUT, Method.POST, Method.DELETE);
		}
		for (Object attr : attrs) {
			String name = attr.toString();
			lm.add(Method.findByName(name.substring(name.lastIndexOf(".") + 1)));
		}
		return lm;
	}

	private class InternalProcessor {
		private final List<String> types;

		protected InternalProcessor(List<String> types) {
			this.types = types;
		}

		public List<String> supportedTypes() {
			return types;
		}

		public boolean consume(TypeElement annotation, RoundEnvironment roundEnv) throws Exception {
			for(String name: types) {
				if (name.equals(annotation.getQualifiedName().toString())) {
					return process(annotation, roundEnv);
				}
			}
			return false;
		}

		protected boolean process(TypeElement annotation, RoundEnvironment roundEnv) throws Exception {
			return false;
		}
	}

	private class HttpMethodProcessor extends InternalProcessor {
		MyValidator mValidator = null;
		MyParser mParser = null;

		public HttpMethodProcessor() {
			super(Arrays.asList(GET.class.getName(), PUT.class.getName(), DELETE.class.getName(),
					POST.class.getName(), Produces.class.getName(), BindService.class.getName()));
		}

		@Override
		protected boolean process(TypeElement annotation, RoundEnvironment roundEnv) throws Exception {
			if (mParser == null) {
				mValidator = new MyValidator(processingEnv.getMessager());
				mParser = new MyParser(processingEnv.getMessager(), mValidator);
			}
			for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
				ExecutableElement ee = (ExecutableElement) element;
				if (!processed.contains(ee)) {
					processed.add(ee);
					ParsedMethod parsedMethod = mParser.parse(ee);
					parsedMethods.add(parsedMethod);
				}
			}
			return true;
		}
	}

	private class ContentProviderProcessor extends InternalProcessor {
		public ContentProviderProcessor() {
			super(Arrays.asList(ContentProvider.class.getName()));
		}

		@Override
		@SuppressWarnings("unchecked")
		protected boolean process(TypeElement annotation, RoundEnvironment roundEnv) {
			for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
				TypeElement ce = (TypeElement) element;
				if (ce.getQualifiedName().toString().equals(""))
					error("Annotation @" + annotation.getSimpleName() + " is not allowed for local or anonymous classes!");
				String value = getAnnotationValue(annotation, ce, "value").toString();
				if (value != null && value.length() > 0) {
					List<Method> methods = convertMethods((List<Object>) getAnnotationValue(annotation, ce, "methods"));
					Provider p = new Provider(value, ce.getSimpleName().toString(), methods);
					providers.add(p);
				}
			}
			return true;
		}
	}

	private Object getAnnotationValue(TypeElement annotation, Element element, String name) {
		for (AnnotationMirror am: element.getAnnotationMirrors()) {
			if (am.getAnnotationType().toString().equals(annotation.getQualifiedName().toString())) {
				for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
					if (entry.getKey().getSimpleName().toString().equals(name)) {
						return entry.getValue().getValue();
					}
				}
			}
		}
		return null;
	}

	private class FilterProcessor extends InternalProcessor {
		MyValidator mValidator = null;

		public FilterProcessor() {
			super(Arrays.asList(Before.class.getName(), After.class.getName()));
		}

		@Override
		protected boolean process(TypeElement annotation, RoundEnvironment roundEnv) throws Exception {
			if (mValidator == null) {
				mValidator = new MyValidator(processingEnv.getMessager());
			}
			for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
				ExecutableElement ee = (ExecutableElement) element;
				String value = getAnnotationValue(annotation, element, "value").toString();
				boolean isBefore = annotation.getQualifiedName().toString().equals(Before.class.getName());
				// validate parameters and return type
				ParsedFilter filter = mValidator.validateFilterAnnotation(value, isBefore, ee);
				parsedFilters.add(filter);
				return true;
			}
			return false;
		}
	}

}
