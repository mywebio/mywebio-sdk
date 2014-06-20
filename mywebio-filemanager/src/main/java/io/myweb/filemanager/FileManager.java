package io.myweb.filemanager;

import android.os.Environment;
import io.myweb.api.GET;
import io.myweb.api.HttpResponse;
import io.myweb.api.MimeTypes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileManager {

	@GET("/sd/*filename")
	public HttpResponse file(String filename) throws IOException {
		File sdCard = Environment.getExternalStorageDirectory();
		String sdCardPath = sdCard.getAbsolutePath() + "/";
		File file = new File(sdCardPath + filename);
		if (file.isDirectory()) {
			return dirPage(sdCardPath, file);
		} else {
			return fileContent(file);
		}
	}

	private HttpResponse fileContent(File file) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(file);
		long length = file.length();
		return HttpResponse.create()
				.ok()
				.withBody(fis)
				.withContentLength(length)
				.withMimeTypeFromFilename(file.getName());
	}

	private HttpResponse dirPage(String basePath, File file) {
		StringBuilder sb = new StringBuilder();
		String relativePath = relativePath(basePath, file.getAbsolutePath());
		sb.append("<html><head><title>");
		sb.append("SD Card: ").append(relativePath);
		sb.append("</title></head>");
		sb.append("<body>");
		sb.append("<h2>").append("SD Card: ").append(relativePath).append("</h2>");
		sb.append("<ul>");
		if (!isRootDir(relativePath)) {
			sb.append("<li><a href=\"").append("..").append("\">").append("..").append("</a></li>");
		}
		File[] filesInDir = file.listFiles();
		for (File f : filesInDir) {
			String absolutePath = f.getAbsolutePath();
			String baseName = absolutePath.substring(absolutePath.lastIndexOf("/") + 1);
			String viewName;
			if (f.isDirectory()) {
				viewName = baseName + "/";
			} else {
				viewName = baseName;
			}
			sb.append("<li><a href=\"").append(viewName).append("\">").append(viewName).append("</a></li>");
		}
		sb.append("</ul>");
		sb.append("<hr noshade><em>Powered by <a href=\"http://myweb.io/\">myweb.io</a></em>");
		sb.append("</body>");
		String respBody = sb.toString();
		return HttpResponse.create()
				.ok()
				.withBody(respBody)
				.withMimeType(MimeTypes.MIME_TEXT_HTML)
				.withContentLength(respBody.length());
	}

	private boolean isRootDir(String relativePath) {
		return "".equals(relativePath) || "/".equals(relativePath);
	}

	private String relativePath(String basePath, String absolutePath) {
		return absolutePath.replaceFirst(basePath, "");
	}
}
