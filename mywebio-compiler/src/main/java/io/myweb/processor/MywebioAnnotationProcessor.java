package io.myweb.processor;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import com.google.common.collect.*;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import io.myweb.api.*;
import io.myweb.processor.model.AssetFile;
import io.myweb.processor.model.ParsedMethod;
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
import javax.tools.JavaFileObject;
import java.io.*;
import java.util.*;

import static com.google.common.io.Files.fileTreeTraverser;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

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
		log("process: annotations=" + annotations + ", roundEnv=" + roundEnv);
		if (annotations.isEmpty()) {
			return false;
		}

		List<ParsedMethod> parsedMethods = new LinkedList<ParsedMethod>();
		Set<ExecutableElement> processed = new HashSet<ExecutableElement>();
		MywebValidator mywebValidator = new MywebValidator(processingEnv.getMessager());
		MywebParser mywebParser = new MywebParser(processingEnv.getMessager(), mywebValidator);

		try {
			for (TypeElement te : annotations) {
				Set<? extends Element> elementsAnnotated = roundEnv.getElementsAnnotatedWith(te);
				Set<ExecutableElement> executableElements = ElementFilter.methodsIn(elementsAnnotated);
				for (ExecutableElement ee : executableElements) {
					log("process: executableElement " + ee.getSimpleName().toString() + " hash=" + ee.hashCode());
					if (!processed.contains(ee)) {
						processed.add(ee);
						log("parameter names: " + Joiner.on(", ").join(ee.getParameters()));
						ParsedMethod parsedMethod = mywebParser.parse(ee);
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

	private void generateCode(List<ParsedMethod> parsedMethods) {
		String sourceCodePath = generateSourcesFromResource();
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new VelocityLogger(processingEnv.getMessager()));
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();
		generateAssetsInfo(ve, sourceCodePath);
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

	private void generateAssetsInfo(VelocityEngine ve, String sourceCodePath) {
		processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, sourceCodePath);
		String currentProjectPath = substringBeforeLast(sourceCodePath, "/build/");
		final String mywebAssetDir = currentProjectPath + "/src/main/assets/myweb";
		FluentIterable<File> filesAndDirs = fileTreeTraverser().breadthFirstTraversal(new File(mywebAssetDir));
		FluentIterable<File> files = filesAndDirs.filter(Files.isFile());
		FluentIterable<AssetFile> assetFiles = files.transform(new Function<File, AssetFile>() {
			@Override
			public AssetFile apply(File f) {
				String relativePath = substringAfter(f.getAbsolutePath(), mywebAssetDir);
				return new AssetFile(relativePath, f.length());
			}
		});
		generateAssetInfo(ve, assetFiles.toList());
		// TODO make debug/trace logs configurable in annotation processor
//		FluentIterable<String> str = assetFiles.transform(new Function<AssetFile, String>() {
//			@Override
//			public String apply(AssetFile af) {
//				return af.getName() + " " + af.getLength();
//			}
//		});
//		processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, Joiner.on("\n").join(str));
	}

	private String generateSourcesFromResource() {
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
		String basePath = "";
		for (String file : files) {
			basePath = generateSourceFileFromResource(file);
		}
		return basePath;
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
			JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile("io.myweb.EndpointContainer");
			OutputStream os = sourceFile.openOutputStream();
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

	private void generateAssetInfo(VelocityEngine ve, List<AssetFile> assetFiles) {
		Template t = ve.getTemplate("asset-info.vm");
		VelocityContext ctx = new VelocityContext();
		ctx.put("assetFiles", assetFiles);
		Writer w = null;
		try {
			OutputStream os = processingEnv.getFiler().createSourceFile("io.myweb.AssetInfo").openOutputStream();
			w = new PrintWriter(os);
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

	/**
	 * @param resourcePath
	 * @return absolute path of directory where file was saved
	 */
	private String generateSourceFileFromResource(String resourcePath) {
		try {
			String className = classNameFromResourcePath(resourcePath);
			InputStream is = this.getClass().getResourceAsStream(resourcePath);
			JavaFileObject sourceFile = processingEnv.getFiler().createSourceFile(className);
			String fullPath = sourceFile.toUri().getPath();
			String basePath = fullPath.replace(resourcePath, "");
			OutputStream os = sourceFile.openOutputStream();
			ByteStreams.copy(is, os);
			os.close();
			is.close();
			return basePath;
		} catch (IOException e) {
			error(e.toString());
		}
		return "";
	}
}
