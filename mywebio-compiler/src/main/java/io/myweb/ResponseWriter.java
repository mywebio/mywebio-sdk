package io.myweb;

import android.net.LocalSocket;

import org.json.JSONObject;

import io.myweb.api.MimeTypes;
import io.myweb.api.Response;

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
		if (!closed) {
			if (response.getContentType() == null) response.withContentType(produces);
			if (response.hasCallback()) writeCallback(response);
			else if (response.getBody() instanceof InputStream) writeInputStream(response);
			else writeObject(response);
			os.flush();
		}
	}

	private void writeCallback(Response response) throws IOException {
		os.write(response.toString().getBytes());
		os.flush();
		response.writeBody(socket.getFileDescriptor());
	}

	private void writeObject(Response response) throws IOException {
		byte[] body = response.getBody().toString().getBytes();
		response.withLength(body.length);
		os.write(response.toString().getBytes());
		os.write(body);
	}

	private void writeInputStream(Response response) throws IOException {
		InputStream is = (InputStream) response.getBody();
		os.write(response.toString().getBytes());
		copy(is, os);
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

	private long copy(InputStream from, OutputStream to) throws IOException {
		byte[] buf = new byte[BUFFER_LENGTH];
		long total = 0;
		while (true) {
			int r = from.read(buf);
			if (r == -1 || Thread.interrupted()) {
				break;
			}
			to.write(buf, 0, r);
			total += r;
		}
		return total;
	}
}
