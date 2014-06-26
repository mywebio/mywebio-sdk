package io.myweb;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class Server implements Runnable {

	public static final String TAG = "myweb.io";

	private  LocalServerSocket serverSocket;

	private Context context;

	private volatile boolean closed = false;

	private ExecutorService workerExecutorService;

	public Server(Context context) {
		this.context = context;
		this.workerExecutorService = new ThreadPoolExecutor(2, 16, 60, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), ThreadFactories.newWorkerThreadFactory());
	}

	@Override
	public void run() {
		openLocalServerSocket(context.getPackageName());
		mainLoop();
		Log.i(TAG, "Server socket has been shutdown. Exiting normally.");
	}

	private void openLocalServerSocket(String packageName) {
		try {
			if (serverSocket == null || serverSocket.getFileDescriptor() == null) {
				String socketName = "/tmp/" + packageName;
				Log.d(TAG, "opening local socket server: " + socketName);
				serverSocket = new LocalServerSocket("/tmp/" + packageName);
			}
			Log.d(TAG, "Binding localServerSocket " + serverSocket);
		} catch (IOException e) {
			Log.e(TAG, "Error in local socket server", e);
		}
	}

	private void mainLoop() {
		while (!closed) {
			try {
				LocalSocket ls = serverSocket.accept();
				Log.d(TAG, "new connection on local socket");
				handleMessage(ls);
			} catch (IOException e) {
				Log.e(TAG, "Error while handling request by App: " + e, e);
			}
		}
	}

	private void handleMessage(LocalSocket socket) throws IOException {
		Log.d(TAG, "send buffer size " + socket.getSendBufferSize());
		RequestTask worker = new RequestTask(socket, context, instantiateEndpoints());
		workerExecutorService.execute(worker);
	}

	// describe why hack with reflection
	public List<? extends Endpoint> instantiateEndpoints() {
		String className = "io.myweb.EndpointContainer";
		List<? extends Endpoint> endpoints;
		try {
			Class c = Class.forName(className);
			Object endpointContainer = c.newInstance();
			Method getAllEndpointsMethod = c.getDeclaredMethod("getAllEndpoints", Context.class);
			endpoints = (List<? extends Endpoint>) getAllEndpointsMethod.invoke(endpointContainer, context);
		} catch (Exception e) {
			// shouldn't happen in properly built application (programmer error)
			Log.d(TAG, "cannot instantiate endpoints", e);
			endpoints = Collections.emptyList();
		}
		return endpoints;
	}

	public synchronized void shutdown() {
		try {
			Log.d(TAG, "shutting down server socket");
			// TODO gk implement graceful shutdown (with workers shutdown)
			serverSocket.close();
			serverSocket = null;
		} catch (IOException e) {
			Log.w(TAG, "problem when closing server socket", e);
		} finally {
			if (serverSocket != null) {
				try {
					serverSocket.close();
					serverSocket = null;
				} catch (IOException e) {
					Log.w(TAG, "cannot close server socket", e);
				}
			}
		}
		closed = true;
	}
}
