package io.myweb.processor.model;

import android.content.Context;
import com.google.common.base.Joiner;

import java.io.FileDescriptor;
import java.util.Collections;
import java.util.List;

public class ParsedMethod {

	private final String destClass;
	private final String destMethod;
	private final String destMethodRetType;
	private final List<ParsedParam> params;
	private final String httpMethod;
	private final String httpUri;
	private final String produces;

	public ParsedMethod(String destClass, String destMethod, String destMethodRetType, List<ParsedParam> params, String httpMethod, String httpUri, String produces) {
		this.destClass = destClass;
		this.destMethod = destMethod;
		this.destMethodRetType = destMethodRetType;
		this.params = Collections.unmodifiableList(params);
		this.httpMethod = httpMethod;
		this.httpUri = httpUri;
		this.produces = produces;
	}

	public String getDestClassSimple() {
		return getDestClass().substring(getDestClass().lastIndexOf(".") + 1).trim();
	}

	public String getGeneratedClassName() {
		return getDestClassSimple() + "_" + getDestMethod();
	}

	public String getDestClass() {
		return destClass;
	}

	public String getDestMethod() {
		return destMethod;
	}

	public String getDestMethodRetType() {
		return destMethodRetType;
	}

	public List<ParsedParam> getParams() {
		return params;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public String getHttpUri() {
		return httpUri;
	}

	public String getPatternStr() {
		// TODO handle errors
		String pathNoParams = cutParamsFromUrl(getHttpUri());
		String[] pathSplit = pathNoParams.split("/");
		StringBuilder patternSb = new StringBuilder();
		for (String pathElm : pathSplit) {
			if ("".equals(pathElm)) {
				// NOP
			} else if (pathElm.startsWith(":")) {
				// TODO store group names
				patternSb.append("/([^/]+)");
			} else if (pathElm.startsWith("*")) {
				// TODO store group names
				patternSb.append("/(.*?)");
			} else {
				patternSb.append("/").append(pathElm);
			}
		}
		return patternSb.toString();
	}

	private String cutParamsFromUrl(String url) {
		int i = url.indexOf("?");
		String pattern;
		if (i == -1) {
			pattern = url;
		} else {
			pattern = url.substring(0, i);
		}
		return pattern;
	}

	public String getProduces() {
		return produces;
	}

	public String getParamsJavaSrc() {
		String[] p = new String[params.size()];
		int i = 0;
		for (ParsedParam param : params) {
			p[i++] = paramToJavaSrc(param);
		}
		return Joiner.on(", ").join(p);
	}

	private String paramToJavaSrc(ParsedParam param) {
		String result;
		// TODO refactor to some kind of handlers
		if (Context.class.getName().equals(param.getType())) {
			result = "context";
		} else if (FileDescriptor.class.getName().equals(param.getType())) {
			result = "localSocket.getFileDescriptor()";
		} else {
			result = "(" + param.getType() + ")" + "ap[0].val()";
		}
		return result;
	}
}
