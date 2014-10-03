package io.myweb.camera;

import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;

import io.myweb.LocalService;

public class StreamingService extends LocalService<Streaming> implements Streaming, MediaRecorder.OnErrorListener {
	private static final String LOG_TAG = CaptureCamera.class.getSimpleName();
	public static final int BUFFER_SIZE = 16 * 10000;
	public static final int SO_TIMEOUT = 5000;
	private static final int HIDDEN_MPEG_TS = 8;
	private MediaRecorder mediaRecorder;
	private Camera camera;
	private LocalSocket receiverSocket;
	private LocalSocket senderSocket;
	private LocalServerSocket serverSocket;
	private volatile InputStreamListener inputListener;
	private volatile boolean streaming;

	public StreamingService() {
		streaming = false;
	}

	public static Connection<Streaming> connection(Context ctx) {
		return new Connection<Streaming>(ctx, StreamingService.class);
	}

	@Override
	public void startStreaming(Camera cam) {
		Log.d(LOG_TAG, "Starting streaming");
		if (inputListener != null && cam != null)
			try {
				camera = cam;
				createLocalSockets();
				startMediaRecorder(senderSocket.getFileDescriptor());
				streaming = true;
				inputListener.onInputStreamReady(receiverSocket.getInputStream());
				Log.d(LOG_TAG, "Streaming started.");
			} catch (Exception e) {
				e.printStackTrace();
				stopStreaming();
			}
	}

	@Override
	public void stopStreaming() {
		Log.d(LOG_TAG, "Stopping streaming");
		stopMediaRecorder();
		destroyLocalSockets();
		streaming = false;
	}

	@Override
	public boolean isStreaming() {
		return streaming;
	}

	@Override
	public void setInputStreamListener(InputStreamListener listener) {
		inputListener = listener;
	}

	protected String localName() {
		return getClass().getName()+"-"+this.hashCode();
	}

	protected void createLocalSockets() throws IOException {
		Log.d(LOG_TAG, "Binding to " + localName());
		serverSocket = new LocalServerSocket(localName());
		receiverSocket = new LocalSocket();
		receiverSocket.connect(new LocalSocketAddress(localName()));
		receiverSocket.setReceiveBufferSize(BUFFER_SIZE);
		receiverSocket.setSoTimeout(SO_TIMEOUT);
		senderSocket = serverSocket.accept();
		senderSocket.setSendBufferSize(BUFFER_SIZE);
	}

	protected void destroyLocalSockets() {
		try {
			if (receiverSocket!=null) receiverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if(senderSocket!=null) senderSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			if(serverSocket!=null) serverSocket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		receiverSocket = null;
		senderSocket = null;
		serverSocket = null;
	}

//	private static String dump(Object obj) {
//		StringBuilder sb = new StringBuilder();
//		for(Field f: obj.getClass().getDeclaredFields()) {
//			try {
//				sb.append(f.getName()+":\t"+f.get(obj).toString()+"\n");
//			} catch (IllegalAccessException e) {
////				e.printStackTrace();
//			}
//		}
//		return sb.toString();
//	}

	private void startMediaRecorder(FileDescriptor fd) throws IllegalStateException, IOException {
		mediaRecorder = new MediaRecorder();

		// Step 1: Unlock and set camera to MediaRecorder
		camera.stopPreview();
		camera.unlock();
		mediaRecorder.setCamera(camera);

		// Step 2: Set source
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);

		// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
//		profile.videoFrameRate = 25;
//		Log.d(LOG_TAG, "Profile: "+dump(profile));
		profile.fileFormat = HIDDEN_MPEG_TS;
		mediaRecorder.setProfile(profile);

		// Step 4: Set output file
		mediaRecorder.setOutputFile(fd);
		mediaRecorder.setOnErrorListener(this);

//		mediaRecorder.setPreviewDisplay();

		// Step 5: Prepare configured MediaRecorder
		mediaRecorder.prepare();
		// it takes some time to prepare
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// Step 6: Start the recording
		mediaRecorder.start();
	}

	private void stopMediaRecorder() {
		if (camera!=null) {
			camera.lock();
			camera = null;
		}
		if (mediaRecorder != null) {
			try {
				mediaRecorder.stop();
			} catch (Exception e) {
				Log.e(LOG_TAG, e.getMessage(), e);
			}
			mediaRecorder.reset();   // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
		}
	}

	@Override
	public void onError(MediaRecorder mediaRecorder, int width, int height) {
		stopStreaming();
	}
}
