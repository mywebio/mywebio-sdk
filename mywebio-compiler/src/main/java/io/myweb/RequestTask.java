package io.myweb;

import android.content.Context;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;
import io.myweb.Endpoint;

import java.io.*;
import java.util.List;
import java.util.regex.Pattern;

public class RequestTask implements Runnable {

	private static final String TAG = "myweb.io";

	private static final String INDEX_HTML = "/index.html";
	private static final String SERVICES_JSON = "/services.json";

	private LocalSocket socket;

	private Context context;

	private final List<? extends Endpoint> endpoints;

	public RequestTask(LocalSocket socket, Context context, List<? extends Endpoint> endpoints) {
		this.socket = socket;
		this.context = context;
		this.endpoints = endpoints;
	}

	private static final int BUFFER_SIZE = 32768;
	private static final int REQUEST_ID_HEADER_LENGTH = 37; // 36 characters of UUID + 1 character "\n"

	private static final String HEADERS = "%s\n" +
			"HTTP/1.1 200 OK\n" +
			"Content-Type: video/x-msvideo\n" +
			"Connection: close\n\n";

	private static final String ERROR_RESPONSE = "%s\n" +
			"HTTP/1.1 404 Not Found\n" +
			"Connection: close\n\n" +
			"File %s not found";

	private static final Pattern IMAGE_PATTERN = Pattern.compile("(?:GET|POST|PUT|DELETE) \\/(.+?) HTTP");

	@Override
	public void run() {
		OutputStream outputStream = null;
		String requestId = null;
		String fileName = null;
		try {
			outputStream = new BufferedOutputStream(socket.getOutputStream());
			InputStream inputStream = socket.getInputStream();
			requestId = readRequestId(inputStream);
			Log.d(TAG, "Read request id: " + requestId);
			String request = readRequest(inputStream);
			findAndInvokeEndpoint(request, requestId);
			Log.i(TAG, "Sent response to Web IO Server");
		} catch (Exception e) {
			Log.e(TAG, "Error " + e, e);
			try {
				writeErrorResponse(outputStream, requestId, fileName);
			} catch (IOException err) {
				Log.e(TAG, "Error while write error response " + err, err);
			}
		} finally {
			closeConnection();
		}
	}

	private String readRequestId(InputStream is) throws IOException {
		byte[] buffer = new byte[REQUEST_ID_HEADER_LENGTH];
		if (is.read(buffer, 0, REQUEST_ID_HEADER_LENGTH) != REQUEST_ID_HEADER_LENGTH) {
			throw new RuntimeException("Cannot read request id");
		}
		return new String(buffer).trim();
	}

	private String readRequest(InputStream is) throws IOException {
		int length;
		byte[] buffer = new byte[BUFFER_SIZE];
		String result = "";
		while ((length = is.read(buffer)) != -1) {
			result = new String(buffer, 0, length);
			Log.d(TAG, "Read request from server (" + length + ") " + result);
		}
		Log.d(TAG, "Finish read request from server");
		return result;
	}

	private void writeErrorResponse(OutputStream out, String requestId, String fileName) throws IOException {
		byte[] response = String.format(ERROR_RESPONSE, requestId, fileName).getBytes();
		Log.d(TAG, "Write error response " + new String(response));
		out.write(response, 0, response.length);
		out.close();
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

	public void findAndInvokeEndpoint(final String request, final String reqId) throws Exception {
		String firstLine = request.substring(0, request.indexOf("\n"));
		String[] split = firstLine.split(" ");
		String method = split[0];
		String uri = split[1];
		String effectiveUri = uri;
		Endpoint endpoint = findEndpoint(method, effectiveUri);
		// TODO think how to handle better default requests (like "/index.html" on "/")
		if (("/".equals(uri) || "".equals(uri)) && endpoint == null) {
			effectiveUri = INDEX_HTML;
			endpoint = findEndpoint(method, effectiveUri);
		}
		if (("/".equals(uri) || "".equals(uri)) && endpoint == null) {
			effectiveUri = SERVICES_JSON;
			endpoint = findEndpoint(method, effectiveUri);
		}
		endpoint.invoke(effectiveUri, request, socket, reqId);
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
