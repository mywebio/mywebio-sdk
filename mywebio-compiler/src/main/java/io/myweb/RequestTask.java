package io.myweb;

import android.net.LocalSocket;
import android.util.Log;

import io.myweb.api.Request;
import io.myweb.api.Response;

import java.io.*;
import java.util.List;

public class RequestTask implements Runnable {

	private static final String TAG = "myweb.io";

	private static final String INDEX_HTML = "/index.html";
	private static final String SERVICES_JSON = "/services.json";

	private LocalSocket socket;

	private final List<? extends Endpoint> endpoints;

	public RequestTask(LocalSocket socket, List<? extends Endpoint> endpoints) {
		this.socket = socket;
		this.endpoints = endpoints;
	}

	private static final int BUFFER_SIZE = 512;
	//	private static final int REQUEST_ID_HEADER_LENGTH = 37; // 36 characters of UUID + 1 character "\n"
	private static final String FILE_NOT_FOUND = "File %s not found";

	private void writeNotFoundResponse(OutputStream out, String requestId, String fileName) throws IOException {
		// TODO old behaviour, first line with request id
		out.write((requestId + "\n").getBytes());
		out.write(Response.notFound().toString().getBytes());
		out.write(String.format(FILE_NOT_FOUND, fileName).getBytes());
		out.close();
	}

	private void writeErrorResponse(OutputStream out, String requestId, String msg) throws IOException {
		// TODO old behaviour, first line with request id
		out.write((requestId + "\n").getBytes());
		out.write(Response.internalError().toString().getBytes());
		out.write(msg.getBytes());
		out.close();
	}

	@Override
	public void run() {
		OutputStream outputStream = null;
		String fileName = null;
		Request request = null;
		try {
			outputStream = new BufferedOutputStream(socket.getOutputStream());
			PushbackInputStream inputStream = new PushbackInputStream(socket.getInputStream());
			request = readRequest(inputStream);
			findAndInvokeEndpoint(request);
			Log.i(TAG, "Sent response to Web IO Server");
		} catch (ClassNotFoundException e) {
			try {
				writeNotFoundResponse(outputStream, request.getId(), fileName);
			} catch (IOException err) {
				Log.e(TAG, "Error while write error response " + err, err);
			}
		} catch (Exception e) {
			Log.e(TAG, "Internal error " + e, e);
			try {
				writeErrorResponse(outputStream, request.getId(), e.getMessage());
			} catch (IOException err) {
				Log.e(TAG, "Error while write error response " + err, err);
			}
		} finally {
			closeConnection();
		}
	}

	private Request readRequest(PushbackInputStream is) throws IOException {
		int length;
		byte[] buffer = new byte[BUFFER_SIZE];
		StringBuilder sb = new StringBuilder();
		while ((length = is.read(buffer)) != -1) {
			// find double EOLs
			String result = new String(buffer, 0, length);
			int idx = result.indexOf("\r\n\r\n");
			if (idx >= 0) {
				sb.append(result.substring(0, idx));
				idx += 4;
				is.unread(buffer, idx, buffer.length - idx);
				break;
			} else {
				sb.append(result);
			}
		}
		//TODO old behaviour, first line contains request id
		String[] lines = sb.toString().split("\n", 2);
		Request result = Request.parse(lines[1]).withId(lines[0].trim()).withBody(is);
		Log.d(TAG, "Finish read request from server");
		return result;
	}

	private void closeConnection() {
		if (socket != null) {
			try {
				socket.shutdownOutput();
				socket.close();
			} catch (IOException e) {
				Log.e(TAG, "Error occurred while closing connection " + e);
			}
		}
	}

	public void findAndInvokeEndpoint(final Request request) throws Exception {
		String uri = request.getURI().toString();
		String effectiveUri = uri;
		Endpoint endpoint = findEndpoint(request.getMethod().toString(), effectiveUri);
		// TODO think how to handle better default requests (like "/index.html" on "/")
		if (("/".equals(uri) || "".equals(uri)) && endpoint == null) {
			effectiveUri = INDEX_HTML;
			endpoint = findEndpoint(request.getMethod().toString(), effectiveUri);
		}
		if (("/".equals(uri) || "".equals(uri)) && endpoint == null) {
			effectiveUri = SERVICES_JSON;
			endpoint = findEndpoint(request.getMethod().toString(), effectiveUri);
		}
		if (endpoint == null)
			throw new ClassNotFoundException("Endpoint class for " + uri + " not found!");
		endpoint.invoke(effectiveUri, request, socket);
	}

	private Endpoint findEndpoint(String method, String uri) {
		for (Endpoint endpoint : endpoints) {
			if (endpoint.match(method, uri)) {
				return endpoint;
			}
		}
		return null;
	}
}
