package io.myweb.camera;


import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.InputStream;

import io.myweb.LocalService;
import io.myweb.api.*;
import io.myweb.http.MimeTypes;
import io.myweb.http.Response;

public class CaptureCamera implements LocalService.ConnectionListener<Streaming>,InputStreamListener {
	private static final String LOG_TAG = CaptureCamera.class.getSimpleName();
	private InputStream is;

	@GET("/camera")
	public Response camera(Context ctx) {
		if (!hasCameraHardware(ctx)) return Response.serviceUnavailable();

//		if (getCamera() == null) {
//			// Camera is not available (in use or does not exist)
//			return Response.serviceUnavailable();
//		}

		Intent captureIntent = new Intent(ctx, CaptureCameraActivity.class);
		captureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK+Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
		ctx.startActivity(captureIntent);

		StreamingService.createConnection(ctx).withConnectionListener(this).open();

		// wait for InputStream
		synchronized (this) {
			try {
				wait(10000);
			} catch (InterruptedException e) {
				// ignore
			}
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
	public void onServiceConnected(Streaming service) {
		Log.d(LOG_TAG, "Web connected to streaming service.");
		service.setInputStreamListener(this);
	}

	@Override
	public void onServiceDisconnected(Streaming service) {

	}

	@Override
	public synchronized void onInputStreamReady(InputStream is) {
		this.is = is;
		notify();
	}
}
