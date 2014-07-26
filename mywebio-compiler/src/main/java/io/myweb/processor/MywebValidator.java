package io.myweb.processor;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import io.myweb.processor.model.ParsedParam;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.processing.Messager;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.tools.Diagnostic;
import java.util.Collection;
import java.util.List;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.size;

public class MywebValidator extends AnnotationMessagerAware {

	private final static String TAB = "    ";

	public MywebValidator(Messager messager) {
		super(messager);
	}

	public void validateAnnotation(String httpMethod, String destMethodRetType, String destMethod, List<ParsedParam> params, String httpUri, ExecutableElement ee, AnnotationMirror am, AnnotationValue entry) {
		int paramsInAnnotation = StringUtils.countMatches(httpUri, ":");
		paramsInAnnotation += StringUtils.countMatches(httpUri, "*");
		int paramsInMethod = size(filter(params, new Predicate<ParsedParam>() {
			@Override
			public boolean apply(ParsedParam param) {
				return String.class.getName().equals(param.getTypeName()) || "int".equals(param.getTypeName());
			}
		}));
		if (paramsInAnnotation != paramsInMethod) {
			getMessager().printMessage(Diagnostic.Kind.ERROR, "Annotation does not apply to given method (details above)", ee, am);
			String errorMsg = buildErrorMsg(httpMethod, httpUri, destMethodRetType, destMethod, params);
			getMessager().printMessage(Diagnostic.Kind.ERROR, errorMsg);
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
		int beginOffset = 5 + httpMethod.length() + 2;  // TAB @NAME ("
		appendSpaces(sb, beginOffset);
		String[] uriSplit = httpUri.split("/");
		for (String pathElem : uriSplit) {
			if (pathElem.startsWith(":")) {
				String paramName = pathElem.substring(1);
				if (isPathParamCorrect(paramName, params)) {
					appendSpaces(sb, 2 + paramName.length()); // NAME + "/:"
				} else {
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
}
