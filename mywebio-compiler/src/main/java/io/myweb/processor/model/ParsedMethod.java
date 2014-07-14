package io.myweb.processor.model;

import com.google.common.base.Joiner;

import java.util.*;

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

	public List<ParsedParam> getParsedParams() {
		return params;
	}

	public String getHttpMethod() {
		return httpMethod;
	}

	public String getHttpUri() {
		return httpUri;
	}

	public GeneratedPattern getGeneratedPattern() {
		// TODO handle errors
		String pathNoParams = cutParamsFromUrl(getHttpUri());
		String[] pathSplit = pathNoParams.split("/");
		StringBuilder patternSb = new StringBuilder();
		List<GroupMapping> groupMapping = new LinkedList<GroupMapping>();
		if (pathSplit.length == 0) {
			patternSb.append("/");
		} else {
			int curGroup = 1; // group indexing in regex starts from 1
			for (String pathElm : pathSplit) {
				if ("".equals(pathElm)) {
					// NOP
				} else if (pathElm.startsWith(":")) {
					groupMapping.add(new GroupMapping(pathElm.substring(1), curGroup));
					patternSb.append("/([^/]+)");
					curGroup++;
				} else if (pathElm.startsWith("*")) {
					groupMapping.add(new GroupMapping(pathElm.substring(1), curGroup));
					patternSb.append("/(.*?)");
					curGroup++;
				} else {
					patternSb.append("/").append(pathElm);
				}
			}
		}
		return new GeneratedPattern(patternSb.toString(), groupMapping);
	}

	public List<DefaultQueryParams> getDefaultQueryParams() {
		String queryString = queryParams(getHttpUri());
		String[] nameAndValues = queryString.split("&");
		List<DefaultQueryParams> result = new LinkedList<DefaultQueryParams>();
		for (String nameAndVal : nameAndValues) {
			if (!"".equals(nameAndVal)) {
				String[] nv = nameAndVal.split("=");
				String nvNoColon = nv[0].replaceFirst(":", "");
				result.add(new DefaultQueryParams(nvNoColon, nv[1]));
			}
		}
		return result;
	}

	private String queryParams(String url) {
		int i = url.indexOf("?");
		String queryParams;
		if (i == -1) {
			queryParams = "";
		} else {
			queryParams = url.substring(i + 1);
		}
		return queryParams;
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
		return "(" + toComplexTypeName(param.getTypeName()) + ")" + "ap[" + param.getId() + "].getVal()";
	}

	private String toComplexTypeName(String typeName) {
		// TODO add support for more types
		String result;
		if ("int".equals(typeName)) {
			result = "java.lang.Integer";
		} else {
			result = typeName;
		}
		return result;
	}
}
