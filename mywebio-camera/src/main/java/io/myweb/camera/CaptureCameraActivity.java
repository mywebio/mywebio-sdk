package io.myweb.camera;

import android.app.Activity;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.Window;

import com.android.grafika.AspectFrameLayout;
import com.android.grafika.CameraUtils;

import java.io.IOException;

import io.myweb.LocalService;

public class CaptureCameraActivity extends Activity implements LocalService.ConnectionListener<Streaming> {
	private static final String LOG_TAG = CaptureCameraActivity.class.getName();

	private Camera mCamera;
	private TextureView mTextureView;
	private int mCameraPreviewWidth;
	private int mCameraPreviewHeight;
	private volatile boolean portraitMode = false;
	private LocalService.Connection<Streaming> streamingConnection;
	private SurfaceTexture mSurfaceTexture;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_capture_camera);

		mTextureView = (TextureView) findViewById(R.id.textureView);
		mTextureView.setSurfaceTextureListener(createSurfaceTextureListener());
		mTextureView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				finish();
			}
		});

		streamingConnection = StreamingService.createConnection(this).withConnectionListener(this);
		streamingConnection.open();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		streamingConnection.close();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if(streamingConnection.getService()!=null) {
			streamingConnection.getService().startStreaming(mCamera);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if(streamingConnection.getService()!=null) {
			streamingConnection.getService().stopStreaming();
		}
	}

	private TextureView.SurfaceTextureListener createSurfaceTextureListener() {
		return new TextureView.SurfaceTextureListener() {

			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
				portraitMode = (width < height);
				onCameraSurfaceTextureAvailable(surfaceTexture, 1280, 720);
			}

			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int width, int height) {
			}

			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
				return onCameraSurfaceTextureDestroyed(surfaceTexture);
			}

			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
			}

		};
	};

	private void onCameraSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		mSurfaceTexture = surface;
		openCamera(width, height);

		// Set the preview aspect ratio.
		AspectFrameLayout layout = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);
		layout.setAspectRatio((double) mCameraPreviewWidth / mCameraPreviewHeight);

		try {
			mCamera.setPreviewTexture(surface);
			mCamera.startPreview();
		} catch (IOException ex) {
			Log.e(LOG_TAG, ex.getMessage(), ex);
		}
		new Handler(Looper.getMainLooper()).post(new Runnable() {
			@Override
			public void run() {
				streamingConnection.getService().startStreaming(mCamera);
			}
		});
	}


	private boolean onCameraSurfaceTextureDestroyed(SurfaceTexture surface) {
		mSurfaceTexture = null;
		releaseCamera();
		return true;
	}

	/**
	 * This is basicly copied as-is from grafika example
	 *
	 * Opens a camera, and attempts to establish preview mode at the specified width and height.
	 * <p>
	 * Sets mCameraPreviewWidth and mCameraPreviewHeight to the actual width/height of the preview.
	 */
	private void openCamera(int desiredWidth, int desiredHeight) {
		if (mCamera != null) {
			throw new RuntimeException("Camera already initialized");
		}
		Camera.CameraInfo info = new Camera.CameraInfo();
// Try to find a front-facing camera (e.g. for videoconferencing).
		int numCameras = Camera.getNumberOfCameras();
		for (int i = 0; i < numCameras; i++) {
			Camera.getCameraInfo(i, info);
			if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				mCamera = Camera.open(i);
				if(portraitMode) mCamera.setDisplayOrientation(90);
				break;
			}
		}
		if (mCamera == null) {
			Log.d(LOG_TAG, "No front-facing camera found; opening default");
			mCamera = Camera.open(); // opens first back-facing camera
		}
		if (mCamera == null) {
			throw new RuntimeException("Unable to open camera");
		}
		Camera.Parameters parms = mCamera.getParameters();
		CameraUtils.choosePreviewSize(parms, desiredWidth, desiredHeight);
// Give the camera a hint that we're recording video. This can have a big
// impact on frame rate.
		parms.setRecordingHint(true);
// leave the frame rate set to default
		mCamera.setParameters(parms);
		int[] fpsRange = new int[2];
		Camera.Size mCameraPreviewSize = parms.getPreviewSize();
		parms.getPreviewFpsRange(fpsRange);
		String previewFacts = mCameraPreviewSize.width + "x" + mCameraPreviewSize.height;
		if (fpsRange[0] == fpsRange[1]) {
			previewFacts += " @" + (fpsRange[0] / 1000.0) + "fps";
		} else {
			previewFacts += " @[" + (fpsRange[0] / 1000.0) +
					" - " + (fpsRange[1] / 1000.0) + "] fps";
		}
		Log.d(LOG_TAG, previewFacts);
//		TextView text = (TextView) findViewById(R.id.cameraParams_text);
//		text.setText(previewFacts);
		if(portraitMode) {
			mCameraPreviewWidth = mCameraPreviewSize.height;
			mCameraPreviewHeight = mCameraPreviewSize.width;
		} else {
			mCameraPreviewWidth = mCameraPreviewSize.width;
			mCameraPreviewHeight = mCameraPreviewSize.height;
		}
		Log.d(LOG_TAG, "openCamera -- done");
	}

	/**
	 * Stops camera preview, and releases the camera to the system.
	 */
	private void releaseCamera() {
		if (mCamera != null) {
			mCamera.stopPreview();
			mCamera.release();
			mCamera = null;
			Log.d(LOG_TAG, "releaseCamera -- done");
		}
	}

	@Override
	public void onServiceConnected(Streaming service) {
		Log.d(LOG_TAG, "Streaming service connected");
	}

	@Override
	public void onServiceDisconnected(Streaming service) {
		Log.d(LOG_TAG, "Streaming service disconnected");
	}


}

