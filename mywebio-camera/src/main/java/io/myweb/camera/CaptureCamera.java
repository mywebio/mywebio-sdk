package io.myweb.camera;


import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;

import io.myweb.api.*;
import io.myweb.http.MimeTypes;
import io.myweb.http.Response;

import java.io.FileDescriptor;
import java.io.IOException;

public class CaptureCamera implements MediaRecorder.OnErrorListener {
	private static final int HIDDEN_MPEG_TS = 8;
	private static final String LOG_TAG = CaptureCamera.class.getSimpleName();
	private MediaRecorder mediaRecorder;
	private Camera camera;

	@GET("/camera")
	public Response camera(Context ctx) {
		if (!hasCameraHardware(ctx)) return Response.serviceUnavailable();

		if (getCamera() == null) {
			// Camera is not available (in use or does not exist)
			return Response.serviceUnavailable();
		}

		mediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		getCamera().unlock();
		mediaRecorder.setCamera(getCamera());

		// Step 2: Set source
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
		profile.fileFormat = HIDDEN_MPEG_TS;
		mediaRecorder.setProfile(profile);

		return Response.ok().withContentType(MimeTypes.MIME_VIDEO_MPEG).withBody("STREAM HERE!");
	}

	private Camera getCamera() {
		if (camera == null) camera = Camera.open(); // attempt to get a Camera instance
		return camera;
	}

	private void releaseMediaRecorder() {
		if (mediaRecorder != null) {
			try {
				mediaRecorder.stop();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage(), e);
			}
			mediaRecorder.reset();   // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
			getCamera().lock();           // lock camera for later use
		}
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

	public void writeBody(FileDescriptor fd) {
		// Step 6: Set output file
		mediaRecorder.setOutputFile(fd);
		mediaRecorder.setOnErrorListener(this);
		try {
			// Step 7: Prepare configured MediaRecorder
			mediaRecorder.prepare();
			// Step 8: Start the recording
			mediaRecorder.start();
		} catch (IllegalStateException e) {
			Log.d(LOG_TAG, "IllegalStateException preparing MediaRecorder: ", e);
			releaseMediaRecorder();
		} catch (IOException e) {
			Log.d(LOG_TAG, "IOException preparing MediaRecorder: ", e);
			releaseMediaRecorder();
		}
	}

	@Override
	public void onError(MediaRecorder mediaRecorder, int i, int i2) {
		releaseMediaRecorder();
	}
}
