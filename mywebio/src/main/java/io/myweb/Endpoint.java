package io.myweb;

import android.content.Context;
import android.net.LocalSocket;
import android.util.Log;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Endpoint {

	public static final int OUTPUT_STREAM_BUFFER_SIZE = 32 * 1024;

	private Context context;

	public Endpoint(Context context) {
		this.context = context;
	}

	public abstract void invoke(String uri, String request, LocalSocket localSocket, String reqId);

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
		Matcher m = matcher(uri);
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

	protected FormalParam[] formalParams() {
		return new FormalParam[] {
				new FormalParam() {
					@Override
					public int id() {
						return 0;
					}

					@Override
					public String type() {
						return null;
					}

					@Override
					public String name() {
						return null;
					}
				},
				new FormalParam() {
					@Override
					public int id() {
						return 1;
					}

					@Override
					public String type() {
						return null;
					}

					@Override
					public String name() {
						return null;
					}
				}
		};
	}

	protected ActualParam[] actualParams(String uri, String request) {
		Matcher m = matcher(uri);
		final String[] matchedGroups;
		int groupCount;
		if (m.matches()) {
			groupCount = m.groupCount();
			matchedGroups = new String[groupCount];
			if (groupCount > 0) {
				// TODO support more groups
				matchedGroups[0] = m.group(1);
			}
		} else {
			throw new RuntimeException("Shouldn't happen");
		}
		return buildActualParams(matchedGroups);
	}

	private ActualParam[] buildActualParams(final String[] matchedGroups) {
		ActualParam[] ap = new ActualParam[matchedGroups.length];
		for (int i = 0; i < matchedGroups.length; i++) {
			final int cur = i;
			ap[i] = new ActualParam() {
				@Override
				public Object val() {
					return matchedGroups[cur];
				}
			};
		}
		return ap;
	}

	public interface FormalParam {
		int id();
		String type();
		String name();
	}

	public interface ActualParam {
		Object val();
	}
}
