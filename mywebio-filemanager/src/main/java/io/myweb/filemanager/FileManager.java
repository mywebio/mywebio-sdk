package io.myweb.filemanager;

import android.os.Environment;
import io.myweb.api.GET;
import io.myweb.api.HttpResponse;
import io.myweb.api.MimeTypes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class FileManager {

	public static final String SLASH = "/";
	public static final String JSON_PWD = "pwd";
	public static final String JSON_LS = "ls";
	public static final String JSON_NAME = "name";
	public static final String JSON_TYPE = "type";
	public static final String JSON_URI = "uri";

	@GET("/*filename")
	public HttpResponse file(String filename) throws IOException, JSONException {
		File sdCard = Environment.getExternalStorageDirectory();
		String sdCardPath = sdCard.getAbsolutePath();
		String slashAndFilename = SLASH + noEndingSlashes(filename);
		File file = new File(sdCardPath + slashAndFilename);
		if (file.isDirectory()) {
			return dirJsonPage(slashAndFilename, file);
		} else {
			return fileContent(file);
		}
	}

	private HttpResponse fileContent(File file) throws FileNotFoundException {
		FileInputStream fis = new FileInputStream(file);
		long length = file.length();
		return HttpResponse.ok()
				.withBody(fis)
				.withContentLength(length)
				.withMimeTypeFromFilename(file.getName());
	}

	private HttpResponse dirJsonPage(String dir, File file) throws JSONException {
		JSONObject dirJson = new JSONObject();
		dirJson.put(JSON_PWD, dir);
		dirJson.put(JSON_LS, listFilesInJson(dir, file));
		return HttpResponse.ok()
				.withBody(dirJson);
	}

	private String noEndingSlashes(String str) {
		if (str.endsWith(SLASH)) {
			return str.substring(0, str.length() - 1);
		} else {
			return str;
		}
	}

	private JSONArray listFilesInJson(String dir, File file) throws JSONException {
		JSONArray resultArr = new JSONArray();
		File[] filesInDir = file.listFiles();
		if (filesInDir != null) {
			for (File f : filesInDir) {
				resultArr.put(fileAsJon(dir, f));
			}
		}
		return resultArr;
	}

	private JSONObject fileAsJon(String dir, File f) throws JSONException {
		JSONObject fileJson = new JSONObject();
		String baseName = baseName(f);
		fileJson.put(JSON_NAME, baseName);
		fileJson.put(JSON_TYPE, mimeType(f));
		fileJson.put(JSON_URI, (dir.equals(SLASH) ? "" : dir) + SLASH + baseName);
		return fileJson;
	}

	private String baseName(File f) {
		String absolutePath = f.getAbsolutePath();
		return absolutePath.substring(absolutePath.lastIndexOf(SLASH) + 1);
	}

	private String mimeType(File f) {
		if (f.isDirectory()) {
			return MimeTypes.MIME_INODE_DIRECTORY;
		} else {
			return MimeTypes.getMimeType(f.getName());
		}
	}
}
