package android.net;

import io.myweb.test.support.LocalWire;

import java.io.FileDescriptor;
import java.io.IOException;

public class LocalServerSocket {

	private LocalServerSocket() {}

	public LocalServerSocket(String name) throws IOException {
	}

	public LocalSocket accept() throws IOException {
		try {
			return LocalWire.getInstance().waitForConnection();
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
	}

	public void close() throws IOException {

	}

	public FileDescriptor getFileDescriptor() {
		return new FileDescriptor();
	}
}
