package io.myweb;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Service extends android.app.Service {

	public static final String TAG = "mywebio";

	private ExecutorService executorService;

	private Server server;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.d(TAG, "Receive intent from server");
		return android.app.Service.START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		Log.d(TAG, "creating myweb.io service");
		executorService = Executors.newSingleThreadExecutor(ThreadFactories.newServerThreadFactory());
		server = new Server(this);
		executorService.submit(server);
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "destroying myweb.io service");
		server.shutdown();
		executorService.shutdown();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

}
