package io.myweb.camera;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.myweb.api.*;
import io.myweb.http.MimeTypes;
import io.myweb.http.Response;

public class CaptureCamera implements InputStreamListener {
	private static final String LOG_TAG = CaptureCamera.class.getSimpleName();
	private volatile InputStream is;
	private CountDownLatch streamReady = new CountDownLatch(1);

	@GET("/camera")
	@BindService("io.myweb.camera.StreamingService :service")
	public Response camera(Context ctx, Streaming service) {
		if (!hasCameraHardware(ctx)) return Response.serviceUnavailable();

		Intent captureIntent = new Intent(ctx, CaptureCameraActivity.class);
		captureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK+Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		ctx.startActivity(captureIntent);

		service.setInputStreamListener(this);

		// wait for InputStream
		try {
			streamReady.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (is == null) {
			return Response.serviceUnavailable();
		}

		Log.d(LOG_TAG, "Web streaming ...");
		return Response.ok().withContentType(MimeTypes.MIME_VIDEO_MPEG).withBody(is);
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

	@Override
	public void onInputStreamReady(InputStream is) {
		this.is = is;
		streamReady.countDown();
	}
}
