package io.myweb;

import android.net.LocalSocket;
import android.util.Log;

import io.myweb.http.*;

import java.io.*;
import java.net.URI;
import java.util.List;

public class RequestTask implements Runnable {

	private static final String TAG = RequestTask.class.getName();

	private final LocalSocket socket;
	private final RequestProcessor processor;

	public RequestTask(LocalSocket socket, RequestProcessor processor) {
		this.socket = socket;
		this.processor = processor;
	}

	private static final int BUFFER_SIZE = 512;

	private void writeErrorResponse(String requestId, HttpException ex) {
		try {
			ResponseWriter rw = new ResponseWriter(socket.getOutputStream());
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
					writeResponse(processor.processRequest(request.withBody(inputStream)));
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

	private void writeResponse(Response response) throws IOException {
		ResponseWriter rw = new ResponseWriter(socket.getOutputStream());
		try {
			rw.write(response);
		} finally {
			rw.close(response);
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

}
