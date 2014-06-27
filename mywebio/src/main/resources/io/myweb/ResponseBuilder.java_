package io.myweb;


import io.myweb.api.HttpResponse;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

public class ResponseBuilder {

	public static final String CONTENT_LENGTH = "Content-Length: ";

	public static final String CONTENT_TYPE = "Content-Type: ";

	public static final String CRLF = "\r\n";

	public static final int BUFFER_LENGTH = 32 * 1024;

	public void writeResponse(String srcString, String produces, OutputStream os) throws IOException {
		os.write((CONTENT_TYPE + produces + CRLF).getBytes());
		os.write((CONTENT_LENGTH + srcString.length() + CRLF).getBytes());
		os.write(CRLF.getBytes());
		os.write(srcString.getBytes());
		os.close();
	}

	public void writeResponse(InputStream srcInputStream, String produces, OutputStream os) throws IOException {
		os.write((CONTENT_TYPE + produces + CRLF).getBytes());
		os.write(CRLF.getBytes());
		copy(srcInputStream, os);
		os.close();
	}

	public void writeResponse(HttpResponse srcHttpResponse, String produces, OutputStream os) throws IOException, JSONException {
		String mime;
		if (srcHttpResponse.getMimeType() == null) {
			mime = produces;
		} else {
			mime = srcHttpResponse.getMimeType();
		}
		os.write((CONTENT_TYPE + mime + CRLF).getBytes());
		writeLengthAndBody(srcHttpResponse.getContentLength(), srcHttpResponse.getBody(), os);
		os.close();
	}

	private void writeLengthAndBody(long explicitLength, Object body, OutputStream os) throws IOException, JSONException {
		if (body instanceof String) {
			writeLengthAndBody(explicitLength, (String) body, os);
		} else if (body instanceof InputStream) {
			writeLengthAndBody(explicitLength, (InputStream) body, os);
		} else if (body instanceof JSONObject) {
			writeLengthAndBody(explicitLength, (JSONObject) body, os);
		} else {
			throw new RuntimeException("unsupported type: " + body.getClass());
		}
	}

	private void writeLengthAndBody(long explicitLength, String body, OutputStream os) throws IOException {
		if (explicitLength > 0) {
			os.write((CONTENT_LENGTH + explicitLength + CRLF).getBytes());
		} else {
			os.write((CONTENT_LENGTH + body.length() + CRLF).getBytes());
		}
		os.write(CRLF.getBytes());
		os.write(body.getBytes());
	}

	private void writeLengthAndBody(long explicitLength, InputStream body, OutputStream os) throws IOException {
		if (explicitLength > 0) {
			os.write((CONTENT_LENGTH + explicitLength + CRLF).getBytes());
		}
		os.write(CRLF.getBytes());
		copy(body, os);
	}

	private void writeLengthAndBody(long explicitLength, JSONObject body, OutputStream os) throws IOException, JSONException {
		String bodyStr = body.toString(2);
		if (explicitLength > 0) {
			os.write((CONTENT_LENGTH + explicitLength + CRLF).getBytes());
		} else {
			os.write((CONTENT_LENGTH + bodyStr.length() + CRLF).getBytes());
		}
		os.write(CRLF.getBytes());
		os.write(bodyStr.getBytes());
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
