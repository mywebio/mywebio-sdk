package io.myweb.test;

import io.myweb.test.support.MywebTestCase;
import org.junit.Test;

import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.util.List;

public class ErrorTest extends MywebTestCase {

	@Test
	public void compilationErrorShouldBe() throws IOException {
		// given
		List<Diagnostic<? extends JavaFileObject>> diagnostics = compile(TestError.class);
		assertCompilationError(7, diagnostics);
	}
}
