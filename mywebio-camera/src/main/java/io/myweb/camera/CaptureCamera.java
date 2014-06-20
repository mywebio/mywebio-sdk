package io.myweb.camera;


import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.util.Log;
import io.myweb.api.GET;
import io.myweb.api.Produces;

import java.io.FileDescriptor;
import java.io.IOException;

public class CaptureCamera {

	@GET("/camera")
	@Produces("video/mp4")
	public String camera(Context ctx, FileDescriptor fd) {
		if (hasCameraHardware(ctx)) {
			Camera c = null;
			try {
				c = Camera.open(); // attempt to get a Camera instance
			}
			catch (Exception e){
				// Camera is not available (in use or does not exist)
				return "";
			}

			MediaRecorder mMediaRecorder = new MediaRecorder();

			// Step 1: Unlock and set camera to MediaRecorder
			c.unlock();
			mMediaRecorder.setCamera(c);

			// Step 2: Set sources
//			mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
			mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

			// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
			mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));

			// Step 4: Set output file
//			mMediaRecorder.setOutputFile(request.buildResponseWithFd().getFileDescriptor());
			mMediaRecorder.setOutputFile(fd);

			// Step 5: Set the preview output
//			mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());

			// Step 6: Prepare configured MediaRecorder
			try {
				mMediaRecorder.prepare();
			} catch (IllegalStateException e) {
				Log.d("CaptureCamera", "IllegalStateException preparing MediaRecorder: ", e);
				releaseMediaRecorder(mMediaRecorder, c);
//				return false;
				return "";
			} catch (IOException e) {
				Log.d("CaptureCamera", "IOException preparing MediaRecorder: ", e);
				releaseMediaRecorder(mMediaRecorder, c);
//				return false;
				return "";
			}
			mMediaRecorder.start();
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return "";
		}
		return "";
	}

	private void releaseMediaRecorder(MediaRecorder mr, Camera c){
		if (mr != null) {
			mr.reset();   // clear recorder configuration
			mr.release(); // release the recorder object
//			mMediaRecorder = null;
			c.lock();           // lock camera for later use
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
}
