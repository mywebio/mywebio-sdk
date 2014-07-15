package io.myweb;


import io.myweb.api.Response;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

public class ResponseBuilder {

	public static final String CONTENT_LENGTH = "Content-Length: ";

	public static final String CONTENT_TYPE = "Content-Type: ";

	private static final String EMPTY = "";

	public static final String CRLF = "\r\n";

	public static final int BUFFER_LENGTH = 32 * 1024;

	public void writeResponse(String produces, String srcString, OutputStream os) throws IOException {
		os.write((CONTENT_TYPE + produces + CRLF).getBytes());
		os.write((CONTENT_LENGTH + srcString.length() + CRLF).getBytes());
		os.write(CRLF.getBytes());
		os.write(srcString.getBytes());
		os.close();
	}

	public void writeResponse(String produces, InputStream srcInputStream, OutputStream os) throws IOException {
		os.write((CONTENT_TYPE + produces + CRLF).getBytes());
		os.write(CRLF.getBytes());
		copy(srcInputStream, os);
		os.close();
	}

	public void writeResponse(String produces, Response srcResponse, OutputStream os) throws IOException, JSONException {
		String mime;
		if (srcResponse.getMimeType() == null) {
			mime = produces;
		} else {
			mime = srcResponse.getMimeType();
		}
		os.write((CONTENT_TYPE + mime + CRLF).getBytes());
		writeLengthAndBody(srcResponse.getContentLength(), srcResponse.getBody(), os);
		os.close();
	}

	private void writeLengthAndBody(long userDefinedLength, Object body, OutputStream os) throws IOException, JSONException {
		if (body instanceof String) {
			writeLengthAndBody(userDefinedLength, (String) body, os);
		} else if (body instanceof InputStream) {
			writeLengthAndBody(userDefinedLength, (InputStream) body, os);
		} else if (body instanceof JSONObject) {
			writeLengthAndBody(userDefinedLength, (JSONObject) body, os);
		} else if (body == null) {
			writeLengthAndBody(userDefinedLength, EMPTY, os);
		} else {
			throw new RuntimeException("unsupported type: " + body.getClass());
		}
	}

	private void writeLengthAndBody(long userDefinedLength, String body, OutputStream os) throws IOException {
		writeLengthAndCrlf(userDefinedLength, body.length(), os);
		os.write(body.getBytes());
	}

	private void writeLengthAndBody(long userDefinedLength, InputStream body, OutputStream os) throws IOException {
		writeLengthAndCrlf(userDefinedLength, 0, os);
		copy(body, os);
	}

	private void writeLengthAndBody(long userDefinedLength, JSONObject body, OutputStream os) throws IOException, JSONException {
		String bodyStr = body.toString(2);
		writeLengthAndCrlf(userDefinedLength, bodyStr.length(), os);
		os.write(bodyStr.getBytes());
	}

	private void writeLengthAndCrlf(long userDefinedLength, long bodyLength, OutputStream os) throws IOException {
		if (userDefinedLength > 0) {
			os.write((CONTENT_LENGTH + userDefinedLength + CRLF).getBytes());
		} else if (bodyLength > 0) {
			os.write((CONTENT_LENGTH + bodyLength + CRLF).getBytes());
		}
		os.write(CRLF.getBytes());
	}

	private long copy(InputStream from, OutputStream to) throws IOException {
		byte[] buf = new byte[BUFFER_LENGTH];
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
