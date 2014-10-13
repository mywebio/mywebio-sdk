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
	private final List<Filter> filters;

	public RequestTask(LocalSocket socket, List<? extends Endpoint> endpoints, List<Filter> filters) {
		this.socket = socket;
		this.endpoints = endpoints;
		this.filters = filters;
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
					processRequest(request.withBody(inputStream));
//					Log.i(TAG, "Sent response to Web IO Server");
					keepAlive = request.isKeptAlive();
					// make sure request body has been read
					if (keepAlive) request.readBody();
				} else keepAlive = false;
			}
		} catch (HttpException e) {
			Log.e(TAG, "Error " + e, e);
			writeErrorResponse(requestId, e);
		} catch (IOException e) {
			writeErrorResponse(requestId, new HttpInternalErrorException(e.getMessage(), e));
		} finally {
			closeConnection();
		}
	}

	private Request readRequest(PushbackInputStream is) throws IOException, HttpBadRequestException {
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
				int len = length - idx;
				if (len>0) is.unread(buffer, idx, len);
				break;
			} else {
				sb.append(result);
			}
		}
		return Request.parse(sb.toString());
	}

	private void closeConnection() {
		if (socket != null) {
			try {
				socket.getOutputStream().close();
			} catch (IOException e) {
				// ignore any errors when closing output stream
			}
			try {
				socket.close();
			} catch (IOException e) {
				Log.e(TAG, "Error occurred while closing connection " + e);
			}
		}
	}

	private void processRequest(final Request request) throws HttpException, IOException {
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
			Response response = null;
			try {
				Request filteredRequest = filterBefore(effectiveUri, request);
				response = endpoint.invoke(effectiveUri, filteredRequest);
				response = filterAfter(effectiveUri, response);
				rw.write(response);
			} catch (Throwable t) {
				if (response != null) response.onError(t);
				if (t instanceof HttpException) throw (HttpException) t;
				if (t instanceof IOException) throw (IOException) t;
				throw new HttpInternalErrorException(t.getMessage(), t);
			} finally {
				rw.close(response);
			}
		}
	}

	private Response filterAfter(String effectiveUri, Response response) {
		for (Filter filter: filters) {
			if (filter.matchAfter(effectiveUri)) response = filter.after(response);
		}
		return response;
	}

	private Request filterBefore(String effectiveUri, Request request) {
		for (Filter filter: filters) {
			if (filter.matchBefore(effectiveUri)) request = filter.before(request);
		}
		return request;
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
