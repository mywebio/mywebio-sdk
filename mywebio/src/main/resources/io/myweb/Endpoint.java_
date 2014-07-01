package io.myweb;

import android.net.LocalSocket;

import java.io.*;

public abstract class Endpoint {

	public abstract boolean match(String method, String uri);

	public abstract void invoke(String uri, String request, LocalSocket localSocket, String reqId);

	protected void writeResponseHeaders(OutputStream os, String reqId) throws IOException {
		os.write((reqId + "\n").getBytes());
		os.write("HTTP/1.1 200 OK\r\n".getBytes());
		os.write("Connection: keep-alive\r\n".getBytes());
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
