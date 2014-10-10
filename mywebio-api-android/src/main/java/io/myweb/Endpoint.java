package io.myweb;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import io.myweb.http.*;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Endpoint {

	public static class Info {
		private final Method method;
		private final String uri;
		private final Class implementingClass;

		public Info(Method method, String uri, Class implementingClass) {
			this.method = method;
			this.uri = uri;
			this.implementingClass = implementingClass;
		}

		public Method getMethod() {
			return method;
		}

		public String getUri() {
			return uri;
		}

		public Class getImplementingClass() {
			return implementingClass;
		}
	}

	private Server server;

	public Endpoint(Server server) {
		this.server = server;
	}

	public String produces() {
		return null;
	}

	public abstract Response invoke(String uri, Request request) throws HttpException, IOException;

	protected Server getServer() {
		return server;
	}

	protected String getServiceName() { // overriden in subclasses
		return null;
	}

	protected Object getServiceObject() { // overriden in subclasses
		return null;
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

	protected ActualParam[] actualParams(String uri, Request request, FormalParam[] formalParams, Map<String, String> defaultQueryParams, Map<String, Integer> groupMap, Context ctx) throws HttpException {
		String urlNoQueryParams = urlNoQueryParams(uri);
		Map<String, String> paramsMap = request.getParameterMap();
		Matcher m = matcher(urlNoQueryParams);
		ActualParam[] actualParams = new ActualParam[formalParams.length];
		if (m.matches()) {
			for (FormalParam fp : formalParams) {
				int fpId = fp.getId();
				Class<?> fpClazz;
				try {
					fpClazz = classForName(fp.getTypeName());
				} catch (ClassNotFoundException e) {
					throw new HttpServiceUnavailableException(e.getMessage(), e);
				}
				String fpName = fp.getName();
				if (fpName.equals(getServiceName())) {
					actualParams[fpId] = new ActualParam(fpClazz, getServiceObject());
				} else if (groupMap.containsKey(fpName)) {
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
			throw new HttpServiceUnavailableException("couldn't match URI: '" + uri + "' with pattern '" + getPattern() + "' (BTW, this shouldn't happen...)");
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
