package io.myweb.processor;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;

import io.myweb.processor.model.AssetFile;
import io.myweb.processor.model.ParsedFilter;
import io.myweb.processor.model.ParsedMethod;
import io.myweb.processor.model.Provider;
import io.myweb.processor.velocity.VelocityLogger;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;


import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;

import java.io.*;
import java.util.List;
import java.util.regex.Pattern;

import static com.google.common.io.Files.fileTreeTraverser;
import static com.google.common.io.Files.isFile;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

public class MyCodeGenerator extends ProcessingEnvAware {
	public static final String PKG_PREFIX = "io.myweb.";
	private String sourceCodePath = "";

	public static class EscTool {
		public String java(String str) {
			return str.replaceAll("\\\\","\\\\\\\\");
		}
	}

	public MyCodeGenerator(ProcessingEnvironment processingEnvironment) {
		super(processingEnvironment);
	}

	public void generateCode(List<ParsedMethod> parsedMethods, List<Provider> providers, List<ParsedFilter> filters) {
		VelocityEngine ve = instantiateVelocityEngine();
		if (!providers.isEmpty()) {
			generateProviders(ve, providers);
		} else {
			generateEndpoints(ve, parsedMethods);
			generateFilters(ve, filters);
			generateAssetsInfo(ve, sourceCodePath);
			generateService(ve, parsedMethods, filters);
		}
	}

	private void generateFilters(VelocityEngine ve, List<ParsedFilter> filters) {
		for (ParsedFilter filter : filters) {
			Context ctx = createContext();
			ctx.put("filter", filter);
			generateFromTemplate(ve, ctx, PKG_PREFIX + filter.getGeneratedClassName(), "filter.vm");
		}
	}

	private void generateProviders(VelocityEngine ve, List<Provider> providers) {
		for (Provider provider : providers) {
			Context ctx = createContext();
			ctx.put("provider", provider);
			generateFromTemplate(ve, ctx, PKG_PREFIX + provider.getGeneratedClassName(), "provider.vm");
		}
	}

	private void generateEndpoints(VelocityEngine ve, List<ParsedMethod> parsedMethods) {
		for (ParsedMethod pm : parsedMethods) {
			Context ctx = createContext();
			ctx.put("parsedMethod", pm);
			generateFromTemplate(ve, ctx, PKG_PREFIX + pm.getGeneratedClassName(), "endpoint.vm");
		}
	}

	private void generateService(VelocityEngine ve, List<ParsedMethod> parsedMethods, List<ParsedFilter> filters) {
		Context ctx = createContext();
		ctx.put("endpoints", parsedMethods);
		ctx.put("filters", filters);
		generateFromTemplate(ve, ctx, PKG_PREFIX + "Service");
	}

	private VelocityEngine instantiateVelocityEngine() {
		VelocityEngine ve = new VelocityEngine();
		ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new VelocityLogger(getMessager()));
		ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
		ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
		ve.init();
		return ve;
	}

	private VelocityContext createContext() {
		VelocityContext velocityContext = new VelocityContext();
		velocityContext.put("esc", new EscTool());
		return velocityContext;
	}

	private void generateAssetsInfo(VelocityEngine ve, String sourceCodePath) {
		String currentProjectPath = substringBeforeLast(sourceCodePath, "/build/");
		final String mywebAssetDir = currentProjectPath + "/src/main/assets/myweb";
		FluentIterable<File> filesAndDirs = fileTreeTraverser().breadthFirstTraversal(new File(mywebAssetDir));
		FluentIterable<File> files = filesAndDirs.filter(isFile());
		FluentIterable<AssetFile> assetFiles = files.transform(new Function<File, AssetFile>() {
			@Override
			public AssetFile apply(File f) {
				String relativePath = substringAfter(f.getAbsolutePath(), mywebAssetDir);
				return new AssetFile(relativePath, f.length());
			}
		});
		generateAssetInfo(ve, assetFiles.toList());
	}

	private void generateAssetInfo(VelocityEngine ve, List<AssetFile> assetFiles) {
		Context ctx = createContext();
		ctx.put("assetFiles", assetFiles);
		generateFromTemplate(ve, ctx, PKG_PREFIX + "MyAssetInfo");
	}

	private void generateFromTemplate(VelocityEngine ve, Context ctx, String classToGenerate) {
		generateFromTemplate(ve, ctx, classToGenerate, classToGenerate + ".vm");
	}

	private void generateFromTemplate(VelocityEngine ve, Context ctx, String classToGenerate, String templateName) {
		Template t = ve.getTemplate(templateName);
		Writer w = null;
		try {
			OutputStream os = null;
			JavaFileObject sourceFile = getProcessingEnv().getFiler().createSourceFile(classToGenerate);
			updateSourceCodePath(sourceFile, classToGenerate);
			os = sourceFile.openOutputStream();
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

	private void updateSourceCodePath(JavaFileObject sourceFile, String classToGenerate) {
		if (sourceCodePath.length()==0) {
			String fullPath = sourceFile.toUri().getPath();
			fullPath = fullPath.substring(0,fullPath.lastIndexOf("/"));
			String resourcePath = classToGenerate.replace(".","/");
			resourcePath = resourcePath.substring(0,resourcePath.lastIndexOf("/"));
			sourceCodePath = fullPath.replace(resourcePath, "");
		}
	}
}
