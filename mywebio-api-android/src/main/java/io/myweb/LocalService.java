package io.myweb;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import java.lang.ref.WeakReference;

public class LocalService<T> extends Service {

	/**
	 * Weak reference binder to avoid memory leaks in service binders (circular reference)
	 *
	 */
	public static class Binder<T> extends android.os.Binder {
		private WeakReference<T> srvc;

		public Binder(T service) {
			super();
			srvc = new WeakReference<T>(service);
		}

		public T getService() {
			return srvc.get();
		}
	}

	public static interface ConnectionListener<T> {
		public void onServiceConnected(T service);
		public void onServiceDisconnected(T service);
	}

	public static class Connection<T> {
		protected volatile T service;
		protected Context ctx;
		protected ConnectionListener<T> connListener;

		private ServiceConnection mConnection = new ServiceConnection() {
			@Override
			@SuppressWarnings("unchecked")
			public void onServiceConnected(ComponentName className, IBinder binder) {
				if (binder != null) {
					T service = ((Binder<T>) binder).getService();
					Connection.this.service = service;
				}
				if (connListener != null) connListener.onServiceConnected(service);
			}

			@Override
			public void onServiceDisconnected(ComponentName arg0) {
				if (service != null && connListener != null) {
					connListener.onServiceDisconnected(service);
				}
				service = null;
			}
		};

		public Connection(Context ctx, Class<?> serviceClass) {
			this.ctx = ctx;
			Intent intent = new Intent(ctx, serviceClass);
			ctx.bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
		}

		public void close() {
			try {
				ctx.unbindService(mConnection);
				if (connListener != null) connListener.onServiceDisconnected(service);
				service=null;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public T getService() {
			return service;
		}

		public Connection<T> withListener(ConnectionListener<T> listener) {
			connListener = listener;
			return this;
		}
	}

	private final IBinder localBinder;

	@SuppressWarnings("unchecked")
	public LocalService() {
		super();
		localBinder = new Binder<T>((T)this);
	}

	@Override
	public IBinder onBind(Intent intent) {
		return localBinder;
	}

}
