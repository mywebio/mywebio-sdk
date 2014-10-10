package io.myweb.examples;

import android.content.Context;

import java.io.File;
import java.io.IOException;

import io.myweb.api.GET;
import io.myweb.api.Produces;
import io.myweb.http.MimeTypes;

// This example requires root access
public class ScreenshotExample {

	@GET("/screenshot")
	@Produces(MimeTypes.MIME_IMAGE_PNG)
	public File screenshot(Context ctx) throws IOException {
		String fileName = ctx.getCacheDir()+"/screenshot.png";
		Process su = new ProcessBuilder().command("su").start();
		su.getOutputStream().write(("screencap -p "+fileName+"\nexit\n").getBytes());
		try {
			su.waitFor();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		su.destroy();
		return new File(fileName);
	}
}
