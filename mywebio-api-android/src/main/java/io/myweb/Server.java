package io.myweb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
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

	private final List<Endpoint.Info> endpointList;

	private final AssetLengthInfo assetLengthInfo;

	private final List<? extends Endpoint> endpoints;

	private final Map<String, InternalServiceConnection> serviceMap;

	private class InternalServiceConnection implements ServiceConnection {
		private static final int TIMEOUT = 60000;
		private final Context context;
		private CountDownLatch serviceConnected = new CountDownLatch(1);
		private volatile IBinder service;
		private InternalCancelableTask unbindTask;

		private class InternalCancelableTask implements Runnable {
			private boolean canceled = false;

			@Override
			public void run() {
				synchronized (this) {
					try {
						wait(TIMEOUT);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					if (!canceled) context.unbindService(InternalServiceConnection.this);
				}
			}

			public synchronized void cancel() {
				canceled = true;
				notify();
			}
		}

		public InternalServiceConnection(Context ctx, Class<?> c) {
			this(ctx, new ComponentName(ctx.getPackageName(), c.getName()));
		}

		public InternalServiceConnection(Context ctx, ComponentName name) {
			context = ctx;
			serviceMap.put(name.getClassName(), this);
			Intent intent = new Intent();
			intent.setComponent(name);
			context.bindService(intent, this, Context.BIND_AUTO_CREATE);
		}

		private void scheduleUnbindTask() {
			if (unbindTask!=null) unbindTask.cancel();
			unbindTask = new InternalCancelableTask();
			workerExecutorService.submit(unbindTask);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			this.service = service;
			serviceConnected.countDown();
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			service = null;
			serviceMap.remove(name.getClassName());
			serviceConnected.countDown();
		}

		public Object getServiceObject() {
			try {
				serviceConnected.await();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if (service != null) {
				scheduleUnbindTask();
				if (service instanceof LocalService.Binder)
					return ((LocalService.Binder) service).getService();
			}
			return service;
		}
	}

	public Server(Context context, List<Endpoint.Info> endpointList, AssetLengthInfo info) {
		this.context = context;
		this.endpointList = endpointList;
		assetLengthInfo = info;
		this.endpoints = createEndpoints();
		this.workerExecutorService = new ThreadPoolExecutor(2, 16, 60, TimeUnit.SECONDS,
				new SynchronousQueue<Runnable>(), ThreadFactories.newWorkerThreadFactory());
		serviceMap = Collections.synchronizedMap(new HashMap<String, InternalServiceConnection>());
	}

	public Context getContext() {
		return context;
	}

	public long getAssetLength(String path) {
		return assetLengthInfo.getAssetLength(path);
	}

	public List<Endpoint.Info> getEndpointList() {
		return endpointList;
	}

	public Object bindService(ComponentName name) {
		InternalServiceConnection connection = serviceMap.get(name.getClassName());
		if (connection == null) connection = new InternalServiceConnection(context, name);
		return connection.getServiceObject(); // awaits connection
	}

	private List<? extends Endpoint> createEndpoints() {
		List<Endpoint> list = new LinkedList<Endpoint>();
		for (Endpoint.Info info: endpointList) {
			try {
				Endpoint ep = (Endpoint) info.getImplementingClass().getConstructor(Server.class).newInstance(this);
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
