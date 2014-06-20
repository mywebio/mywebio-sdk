package io.myweb.gallery;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import io.myweb.api.GET;
import io.myweb.api.Produces;

import java.io.*;

public class PhotoGallery {

	@GET("/thumbs/:id")
	@Produces("image/jpeg")
	public InputStream thumbs(Context ctx, String id) {
		int idInt = Integer.parseInt(id);
		Bitmap thumbnail = MediaStore.Images.Thumbnails.getThumbnail(ctx.getContentResolver(), idInt, MediaStore.Images.Thumbnails.MICRO_KIND, null);
		if (thumbnail != null) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, baos);
			return new ByteArrayInputStream(baos.toByteArray());
		} else {
			// TODO 404
			return null;
		}
	}

	@GET("/images/:id")
	@Produces("image/jpeg")
	public InputStream images(Context ctx, String id) throws FileNotFoundException {
		Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		String[] projection = new String[] { MediaStore.Images.ImageColumns.DATA };
		String selection = MediaStore.Images.ImageColumns._ID + " = ?";
		String[] selectionArgs = new String[] { id };
		Cursor cursor = ctx.getContentResolver().query(contentUri, projection, selection, selectionArgs, null);
		int dataIdx = cursor.getColumnIndexOrThrow(projection[0]);
		if (cursor.getCount() == 1) {
			cursor.moveToNext();
			String data = cursor.getString(dataIdx);
			File img = new File(data);
			return new BufferedInputStream(new FileInputStream(img), 32 * 1024);
		} else {
			// TODO 404
			return null;
		}
	}

	@GET("/config.xml")
	@Produces("application/xml")
	public String config(Context ctx) {
		Uri contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
		String[] projection = new String[] { MediaStore.Images.ImageColumns._ID };
		Cursor cursor = ctx.getContentResolver().query(contentUri, projection, null, null, null);
		int idIdx = cursor.getColumnIndexOrThrow(projection[0]);
		int[] imgIds = new int[cursor.getCount()];
		int i = 0;
		while (cursor.moveToNext()) {
			int id = cursor.getInt(idIdx);
			imgIds[i++] = id;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		sb.append("<juiceboxgallery galleryTitle=\"Web.io Photo Gallery\">\n");
		for (int imgId : imgIds) {
			String imageTag = "  <image imageURL=\"images/%d\" thumbURL=\"thumbs/%d\" linkURL=\"images/%d\" linkTarget=\"_blank\" >";
			sb.append(String.format(imageTag, imgId, imgId, imgId));
			sb.append("  </image>");
		}
		sb.append("</juiceboxgallery>");
		return sb.toString();
	}
}
