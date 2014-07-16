package io.myweb;

import android.content.Context;
import android.net.LocalSocket;
import android.util.Log;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Endpoint {

	public static final int OUTPUT_STREAM_BUFFER_SIZE = 32 * 1024;

	private Context context;

	public Endpoint(Context context) {
		this.context = context;
	}

	public abstract void invoke(String uri, String request, LocalSocket localSocket, String reqId) throws Exception;

	protected Context getContext() {
		return context;
	}

	protected abstract Pattern getPattern();

	protected Matcher matcher(String uri) {
		return getPattern().matcher(uri);
	}

	protected void writeResponseHeaders(OutputStream os, String reqId) throws IOException {
		os.write((reqId + "\n").getBytes());
		os.write("HTTP/1.1 200 OK\r\n".getBytes());
		os.write("Connection: keep-alive\r\n".getBytes());
	}

	protected OutputStream outputStream(LocalSocket localSocket) throws IOException {
		return new BufferedOutputStream(localSocket.getOutputStream(), OUTPUT_STREAM_BUFFER_SIZE);
	}

	public boolean match(String method, String uri) {
		String urlNoQueryParams = urlNoQueryParams(uri);
		Matcher m = matcher(urlNoQueryParams);
		boolean matched = httpMethod().equals(method) && m.matches();
		if (matched) {
			Log.d("Endpoint", "matched path " + httpMethod() + " " + originalPath() + " (pattern: " + getPattern() + ") request: " + method + " " + uri);
		} else {
			Log.d("Endpoint", "not matched path " + httpMethod() + " " + originalPath() + " (pattern: " + getPattern() + ") request: " + method + " " + uri);
		}
		return matched;
	}

	protected abstract String originalPath();

	protected abstract String httpMethod();

	protected ActualParam[] actualParams(String uri, String requestBody, FormalParam[] formalParams, Map<String, String> defaultQueryParams, Map<String, Integer> groupMap, Context ctx) throws Exception {
		String urlNoQueryParams = urlNoQueryParams(uri);
		Map<String, String> paramsMap;
		if ("POST".equals(httpMethod())) {
			paramsMap = decodeQueryString(requestBody);
		} else {
			paramsMap = decodeQueryString(queryParams(uri));
		}
		Matcher m = matcher(urlNoQueryParams);
		ActualParam[] actualParams = new ActualParam[formalParams.length];
		if (m.matches()) {
			for (FormalParam fp : formalParams) {
				int fpId = fp.getId();
				Class<?> fpClazz = classForName(fp.getTypeName());
				String fpName = fp.getName();
				if (groupMap.containsKey(fpName)) {
					int urlGroupIdx = groupMap.get(fpName);
					String val = m.group(urlGroupIdx);
					Object convertedVal = convert(val, fp.getTypeName());
					actualParams[fpId] = new ActualParam(fpClazz, convertedVal);
				} else if (Context.class.equals(fpClazz)) {
					actualParams[fpId] = new ActualParam(Context.class, ctx);
				} else if (paramsMap.containsKey(fpName)) {
					String val = paramsMap.get(fpName);
					Object convertedVal = convert(val, fp.getTypeName());
					actualParams[fpId] = new ActualParam(fpClazz, convertedVal);
				} else if (defaultQueryParams.containsKey(fpName)) {
					String val = defaultQueryParams.get(fpName);
					Object convertedVal = convert(val, fp.getTypeName());
					actualParams[fpId] = new ActualParam(fpClazz, convertedVal);
				}
			}
		} else {
			throw new Exception("couldn't match URI: '" + uri + "' with pattern '" + getPattern() + "' (BTW, this shouldn't happen...)");
		}
		return actualParams;
	}

	protected String body(String request) {
		int firstEmptyLine = request.indexOf("\r\n\r\n");
		return request.substring(firstEmptyLine + 4);
	}

	private String urlNoQueryParams(String url) {
		int i = url.indexOf("?");
		String result;
		if (i == -1) {
			result = url;
		} else {
			result = url.substring(0, i);
		}
		return result;
	}

	private Map<String, String> decodeQueryString(String queryParamsStr) {
		String[] nameAndValues = queryParamsStr.split("&");
		Map<String, String> result = new HashMap<String, String>();
		for (String nameAndVal : nameAndValues) {
			if (!"".equals(nameAndVal)) {
				String[] nv = nameAndVal.split("=");
				result.put(nv[0], nv[1]);
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

	private Object convert(String val, String typeName) {
		Object result;
		if ("int".equals(typeName) || Integer.class.getName().equals(typeName)) {
			result = Integer.parseInt(val);
		} else {
			result = val;
		}
		return result;
	}

	private Class<?> classForName(String typeName) throws ClassNotFoundException {
		String classToLoad;
		if ("int".equals(typeName)) {
			classToLoad = "[I";
		} else {
			classToLoad = typeName;
		}
		return Class.forName(classToLoad);
	}
}
