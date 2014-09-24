package io.myweb.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;

public interface Streaming {
	void startStreaming(Camera c);

	void stopStreaming();

	boolean isStreaming();

	void setInputStreamListener(InputStreamListener listener);
}
