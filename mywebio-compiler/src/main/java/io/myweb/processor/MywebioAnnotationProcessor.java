package io.myweb.processor;

import android.content.Context;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;

import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.io.ByteStreams;
import io.myweb.api.*;
import io.myweb.processor.model.ParsedMethod;
import io.myweb.processor.model.ParsedParam;
import io.myweb.processor.velocity.VelocityLogger;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.*;
import java.util.*;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.size;

@SupportedAnnotationTypes({
		"io.myweb.api.GET",
		"io.myweb.api.POST",
		"io.myweb.api.DELETE",
		"io.myweb.api.PUT",
		"io.myweb.api.Produces"
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class MywebioAnnotationProcessor extends AbstractProcessor {

	private static final boolean quiet = false;

	private final static String TAB = "    ";

	private void log(String msg) {
		if (!quiet) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
		}
	}

	private void error(String msg) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
	}

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		log("process: annotations=" + annotations + ", roundEnv=" + roundEnv);
		if (annotations.isEmpty()) {
			return false;
		}

		List<ParsedMethod> parsedMethods = new LinkedList<ParsedMethod>();
		Set<ExecutableElement> processed = new HashSet<ExecutableElement>();

		try {
			for (TypeElement te : annotations) {
				Set<? extends Element> elementsAnnotated = roundEnv.getElementsAnnotatedWith(te);
				Set<ExecutableElement> executableElements = ElementFilter.methodsIn(elementsAnnotated);
				for (ExecutableElement ee : executableElements) {
					log("process: executableElement " + ee.getSimpleName().toString() + " hash=" + ee.hashCode());
					if (!processed.contains(ee)) {
						processed.add(ee);
						log("parameter names: " + Joiner.on(", ").join(ee.getParameters()));
						String destClass = ee.getEnclosingElement().toString();
						String destMethod = ee.getSimpleName().toString();
						String destMethodRetType = ee.getReturnType().toString();
						List<ParsedParam> params = new LinkedList<ParsedParam>();
						String httpMethod = "GET";
						String httpUri = "/";
						String produces = MimeTypes.MIME_TEXT_PLAIN;
						List<? extends VariableElement> parameters = ee.getParameters();
						int i = 0;
						Collection<String> types = transform(parameters, new Function<VariableElement, String>() {
							@Override
							public String apply(VariableElement ve) {
								return ve.asType().toString().replaceFirst("class ", "");
							}
						});
						log("parameters types: " + Joiner.on(", ").join(types));
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
							// TODO support other annotation types as well
							String annotationName = am.getAnnotationType().toString();
							if (GET.class.getName().equals(annotationName) || POST.class.getName().equals(annotationName)
									|| DELETE.class.getName().equals(annotationName) || PUT.class.getName().equals(annotationName)) {
								httpMethod = annotationName.substring(annotationName.lastIndexOf(".") + 1).trim();
								for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
									if ("value".equals(entry.getKey().getSimpleName().toString())) {
										httpUri = entry.getValue().getValue().toString();
										validateAnnotation(httpMethod, destMethodRetType, destMethod, params, httpUri, ee, am, entry.getValue());
										break;
									}
								}
							}
							if (Produces.class.getName().equals(annotationName)) {
								for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
									if ("value".equals(entry.getKey().getSimpleName().toString())) {
										produces = entry.getValue().getValue().toString();
										break;
									}
								}
							}
						}
						ParsedMethod parsedMethod = new ParsedMethod(destClass, destMethod, destMethodRetType, params, httpMethod, httpUri, produces);
						parsedMethods.add(parsedMethod);
					}
				}
			}
			generateCode(parsedMethods);
		} catch (Exception e) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "after exception " + e);
//			processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "after exception\n" + Throwables.getStackTraceAsString(e));
		}
		return false;
	}

	private void validateAnnotation(String httpMethod, String destMethodRetType, String destMethod, List<ParsedParam> params, String httpUri, ExecutableElement ee, AnnotationMirror am, AnnotationValue entry) {
		int paramsInAnnotation = StringUtils.countMatches(httpUri, ":");
		paramsInAnnotation += StringUtils.countMatches(httpUri, "*");
		int paramsInMethod = size(filter(params, new Predicate<ParsedParam>() {
			@Override
			public boolean apply(ParsedParam param) {
				return String.class.getName().equals(param.getTypeName()) || "int".equals(param.getTypeName());
			}
		}));
		if (paramsInAnnotation != paramsInMethod) {
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation does not apply to given method (details above)", ee, am);
			String errorMsg = buildErrorMsg(httpMethod, httpUri, destMethodRetType, destMethod, params);
			processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, errorMsg);
			throw new RuntimeException();
		}
	}

	private String buildErrorMsg(String httpMethod, String httpUri, String destMethodRetType, String destMethod, List<ParsedParam> params) {
		StringBuilder sb = new StringBuilder();
		sb.append("\n");
		appendAnnotation(sb, httpMethod, httpUri);
		sb.append("\n");
		appendAnnotationUnderline(sb, httpMethod, httpUri, params);
		sb.append("\n");
		appendMethod(sb, destMethodRetType, destMethod, params);
		sb.append("\n");
		appendMethodUnderline(sb, destMethodRetType, destMethod, httpUri, params);
		return sb.toString();
	}

	private void appendMethod(StringBuilder sb, String destMethodRetType, String destMethod, List<ParsedParam> params) {
		sb.append("\u001B[33m");
		sb.append(TAB);
		sb.append("public ");
		sb.append(simpleTypeName(destMethodRetType)).append(" ");
		sb.append(destMethod);
		sb.append("(");
		appendMethodParams(sb, params);
		sb.append(")");
		sb.append("\u001B[0m");
	}

	private String simpleTypeName(String destMethodRetType) {
		int lastDotIndex = destMethodRetType.lastIndexOf(".");
		if (lastDotIndex > 0) {
			return destMethodRetType.substring(lastDotIndex + 1);
		} else {
			return destMethodRetType;
		}
	}

	private void appendMethodParams(StringBuilder sb, List<ParsedParam> params) {
		Collection<String> typesAndNames = transform(params, new Function<ParsedParam, String>() {
			@Override
			public String apply(ParsedParam pe) {
				return simpleTypeName(pe.getTypeName()) + " " + pe.getName();
			}
		});
		String paramsStr = Joiner.on(", ").join(typesAndNames);
		sb.append(paramsStr);
	}

	private void appendAnnotation(StringBuilder sb, String httpMethod, String httpUri) {
		sb.append("\u001B[33m");
		sb.append(TAB).append("@").append(httpMethod);
		sb.append("(\"").append(httpUri).append("\")");
		sb.append("\u001B[0m");
	}

	private void appendAnnotationUnderline(StringBuilder sb, String httpMethod, String httpUri, List<ParsedParam> params) {
//		sb.append("                      ^^^^       ^^^");
		int beginOffset = 5 + httpMethod.length() + 2;  // TAB @NAME ("
		appendSpaces(sb, beginOffset);
		String[] uriSplit = httpUri.split("/");
		for (String pathElem : uriSplit) {
//			int i = httpUri.indexOf(pathElem);
			if (pathElem.startsWith(":")) {
				String paramName = pathElem.substring(1);
				if (isPathParamCorrect(paramName, params)) {
//					int paramTypeNameLength = paramTypeNameLength(paramName, params);
//					appendSpaces(sb, paramTypeNameLength + 1); // TYPENAME[space]
					appendSpaces(sb, 2 + paramName.length()); // NAME + "/:"
				} else {
//					int paramTypeNameLength = paramTypeNameLength(paramName, params);
//					appendUnderlineChars(sb, paramTypeNameLength + 1); // TYPENAME[space]
//					appendUnderlineChars(sb, pathElem.length() + 2); // NAME + ", "
					appendUnderlineChars(sb, 1 + paramName.length()); // NAME + "/:"
					appendSpaces(sb, 1); // NAME + "/:"
				}
			} else {
				appendSpaces(sb, 1 + pathElem.length());
			}
		}
	}

	private void appendMethodUnderline(StringBuilder sb, String destMethodRetType, String destMethod, String httpUri, List<ParsedParam> params) {
//		sb.append("                                      ^^^^^  ^^^^^^^^");
		int beginOffset  = 4 + "public ".length() + simpleTypeName(destMethodRetType).length() + 1 + destMethod.length() + 1;
		appendSpaces(sb, beginOffset);
		for (ParsedParam param : params) {
			if (isMethodParamCorrect(httpUri, param)) {
				int offset = simpleTypeName(param.getTypeName()).length() + 1; // TYPE AND SPACE
				offset += param.getName().length() + 2;
				appendSpaces(sb, offset); // NAME COMA SPACE
			} else {
				int offset = simpleTypeName(param.getTypeName()).length() + 1; // TYPE AND SPACE
				offset += param.getName().length();
				appendUnderlineChars(sb, offset); // NAME COMA
				appendSpaces(sb, 2);   // COMA SPACE
			}
		}
	}

	private boolean isMethodParamCorrect(String httpUri, ParsedParam param) {
		String[] pathElems = httpUri.split("/");
		boolean methodParamCorrect = false;
		for (String pathElem : pathElems) {
			if (pathElem.startsWith(":")) {
				String paramName = pathElem.substring(1);
				if (paramName.equals(param.getName())) {
					methodParamCorrect = true;
				}
			}
		}
		if ("android.content.Context".equals(param.getTypeName()) ||
				"org.json.JSONObject".equals(param.getTypeName())) {
			methodParamCorrect = true;
		}
		return methodParamCorrect;
	}

	private void appendUnderlineChars(StringBuilder sb, int count) {
		sb.append("\u001B[31m");
		for (int i = 0; i < count; i++) {
			sb.append("^");
		}
		sb.append("\u001B[0m");
	}

	private int paramTypeNameLength(final String paramName, List<ParsedParam> params) {
		ParsedParam param = Iterables.find(params, new Predicate<ParsedParam>() {
			@Override
			public boolean apply(ParsedParam pp) {
				return pp.getName().equals(paramName);
			}
		});
		return simpleTypeName(param.getTypeName()).length();
	}

	private void appendSpaces(StringBuilder sb, int count) {
		for (int i = 0; i < count; i++) {
			sb.append(" ");
		}
	}

	private boolean isPathParamCorrect(String pathParam, List<ParsedParam> params) {
		for (ParsedParam param : params) {
			if (param.getName().equals(pathParam)) {
				return true;
			}
		}
		return false;
	}

	private void generateCode(List<ParsedMethod> parsedMethods) {
		generateSourcesFromResource();
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new VelocityLogger(processingEnv.getMessager()));
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();
		Template t = ve.getTemplate("endpoint.vm");
		VelocityContext context = new VelocityContext();

		for (ParsedMethod pm : parsedMethods) {
			Writer w = null;
			try {
				Filer filer = processingEnv.getFiler();
				OutputStream os = filer.createSourceFile("io.myweb." + pm.getGeneratedClassName()).openOutputStream();
				w = new PrintWriter(os);
				context.put("i", pm);
				t.merge(context, w);
				w.close();
			} catch (IOException e) {
				error("Cannot create file: " + e.toString());
				return;
			}
		}
		generateAppInfoEndpoint(ve, parsedMethods);
		generateEndpointContainer(ve, parsedMethods);
	}

	private void generateSourcesFromResource() {
		String prefix = "/io/myweb/";
		String[] files = new String[] {
				prefix + "AssetEndpoint.java",
				prefix + "Endpoint.java",
				prefix + "FormalParam.java",
				prefix + "ActualParam.java",
				prefix + "RequestTask.java",
				prefix + "ResponseBuilder.java",
				prefix + "Server.java",
				prefix + "Service.java",
				prefix + "ThreadFactories.java",
		};
		for (String file : files) {
			generateSourceFileFromResource(file, classNameFromResourcePath(file));
		}
	}

	private void generateAppInfoEndpoint(VelocityEngine ve, List<ParsedMethod> parsedMethods) {
		Template t = ve.getTemplate("app-info-endpoint.vm");
		VelocityContext ctx = new VelocityContext();
		ctx.put("methods", parsedMethods);
		ctx.put("esc", new StringEscapeUtils());
		Writer w = null;
		try {
			OutputStream os = processingEnv.getFiler().createSourceFile("io.myweb.AppInfoEndpoint").openOutputStream();
			w = new PrintWriter(os);
//			OutputStream os = filer.createSourceFile("io.web.Service").openOutputStream();
//			w = new PrintWriter(os);
		} catch (IOException e) {
			error("Cannot create file: " + e.toString());
			return;
		}
		t.merge(ctx, w);
		try {
			w.close();
		} catch (IOException e) {
		}
	}

	private void generateEndpointContainer(VelocityEngine ve, List<ParsedMethod> parsedMethods) {
		Template t = ve.getTemplate("endpoint-container.vm");
		VelocityContext ctx = new VelocityContext();
		List<String> ls = new LinkedList<String>();
		for (ParsedMethod parsedMethod : parsedMethods) {
			ls.add("io.myweb." + parsedMethod.getGeneratedClassName());
		}
		ctx.put("endpoints", ls);
		Writer w = null;
		try {
			OutputStream os = processingEnv.getFiler().createSourceFile("io.myweb.EndpointContainer").openOutputStream();
			w = new PrintWriter(os);
//			OutputStream os = filer.createSourceFile("io.web.Service").openOutputStream();
//			w = new PrintWriter(os);
		} catch (IOException e) {
			error("Cannot create file: " + e.toString());
			return;
		}
		t.merge(ctx, w);
		try {
			w.close();
		} catch (IOException e) {
		}
	}

	private String classNameFromResourcePath(String resourePath) {
		String noTrailingSlash = resourePath.substring(1);
		String noJava = noTrailingSlash.replace(".java", "");
		return noJava.replaceAll("/", ".");
	}

	private void generateSourceFileFromResource(String resourcePath, String className) {
		try {
			InputStream is = this.getClass().getResourceAsStream(resourcePath);
			OutputStream os = processingEnv.getFiler().createSourceFile(className).openOutputStream();
			ByteStreams.copy(is, os);
			os.close();
			is.close();
		} catch (IOException e) {
			error(e.toString());
		}
	}
}
