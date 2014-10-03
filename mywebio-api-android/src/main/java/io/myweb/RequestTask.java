package io.myweb;

import android.net.LocalSocket;
import android.util.Log;

import io.myweb.http.*;

import java.io.*;
import java.util.List;

public class RequestTask implements Runnable {

	private static final String TAG = "myweb.io";

	private static final String INDEX_HTML = "/index.html";
	private static final String SERVICES_JSON = "/services.json";

	private final LocalSocket socket;

	private final List<? extends Endpoint> endpoints;

	public RequestTask(LocalSocket socket, List<? extends Endpoint> endpoints) {
		this.socket = socket;
		this.endpoints = endpoints;
	}

	private static final int BUFFER_SIZE = 512;

	private void writeErrorResponse(String requestId, HttpException ex) {
		try {
			ResponseWriter rw = new ResponseWriter(MimeTypes.MIME_TEXT_HTML, socket.getOutputStream());
			rw.write(Response.error(ex).withId(requestId).withBody(preformatted(Log.getStackTraceString(ex))));
			rw.close();
		} catch (IOException err) {
			Log.e(TAG, "Error while writing error response " + err, err);
		}
	}

	private static String preformatted(String text) {
		return "<pre>"+text+"</pre>";
	}

	@Override
	public void run() {
		String requestId = null;
		boolean keepAlive = true;
		try {
			PushbackInputStream inputStream;
			try {
				inputStream = new PushbackInputStream(socket.getInputStream(), BUFFER_SIZE);
			} catch (IOException e) {
				throw new HttpBadRequestException(e.getMessage(), e);
			}
			while (keepAlive) {
				Request request;
				try {
					request = readRequest(inputStream);
				} catch (IOException e) {
					throw new HttpBadRequestException(e.getMessage(), e);
				}
				if (request != null) {
					requestId = request.getId();
					findAndInvokeEndpoint(request);
					Log.i(TAG, "Sent response to Web IO Server");
					keepAlive = request.isKeptAlive();
				} else keepAlive = false;
			}
		} catch (HttpException e) {
			Log.e(TAG, "Error " + e, e);
			writeErrorResponse(requestId, e);
		} catch (Throwable t) {
			writeErrorResponse(requestId, new HttpInternalErrorException(t.getMessage(), t));
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
				int len = buffer.length - idx;
				if (len>0) is.unread(buffer, idx, len);
				break;
			} else {
				sb.append(result);
			}
		}
		//TODO old behaviour, first line contains request id
		String[] lines = sb.toString().split("\n", 2);
		if (lines.length==2) {
			Request result = Request.parse(lines[1]).withId(lines[0].trim()).withBody(is);
			Log.d(TAG, "Finish read request from server");
			return result;
		}
		return null; // no request
	}

	private void closeConnection() {
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException e) {
				Log.e(TAG, "Error occurred while closing connection " + e);
			}
		}
	}

	public void findAndInvokeEndpoint(final Request request) throws HttpException, IOException {
		String uri = request.getURI().toString();
		String effectiveUri = uri;
		Endpoint endpoint = findEndpoint(request.getMethod(), effectiveUri);
		// TODO think how to handle better default requests (like "/index.html" on "/")
		if (("/".equals(uri) || "".equals(uri)) && endpoint == null) {
			effectiveUri = INDEX_HTML;
			endpoint = findEndpoint(request.getMethod(), effectiveUri);
		}
		if (("/".equals(uri) || "".equals(uri)) && endpoint == null) {
			effectiveUri = SERVICES_JSON;
			endpoint = findEndpoint(request.getMethod(), effectiveUri);
		}
		if (endpoint == null)
			throw new HttpNotFoundException("Not found " + uri);
		else {
			ResponseWriter rw = new ResponseWriter(endpoint.produces(), socket.getOutputStream());
			endpoint.invoke(effectiveUri, request, rw);
			rw.close();
		}
	}

	private Endpoint findEndpoint(Method method, String uri) {
		for (Endpoint endpoint : endpoints) {
			if (endpoint.match(method, uri)) {
				return endpoint;
			}
		}
		return null;
	}
}
