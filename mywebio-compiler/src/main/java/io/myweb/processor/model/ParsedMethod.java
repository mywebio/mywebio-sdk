package io.myweb.processor.model;

import com.google.common.base.Joiner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.*;

import io.myweb.Endpoint;
import io.myweb.http.Method;

public class ParsedMethod {
	public static final String MINUS_DEFAULT = "-1";
	public static final String ZERO_LENGTH_DEFAULT = "";
	public static final String EMPTY_DEFAULT = "[]";
	public static final String NULL_DEFAULT = "null";
	public static final String FALSE_DEFAULT = "false";
	public static Map<String,String> DEFAULT_VALUES;

	static {
		Map<String, String> m = new HashMap<String, String>(10);
		m.put(Integer.class.getName(), MINUS_DEFAULT);
		m.put(Long.class.getName(), MINUS_DEFAULT);
		m.put(Float.class.getName(), MINUS_DEFAULT);
		m.put(Double.class.getName(), MINUS_DEFAULT);
		m.put(String.class.getName(), ZERO_LENGTH_DEFAULT);
		m.put(Boolean.class.getName(), FALSE_DEFAULT);
		m.put(Object.class.getName(), NULL_DEFAULT);
		m.put(JSONArray.class.getName(), EMPTY_DEFAULT);
		m.put(JSONObject.class.getName(), NULL_DEFAULT);
		DEFAULT_VALUES = Collections.unmodifiableMap(m);
	}

	private final String destClass;
	private final String destMethod;
	private final String destMethodRetType;
	private final List<ParsedParam> params;
	private final Method httpMethod;
	private final String httpUri;
	private final String produces;
	private final ServiceParam service;

	public ParsedMethod(String destClass, String destMethod, String destMethodRetType,
	                    List<ParsedParam> params, Method httpMethod, String httpUri,
	                    String produces, ServiceParam service) {
		this.destClass = destClass;
		this.destMethod = destMethod;
		this.destMethodRetType = destMethodRetType;
		this.params = Collections.unmodifiableList(params);
		this.httpMethod = httpMethod;
		this.httpUri = httpUri;
		this.produces = produces;
		this.service = service;
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
		return httpMethod.toString();
	}

	public String getHttpUri() {
		return httpUri;
	}

	public String getProduces() {
		return produces;
	}

	public ServiceParam getService() {
		return service;
	}

	public boolean isServicePresent() {
		return service != null;
	}

	public GeneratedPattern getGeneratedPattern() {
		// TODO handle errors
		String pathNoParams = cutParamsFromUrl(getHttpUri());
		String[] pathSplit = pathNoParams.split("/");
		StringBuilder patternSb = new StringBuilder();
		List<GroupMapping> groupMapping = new LinkedList<GroupMapping>();
		if (pathSplit.length == 0) {
			patternSb.append("[/]?");
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
					patternSb.append("[/]?(.*?)");
					curGroup++;
				} else {
					patternSb.append("[/]?").append(pathElm);
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
				String name, value;
				int idx = nameAndVal.indexOf("=");
				if (idx<0) {
					name = nameAndVal;
					value = "";
				} else {
					name = nameAndVal.substring(0, idx);
					value = nameAndVal.substring(idx + 1, nameAndVal.length());
				}
				if(name.startsWith(":")) name = name.substring(1);
				ParsedParam param = getParsedParamByName(name);
				if (param != null) {
					String typeName = toComplexTypeName(param.getTypeName());
					if (value.length()==0) {
						value = DEFAULT_VALUES.get(typeName);
					} else if (!typeName.equals(String.class.getName())) {
						// check if default value type is OK
						try {
							Object obj = new JSONTokener(value).nextValue();
							if (!Endpoint.classForName(typeName).isAssignableFrom(obj.getClass())) {
								throw new RuntimeException("Default value (" + value +
										") for parameter " + name + " is not assignable!");
							}
						} catch (JSONException e) {
							throw new RuntimeException("Invalid default value (" +
									value + ") for parameter " + name + "!",e);
						} catch (ClassNotFoundException e) {
							throw new RuntimeException(e.getMessage(),e);
						}
					}
				}
				result.add(new DefaultQueryParams(name, value));
			}
		}
		return result;
	}

	private ParsedParam getParsedParamByName(String name) {
		for (ParsedParam param: params) {
			if (param.getName().equals(name)) return param;
		}
		return null;
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

	private static String toComplexTypeName(String typeName) {
		if ("int".equals(typeName)) return Integer.class.getName();
		if ("long".equals(typeName)) return Long.class.getName();
		if ("float".equals(typeName)) return Float.class.getName();
		if ("double".equals(typeName)) return Double.class.getName();
		if ("boolean".equals(typeName)) return Boolean.class.getName();
		return typeName;
	}

}
