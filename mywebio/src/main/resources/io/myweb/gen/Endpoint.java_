package io.myweb.gen;

import android.net.LocalSocket;

import java.io.*;
import java.util.regex.Pattern;

public abstract class Endpoint {

	public abstract String httpMethod();

	public abstract String originalPath();

	public abstract Pattern matcher();

	public abstract boolean match(String method, String uri);

	public abstract FormalParam[] formalParams();

	public abstract ActualParam[] actualParams(String uri, String request);

	public abstract void invoke(String uri, String request, LocalSocket localSocket, String reqId);

	protected void writeResponseHeaders(OutputStream os, String reqId) throws IOException {
		os.write((reqId + "\n").getBytes());
		os.write("HTTP/1.1 200 OK\r\n".getBytes());
		os.write("Connection: keep-alive\r\n".getBytes());
	}

	public abstract class Response {
		public abstract String contentType();
		public abstract long length();
		public abstract InputStream inputStream();
	}

	public interface FormalParam {
		int id();
		String type();
		String name();
	}

	public interface ActualParam {
		Object val();
	}

	protected long copy(InputStream from, OutputStream to) throws IOException {
		byte[] buf = new byte[32 * 1024];
		long total = 0;
		while (true) {
			int r = from.read(buf);
			if (r == -1) {
				break;
			}
			to.write(buf, 0, r);
			total += r;
		}
		return total;
	}
}
