package io.myweb.test.support;

import android.net.LocalSocket;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


public class LocalWire {

	private static volatile LocalWire instance;

	private BlockingQueue<LocalSocket> socketsToAccept = new LinkedBlockingQueue<LocalSocket>();

	public static LocalWire getInstance() {
		synchronized (LocalWire.class) {
			if (instance == null) {
				instance = new LocalWire();
			}
			return instance;
		}
	}

	public ClientsSideStreams connect() throws IOException {
		PipedInputStream clientsInputStream = new PipedInputStream();
		PipedOutputStream serversOutputStream = new PipedOutputStream();
		clientsInputStream.connect(serversOutputStream);

		PipedInputStream serversInputStream = new PipedInputStream();
		PipedOutputStream clientsOutputStream = new PipedOutputStream();
		serversInputStream.connect(clientsOutputStream);

		socketsToAccept.add(new LocalSocket(serversInputStream, serversOutputStream));

		return new ClientsSideStreams(clientsInputStream, clientsOutputStream);
	}

	public LocalSocket waitForConnection() throws InterruptedException {
		return socketsToAccept.take();
	}

	public class ClientsSideStreams {
		private final InputStream inputStream;
		private final OutputStream outputStream;

		ClientsSideStreams(InputStream is, OutputStream os) {
			this.inputStream = is;
			this.outputStream = os;
		}

		public OutputStream getOutputStream() {
			return outputStream;
		}

		public InputStream getInputStream() {
			return inputStream;
		}
	}
}
