package io.myweb.processor;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.io.ByteStreams;

import io.myweb.processor.model.AssetFile;
import io.myweb.processor.model.ParsedMethod;
import io.myweb.processor.velocity.VelocityLogger;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileObject;

import java.io.*;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.io.Files.fileTreeTraverser;
import static com.google.common.io.Files.isFile;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBeforeLast;

public class MywebCodeGenerator extends ProcessingEnvAware {

    public MywebCodeGenerator(ProcessingEnvironment processingEnvironment) {
        super(processingEnvironment);
    }

    public void generateCode(List<ParsedMethod> parsedMethods) {
        String sourceCodePath = copySourcesFromResources();
        VelocityEngine ve = instantiateVelocityEngine();
        generateAssetsInfo(ve, sourceCodePath);
        generateEndpoints(ve, parsedMethods);
        generateAppInfoEndpoint(ve, parsedMethods);
        generateEndpointContainer(ve, parsedMethods);
    }

    private void generateEndpoints(VelocityEngine ve, List<ParsedMethod> parsedMethods) {
        for (ParsedMethod pm : parsedMethods) {
            VelocityContext ctx = new VelocityContext();
            ctx.put("parsedMethod", pm);
            generateFromTemplate(ve, ctx, "io.myweb." + pm.getGeneratedClassName(), "endpoint.vm");
        }
    }

    private VelocityEngine instantiateVelocityEngine() {
        VelocityEngine ve = new VelocityEngine();
        ve.setProperty(VelocityEngine.RUNTIME_LOG_LOGSYSTEM, new VelocityLogger(getMessager()));
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
        ve.setProperty("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        ve.init();
        return ve;
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

    private void generateAppInfoEndpoint(VelocityEngine ve, List<ParsedMethod> parsedMethods) {
        VelocityContext ctx = new VelocityContext();
        ctx.put("methods", parsedMethods);
        ctx.put("esc", new StringEscapeUtils());
        generateFromTemplate(ve, ctx, "io.myweb.AppInfoEndpoint");
    }

    private void generateEndpointContainer(VelocityEngine ve, List<ParsedMethod> parsedMethods) {
        VelocityContext ctx = new VelocityContext();
        List<String> ls = new LinkedList<String>();
        for (ParsedMethod parsedMethod : parsedMethods) {
            ls.add("io.myweb." + parsedMethod.getGeneratedClassName());
        }
        ctx.put("endpoints", ls);
        generateFromTemplate(ve, ctx, "io.myweb.EndpointContainer");
    }

    private void generateAssetInfo(VelocityEngine ve, List<AssetFile> assetFiles) {
        VelocityContext ctx = new VelocityContext();
        ctx.put("assetFiles", assetFiles);
        generateFromTemplate(ve, ctx, "io.myweb.AssetInfo");
    }

    private void generateFromTemplate(VelocityEngine ve, VelocityContext ctx, String classToGenerate) {
        generateFromTemplate(ve, ctx, classToGenerate, classToGenerate + ".vm");
    }

    private void generateFromTemplate(VelocityEngine ve, VelocityContext ctx, String classToGenerate, String templateName) {
        Template t = ve.getTemplate(templateName);
        Writer w = null;
        try {
            OutputStream os = getProcessingEnv().getFiler().createSourceFile(classToGenerate).openOutputStream();
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
     * @return absolute path of directory where files was saved
     */
    private String copySourcesFromResources() {
        String prefix = "/io/myweb/";
        String[] files = new String[]{
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
            basePath = copySourceFromResources(file);
        }
        return basePath;
    }

    /**
     * @param resourcePath
     * @return absolute path of directory where file was saved
     */
    private String copySourceFromResources(String resourcePath) {
        try {
            String className = classNameFromResourcePath(resourcePath);
            InputStream is = this.getClass().getResourceAsStream(resourcePath);
            JavaFileObject sourceFile = getProcessingEnv().getFiler().createSourceFile(className);
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
