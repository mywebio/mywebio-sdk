package android.app;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;

public abstract class Service extends Context {

	public static final int START_NOT_STICKY = 2;

	public abstract int onStartCommand(Intent intent, int flags, int startId);

	public void onCreate() {

	}

	public abstract void onDestroy();

	public abstract IBinder onBind(Intent intent);
}
