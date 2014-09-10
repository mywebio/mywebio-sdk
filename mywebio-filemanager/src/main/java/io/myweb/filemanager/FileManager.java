package io.myweb.filemanager;

import static java.net.URLDecoder.decode;

import android.os.Environment;
import io.myweb.api.GET;
import io.myweb.http.Response;
import io.myweb.http.MimeTypes;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FileManager {

	public static final String SLASH = "/";
	public static final String JSON_PWD = "pwd";
	public static final String JSON_LS = "ls";
	public static final String JSON_NAME = "name";
	public static final String JSON_TYPE = "type";
	public static final String JSON_URI = "uri";

	@GET("/ls/*filename")
	public Response file(String filename) throws IOException, JSONException {
		String slashAndFilename = SLASH + noEndingSlashes(decode(filename, "UTF-8"));
		File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + slashAndFilename);
		return file.isDirectory() ? dirJsonPage(slashAndFilename, file) : fileContent(file);
	}

	private Response fileContent(File file) throws FileNotFoundException {
		return Response.ok().withFile(file);
	}

	private Response dirJsonPage(String dir, File file) throws JSONException {
		JSONObject dirJson = new JSONObject();
		dirJson.put(JSON_PWD, dir);
		dirJson.put(JSON_LS, listFilesInJson(dir, file));
		return Response.ok().withBody(dirJson);
	}

	private String noEndingSlashes(String str) {
		return str.endsWith(SLASH) ? str.substring(0, str.length() - 1) : str;
	}

	private JSONArray listFilesInJson(String dir, File file) throws JSONException {
		JSONArray resultArr = new JSONArray();
		File[] filesInDir = file.listFiles();
		Arrays.sort(filesInDir, new FilesComparator());
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
		return f.isDirectory() ? MimeTypes.MIME_INODE_DIRECTORY : MimeTypes.getMimeType(f.getName());
	}

	class FilesComparator implements Comparator<File> {
		@Override
		public int compare(File f1, File f2) {
			if (f1.isDirectory() == f2.isDirectory()) {
				return f1.getName().compareToIgnoreCase(f2.getName());
			} else {
				return f1.isDirectory() && !f2.isDirectory() ? -1 : 1;
			}
		}
	}
}