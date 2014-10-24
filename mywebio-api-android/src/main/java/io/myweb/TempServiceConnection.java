package io.myweb;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class TempServiceConnection implements ServiceConnection {
	private static final int DEFAULT_TIMEOUT = 60000;
	private static final Map<String, TempServiceConnection> serviceMap =
			Collections.synchronizedMap(new HashMap<String, TempServiceConnection>());
	private final Context context;
	private final int timeout;
	private CountDownLatch serviceConnected = new CountDownLatch(1);
	private volatile Object service;
	private CancelableUnbindTask unbindTask;
	private ExecutorService executor;

	private class CancelableUnbindTask implements Runnable {
		private boolean canceled = false;

		@Override
		public void run() {
			synchronized (this) {
				try {
					wait(timeout);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!canceled) context.unbindService(TempServiceConnection.this);
			}
		}

		public synchronized void cancel() {
			canceled = true;
			notify();
		}

		public synchronized void perform() {
			notifyAll();
		}
	}

	public TempServiceConnection(Context ctx, Class<?> c, ExecutorService executor) {
		this(ctx, c, executor, DEFAULT_TIMEOUT);
	}

	public TempServiceConnection(Context ctx, Class<?> c, ExecutorService executor, int timeout) {
		this(ctx, new ComponentName(ctx.getPackageName(), c.getName()), executor, timeout);
	}

	public TempServiceConnection(Context ctx, ComponentName name, ExecutorService executor) {
		this(ctx, name, executor, DEFAULT_TIMEOUT);
	}

	public TempServiceConnection(Context ctx, ComponentName name, ExecutorService executor, int timeout) {
		context = ctx;
		this.timeout = timeout;
		this.executor = executor;
		serviceMap.put(name.getClassName(), this);
		Intent intent = new Intent();
		intent.setComponent(name);
		context.bindService(intent, this, Context.BIND_AUTO_CREATE);
	}

	private void scheduleUnbindTask() {
		if (unbindTask!=null) unbindTask.cancel();
		if (executor != null) {
			unbindTask = new CancelableUnbindTask();
			executor.submit(unbindTask);
		}
	}

	private void terminate() {
		if (unbindTask != null) unbindTask.perform();
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		if (service instanceof LocalService.Binder)
			this.service =((LocalService.Binder) service).getService();
		else this.service = service;
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
		}
		return service;
	}

	public static TempServiceConnection get(String className) {
		return serviceMap.get(className);
	}

	public static void terminateAll() {
		for (TempServiceConnection conn: serviceMap.values()) {
			conn.terminate();
		}
	}

}
