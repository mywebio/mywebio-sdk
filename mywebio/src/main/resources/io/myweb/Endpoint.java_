package io.myweb;

import android.content.Context;
import android.net.LocalSocket;

import java.io.*;

public abstract class Endpoint {

	public static final int OUTPUT_STREAM_BUFFER_SIZE = 32 * 1024;

	private Context context;

	public Endpoint(Context context) {
		this.context = context;
	}

	protected Context getContext() {
		return context;
	}

	public abstract boolean match(String method, String uri);

	public abstract void invoke(String uri, String request, LocalSocket localSocket, String reqId);

	protected void writeResponseHeaders(OutputStream os, String reqId) throws IOException {
		os.write((reqId + "\n").getBytes());
		os.write("HTTP/1.1 200 OK\r\n".getBytes());
		os.write("Connection: keep-alive\r\n".getBytes());
	}

	protected OutputStream outputStream(LocalSocket localSocket) throws IOException {
		return new BufferedOutputStream(localSocket.getOutputStream(), OUTPUT_STREAM_BUFFER_SIZE);
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
