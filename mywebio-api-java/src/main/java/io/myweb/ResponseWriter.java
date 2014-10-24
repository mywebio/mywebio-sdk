package io.myweb;

import io.myweb.http.Response;

import java.io.*;

public class ResponseWriter {

	public static final int BUFFER_LENGTH = 32 * 1024;

	private final OutputStream os;

	private boolean closed = false;

	public ResponseWriter(OutputStream os) throws IOException {
		this.os = os;
	}

	public void close() throws IOException {
		close(null);
	}

	public synchronized void close(Response r) throws IOException {
		if (!isClosed()) {
			os.flush();
			closed = true;
			if (r != null) r.onClose();
		}
	}

	public boolean isClosed() {
		return closed;
	}

	public void write(Response response) throws IOException {
		if (!isClosed() && response!=null) {
			if (response.getBody() instanceof InputStream) writeInputStream(response);
			else writeObject(response);
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

	private long copy(InputStream from, OutputStream to, boolean chunkEncode) throws IOException {
		byte[] buf = new byte[BUFFER_LENGTH];
		long total = 0;
		while (true) {
			int r = -1;
			boolean tryAgain = true;
			while (tryAgain) {
				try {
					r = from.read(buf);
				} catch (IOException e) {
					if (e.getMessage().startsWith("Try again")) continue;
					e.printStackTrace();
				}
				tryAgain = false;
			}
			if (r == -1 || Thread.interrupted()) {
				if (chunkEncode) to.write("0\r\n\r\n".getBytes());
				break;
			}
			if(chunkEncode) {
				to.write((Integer.toHexString(r)+"\r\n").getBytes());
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
