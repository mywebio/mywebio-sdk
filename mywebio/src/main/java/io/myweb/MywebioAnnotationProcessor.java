package io.myweb;

import com.google.common.io.ByteStreams;
import io.myweb.model.ParsedMethod;
import io.myweb.model.ParsedParam;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import io.myweb.api.GET;
import io.myweb.api.MimeTypes;
import io.myweb.api.POST;
import io.myweb.api.Produces;

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
		"io.myweb.api.Produces"
})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class MywebioAnnotationProcessor extends AbstractProcessor implements LogChute {

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
//				log("process: executableElement " + ee.getSimpleName().toString() + " hash=" + ee.hashCode());
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
						ParsedParam pp = new ParsedParam(i++, p.asType().toString(), p.getSimpleName().toString());
						params.add(pp);
					}
					for (AnnotationMirror am : ee.getAnnotationMirrors()) {
						// TODO verify if annotation aren't duplicated
						// TODO support other annotation types as well
						String annotationName = am.getAnnotationType().toString();
						if (GET.class.getName().equals(annotationName) || POST.class.getName().equals(annotationName)) {
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
		generateEndpointInterface();
		generateAssetEndpoint();
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, this);
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

		List<String> ls = new LinkedList<String>();
		for (ParsedMethod parsedMethod : parsedMethods) {
			ls.add("io.myweb." + parsedMethod.getGeneratedClassName());
		}
		Template t2 = ve.getTemplate("service.vm");
		VelocityContext ctx2 = new VelocityContext();
		ctx2.put("endpoints", ls);
		ctx2.put("importPrefix", "");
		Writer w = null;
		try {
			Filer filer = processingEnv.getFiler();
			OutputStream os = filer.createSourceFile("io.myweb.Service").openOutputStream();
			w = new PrintWriter(os);
		} catch (IOException e) {
//				error("Cannot create file: " + e.toString());
			return;
		}
//			log(" template : " + w);
			t2.merge(ctx2, w);
//			log(" string : " + w);
		try {
			w.close();
		} catch (IOException e) {
//			error(e.toString());
		}
		generateAppInfoEndpoint(ve, parsedMethods);
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
//				error("Cannot create file: " + e.toString());
			return;
		}
		t.merge(ctx, w);
		try {
			w.close();
		} catch (IOException e) {
		}
	}

	private void generateEndpointInterface() {
		try {
			InputStream is = this.getClass().getResourceAsStream("/io/myweb/gen/Endpoint.java_");
			OutputStream os = processingEnv.getFiler().createSourceFile("io.myweb.gen.Endpoint").openOutputStream();
			ByteStreams.copy(is, os);
			os.close();
			is.close();
		} catch (IOException e) {
//			error(e.toString());
		}
	}

	private void generateAssetEndpoint() {
		try {
			InputStream is = this.getClass().getResourceAsStream("/io/myweb/gen/AssetEndpoint.java_");
			OutputStream os = processingEnv.getFiler().createSourceFile("io.myweb.gen.AssetEndpoint").openOutputStream();
			ByteStreams.copy(is, os);
			os.close();
			is.close();
		} catch (IOException e) {
//			error(e.toString());
		}
	}

	public void writeInputStream(InputStream is, Writer w) throws IOException {
		char[] buffer = new char[1024];
		Reader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
		int n;
		while ((n = reader.read(buffer)) != -1) {
			w.write(buffer, 0, n);
		}
	}

	private void processExecutableElement(ExecutableElement ee) {
		log("processExecutableElement ee=" + ee);
	}

	private void processAnnotation(Element e) {
		log("processAnnotation element=" + e);
	}

	@Override
	public void init(RuntimeServices rs) throws Exception {
		log("init: " + rs);
	}

	@Override
	public void log(int level, String message) {
		log("log(" + level + "): " + message);
	}

	@Override
	public void log(int level, String message, Throwable t) {
		log("log(" + level + "): " + message + " ex: " + t);
	}

	@Override
	public boolean isLevelEnabled(int level) {
		return true;
	}
}
