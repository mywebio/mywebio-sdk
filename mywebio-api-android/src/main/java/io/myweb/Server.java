package io.myweb;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Server implements Runnable {

	public static final String TAG = "myweb.io";

	private  LocalServerSocket serverSocket;

	private Context context;

	private volatile boolean closed = false;

	private ExecutorService workerExecutorService;

	private final Map<Endpoint.MethodAndUri, Class> endpointRegistry;

	private final AssetLengthInfo assetLengthInfo;

	private final List<? extends Endpoint> endpoints;

	public Server(Context context, Map<Endpoint.MethodAndUri, Class> registry, AssetLengthInfo info) {
		this.context = context;
		this.endpointRegistry = registry;
		assetLengthInfo = info;
		this.endpoints = createEndpoints();
		this.workerExecutorService = new ThreadPoolExecutor(2, 16, 60, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), ThreadFactories.newWorkerThreadFactory());
	}

	public Context getContext() {
		return context;
	}

	public long getAssetLength(String path) {
		return assetLengthInfo.getAssetLength(path);
	}

	public Map<Endpoint.MethodAndUri, Class> getEndpointRegistry() {
		return endpointRegistry;
	}


	private List<? extends Endpoint> createEndpoints() {
		List<Endpoint> list = new LinkedList<Endpoint>();
		for (Class c: endpointRegistry.values()) {
			try {
				Endpoint ep = (Endpoint) c.getConstructor(Server.class).newInstance(this);
				System.out.println(ep.httpMethod().toString()+" "+ep.originalPath()+" instantiated!");
				list.add(ep);
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
		return list;
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
		RequestTask worker = new RequestTask(socket, endpoints);
		workerExecutorService.execute(worker);
	}

	private void shutdownAllTasks() {
		Log.d(TAG, "shutting down all server tasks");
		workerExecutorService.shutdown();
		try {
			workerExecutorService.awaitTermination(1, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	public synchronized void shutdown() {
		try {
			shutdownAllTasks();
			Log.d(TAG, "shutting down server socket");
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
