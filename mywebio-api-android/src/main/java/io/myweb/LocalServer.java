package io.myweb;

import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

public class LocalServer implements Runnable {
	public static final String LOG_TAG = LocalServer.class.getName();
	private final WebContext context;
	private final ExecutorService executor;

	private LocalServerSocket serverSocket;

	private volatile boolean closed;

	public LocalServer(WebContext context, ExecutorService executor) {
		this.context = context;
		this.executor = executor;
	}

	public boolean isClosed() {
		return closed;
	}

	@Override
	public void run() {
		closed = false;
		mainLoop();
		Log.i(LOG_TAG, "Server socket has been shutdown. Exiting normally.");
	}

	private void openLocalServerSocket(String packageName) {
		try {
			if (serverSocket == null || serverSocket.getFileDescriptor() == null) {
				String socketName = "/tmp/" + packageName;
				Log.d(LOG_TAG, "opening local socket server: " + socketName);
				serverSocket = new LocalServerSocket("/tmp/" + packageName);
			}
			Log.d(LOG_TAG, "Binding localServerSocket " + serverSocket);
		} catch (IOException e) {
			Log.e(LOG_TAG, "Error in local socket server", e);
		}
	}

	private void mainLoop() {
		openLocalServerSocket(context.getContext().getPackageName());
		while (!closed) {
			try {
				LocalSocket ls = serverSocket.accept();
//				Log.d(LOG_TAG, "new connection on local socket");
				RequestTask worker = new RequestTask(ls, context.getRequestProcessor());
				executor.execute(worker);
			} catch (IOException e) {
				Log.e(LOG_TAG, "Error while handling request by App: " + e, e);
			}
		}
		Log.d(LOG_TAG, "shutting down server socket");
		shutdown();
	}

	public void shutdown() {
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			serverSocket = null;
		}
		closed = true;
	}
}
