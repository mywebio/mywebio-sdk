package io.myweb.camera;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.myweb.api.*;
import io.myweb.http.HttpServiceUnavailableException;
import io.myweb.http.MimeTypes;

public class CaptureCamera implements InputStreamListener {
	private static final String LOG_TAG = CaptureCamera.class.getSimpleName();
	private volatile InputStream is;
	private CountDownLatch streamReady = new CountDownLatch(1);

	@GET("/camera")
	@BindService("StreamingService")
	@Produces(MimeTypes.MIME_VIDEO_MPEG)
	public InputStream camera(Context ctx, Streaming streamingService) throws HttpServiceUnavailableException {
		if (!hasCameraHardware(ctx)) throw new HttpServiceUnavailableException("No camera hardware!");

		streamingService.setInputStreamListener(this);

		Intent captureIntent = new Intent(ctx, CaptureCameraActivity.class);
		captureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK+Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		ctx.startActivity(captureIntent);

		if (!waitForInputStream(10)) throw new HttpServiceUnavailableException("No input from the camera!");

		Log.d(LOG_TAG, "Web streaming ...");
		return is;
	}

	private boolean hasCameraHardware(Context context) {
		if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			// this device has a camera
			return true;
		} else {
			// no camera on this device
			return false;
		}
	}

	private boolean waitForInputStream(int seconds) {
		try {
			streamReady.await(seconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return is != null;
	}

	@Override
	public void onInputStreamReady(InputStream is) {
		this.is = is;
		streamReady.countDown();
	}
}
