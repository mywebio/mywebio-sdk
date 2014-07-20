package io.myweb.test.support;

import io.myweb.processor.MywebioAnnotationProcessor;

import java.io.File;
import java.io.IOException;
import java.util.*;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.tools.JavaCompiler.CompilationTask;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public abstract class MywebTestCase {

	private static final String SRC_TEST_DIR = "src/main/java";

	private static final String JAVA_FILE_EXT = ".java";

	private static final String OUT_DIR = "build/generated-test-src/test/java";

	private static final JavaCompiler JAVAC = ToolProvider.getSystemJavaCompiler();

	public static final List<String> EMPTY_LIST = Collections.emptyList();

	private final List<String> compilerOptions = new ArrayList<String>();

	private List<? extends Processor> processors = asList(new MywebioAnnotationProcessor());

	public MywebTestCase() {
		constructCompilerOptions();
	}

	private void constructCompilerOptions() {
		String dir = createDirIfNotExists(OUT_DIR).getAbsolutePath();
		compilerOptions.add("-s");
		compilerOptions.add(dir);
		compilerOptions.add("-d");
		compilerOptions.add(dir);
	}

	private File createDirIfNotExists(String dir) {
		File file = new File(dir);
		if (!file.exists()) {
			file.mkdirs();
		}
		return file;
	}

	protected List<Diagnostic<? extends JavaFileObject>> compile(Class<?> classToCompileAndProcess) {
		File fileToCompile = toFile(classToCompileAndProcess);
		return compile(fileToCompile);
	}

	private List<Diagnostic<? extends JavaFileObject>> compile(File fileToCompile) {
		DiagnosticCollector<JavaFileObject> diagC = new DiagnosticCollector<JavaFileObject>();
		StandardJavaFileManager fm = JAVAC.getStandardFileManager(diagC, null, null);
		Iterable<? extends JavaFileObject> javaFileObjects = fm.getJavaFileObjectsFromFiles(asList(fileToCompile));
		CompilationTask task = JAVAC.getTask(null, fm, diagC, compilerOptions, null, javaFileObjects);
		task.setProcessors(processors);
		task.call();
		try {
			fm.close();
		} catch (IOException exception) {
			System.out.println(exception.getMessage());
		}
		return diagC.getDiagnostics();
	}

	private File toFile(Class<?> clazz) {
		return new File(SRC_TEST_DIR + File.separator + classNameToSrcPath(clazz.getCanonicalName()) + JAVA_FILE_EXT);
	}

	private String classNameToSrcPath(String name) {
		return name.replace(".", File.separator);
	}

	protected static void assertCompilationSuccessful(List<Diagnostic<? extends JavaFileObject>> diagnostics) {
		for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
			assertFalse("Compilation error", diagnostic.getKind().equals(Diagnostic.Kind.ERROR));
		}
	}

	protected static void assertCompilationError(long expectedLineNumber, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
		boolean expectedDiagnosticFound = false;
		for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
			if ((diagnostic.getLineNumber() == expectedLineNumber)) {
				expectedDiagnosticFound = true;
			}
		}
		assertTrue("Expected error at line " + expectedLineNumber, expectedDiagnosticFound);
	}
}
