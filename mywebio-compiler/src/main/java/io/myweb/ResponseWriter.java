package io.myweb;

import android.net.LocalSocket;

import org.json.JSONObject;

import io.myweb.http.MimeTypes;
import io.myweb.http.Response;

import java.io.*;

public class ResponseWriter {

	public static final int BUFFER_LENGTH = 32 * 1024;

	private final LocalSocket socket;
	private final OutputStream os;
	private final String produces;

	private boolean closed = false;

	public ResponseWriter(String produces, LocalSocket socket) throws IOException {
		this.produces = produces;
		this.socket = socket;
		os = socket.getOutputStream();
	}

	public ResponseWriter(LocalSocket socket) throws IOException {
		this(MimeTypes.MIME_TEXT_HTML, socket);
	}

	public void close() throws IOException {
		os.flush();
		os.close();
		closed = true;
	}

	public boolean isClosed() {
		return closed;
	}

	public void write(Response response) throws IOException {
		if (!closed && response!=null) {
			if (response.getContentType() == null) response.withContentType(produces);
			if (response.getBody() instanceof InputStream) writeInputStream(response);
			else writeObject(response);
			os.flush();
		}
	}

	private void writeObject(Response response) throws IOException {
		if (response.getBody() != null) {
			byte[] body = response.getBody().toString().getBytes();
			response.withLength(body.length);
			os.write(response.toString().getBytes());
			os.write(body);
		} else {
			response.withLength(0);
			os.write(response.toString().getBytes());
		}
	}

	private void writeInputStream(Response response) throws IOException {
		InputStream is = (InputStream) response.getBody();
		final boolean chunked = !response.hasLength();
		if (chunked) response.withChunks();
		os.write(response.toString().getBytes());
		copy(is, os, chunked);
		is.close();
	}

	public void write(InputStream is) throws IOException {
		write(Response.ok().withBody(is));
	}

	public void write(String text) throws IOException {
		write(Response.ok().withBody(text));
	}

	public void write(JSONObject json) throws IOException {
		write(Response.ok().withBody(json));
	}

	public void write(File file) throws IOException {
		write(Response.ok().withFile(file));
	}

	//TODO old behaviour
	public void writeRequestId(String reqId) throws IOException {
		os.write((reqId + "\n").getBytes());
	}

	private long copy(InputStream from, OutputStream to, boolean chunkEncode) throws IOException {
		byte[] buf = new byte[BUFFER_LENGTH];
		long total = 0;
		while (true) {
			int r = from.read(buf);
			if (r == -1 || Thread.interrupted()) {
				if (chunkEncode) to.write("0\r\n\r\n".getBytes());
				break;
			}
			if(chunkEncode) {
				to.write((Integer.toString(buf.length)+"\r\n").getBytes());
			}
			to.write(buf, 0, r);
			if(chunkEncode) {
				to.write("\r\n".getBytes());
			}
			total += r;
		}
		return total;
	}
}
