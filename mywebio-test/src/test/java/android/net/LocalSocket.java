package android.net;

import io.myweb.test.support.LocalWire;

import java.io.*;

public class LocalSocket {

	private InputStream inputStream;

	private OutputStream outputStream;

	public LocalSocket() {
		this(null, null);
	}

	public LocalSocket(InputStream inputStream, OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	public OutputStream getOutputStream() throws IOException {
		return outputStream;
	}

	public InputStream getInputStream() throws IOException {
		return inputStream;
	}

	public FileDescriptor getFileDescriptor() {
        return new FileDescriptor();
    }

	public void shutdownOutput() throws IOException {

	}

	public void close() throws IOException {

	}

	public int getSendBufferSize() throws IOException {
		return 1111;
	}

	public void connect(LocalSocketAddress localSocketAddress) throws IOException {
		LocalWire.ClientsSideStreams clientsSideStreams = LocalWire.getInstance().connect();
		inputStream = clientsSideStreams.getInputStream();
		outputStream = clientsSideStreams.getOutputStream();
	}
}
