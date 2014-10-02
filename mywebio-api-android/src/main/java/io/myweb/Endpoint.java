package io.myweb;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.myweb.http.Cookie;
import io.myweb.http.Cookies;
import io.myweb.http.Headers;
import io.myweb.http.Method;
import io.myweb.http.Request;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Endpoint {

	public static class MethodAndUri {
		private final Method method;
		private final String uri;

		public MethodAndUri(Method method, String uri) {
			this.method = method;
			this.uri = uri;
		}

		public Method getMethod() {
			return method;
		}

		public String getUri() {
			return uri;
		}
	}

	private Server server;

	public Endpoint(Server server) {
		this.server = server;
	}

	public abstract void invoke(String uri, Request request, OutputStream os) throws Exception;

	protected Server getServer() {
		return server;
	}

	protected Context getContext() {
		return server.getContext();
	}

	protected abstract Pattern getPattern();

	protected Matcher matcher(String uri) {
		return getPattern().matcher(uri);
	}

	public boolean match(Method method, String uri) {
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

	protected abstract Method httpMethod();

	protected ActualParam[] actualParams(String uri, Request request, FormalParam[] formalParams, Map<String, String> defaultQueryParams, Map<String, Integer> groupMap, Context ctx) throws Exception {
		String urlNoQueryParams = urlNoQueryParams(uri);
		Map<String, String> paramsMap;
		if (Method.POST.equals(httpMethod())) {
			paramsMap = decodeQueryString(request.getBodyAsString());
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
				} else if (Cookies.class.equals(fpClazz)){
					actualParams[fpId] = new ActualParam(Cookies.class, request.getCookies());
				} else if (Cookie.class.equals(fpClazz)) {
					Cookie cookie = request.getCookies().getCookie(fpName);
					actualParams[fpId] = new ActualParam(Cookie.class, cookie);
				} else if (Headers.class.equals(fpClazz)) {
					actualParams[fpId] = new ActualParam(Headers.class, request.getHeaders());
				} else if (Request.class.equals(fpClazz)) {
					actualParams[fpId] = new ActualParam(Request.class, request);
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

	protected String urlNoQueryParams(String url) {
		int i = url.indexOf("?");
		String result;
		if (i == -1) {
			result = url;
		} else {
			result = url.substring(0, i);
		}
		return result;
	}

	protected Map<String, String> decodeQueryString(String queryParamsStr) {
		String[] nameAndValues = queryParamsStr.split("&");
		Map<String, String> result = new HashMap<String, String>();
		for (String nameAndVal : nameAndValues) {
			if (!"".equals(nameAndVal)) {
				int idx = nameAndVal.indexOf("=");
				try {
					if(idx>0)
					result.put(nameAndVal.substring(0,idx),
							URLDecoder.decode(nameAndVal.substring(idx+1),"UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	protected String queryParams(String url) {
		int i = url.indexOf("?");
		String queryParams;
		if (i == -1) {
			queryParams = "";
		} else {
			queryParams = url.substring(i + 1);
		}
		return queryParams;
	}

	public static Object convert(String val, String typeName) {
		if(!String.class.getName().equals(typeName)) {
			try {
				Object obj = new JSONTokener(val).nextValue();
				if(obj.equals(JSONObject.NULL)) return null;
				if(classForName(typeName).isAssignableFrom(obj.getClass())) return obj;
			} catch (JSONException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		}
		return val;
	}

	public static Class<?> classForName(String typeName) throws ClassNotFoundException {
		if ("int".equals(typeName)) return Integer.class;
		if ("long".equals(typeName)) return Long.class;
		if ("float".equals(typeName)) return Float.class;
		if ("double".equals(typeName)) return Double.class;
		if ("boolean".equals(typeName)) return Boolean.class;
		return Class.forName(typeName);
	}
}
