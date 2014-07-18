package io.myweb.processor;

import com.google.common.io.ByteStreams;
import io.myweb.api.*;
import io.myweb.processor.model.ParsedMethod;
import io.myweb.processor.model.ParsedParam;
import io.myweb.processor.velocity.VelocityLogger;
import org.apache.commons.lang3.StringEscapeUtils;
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

		for (TypeElement te: annotations) {
			Set<? extends Element> elementsAnnotated = roundEnv.getElementsAnnotatedWith(te);
			Set<ExecutableElement> executableElements = ElementFilter.methodsIn(elementsAnnotated);
			for (ExecutableElement ee : executableElements) {
				log("process: executableElement " + ee.getSimpleName().toString() + " hash=" + ee.hashCode());
				if (!processed.contains(ee)) {
					processed.add(ee);
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
						// TODO support other annotation types as well
						String annotationName = am.getAnnotationType().toString();
						if (GET.class.getName().equals(annotationName) || POST.class.getName().equals(annotationName)
								|| DELETE.class.getName().equals(annotationName) || PUT.class.getName().equals(annotationName)) {
							httpMethod = annotationName.substring(annotationName.lastIndexOf(".") + 1).trim();
							for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
								if ("value".equals(entry.getKey().getSimpleName().toString())) {
									httpUri = entry.getValue().getValue().toString();
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
