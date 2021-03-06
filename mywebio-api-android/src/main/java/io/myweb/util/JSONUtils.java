package io.myweb.util;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Iterator;

public class JSONUtils {
	public static ContentValues jsonToValues(JSONObject json) {
		ContentValues values = new ContentValues();
		Iterator<String> i = json.keys();
		while (i.hasNext()) {
			String name = i.next();
			Object value = json.opt(name);
			if (value != null) values.put(name, value.toString());
			else values.putNull(name);
		}
		return values;
	}

	public static JSONObject cursorToJSONObject(Cursor cursor, JSONArray range) {
		JSONObject json = new JSONObject();
		if (cursor != null) {
			JSONArray jsonArray = new JSONArray();
			int fromIdx = 0;
			int toIdx = cursor.getCount();
			if (range != null && range.length()==2) try { // calculate proper range
				int rangeFrom = range.getInt(0);
				if (rangeFrom < 0) rangeFrom = 0;
				else if (rangeFrom > toIdx) rangeFrom = toIdx;
				int rangeTo = range.getInt(1);
				if (rangeTo<rangeFrom) rangeTo = rangeFrom;
				else if (rangeTo > toIdx) rangeTo = toIdx;
				fromIdx = rangeFrom;
				toIdx = rangeTo;
				JSONArray rangeArray = new JSONArray();
				rangeArray.put(rangeFrom);
				rangeArray.put(rangeTo);
				json.put("range", rangeArray);
			} catch(JSONException e) {
				e.printStackTrace();
			}
			if (fromIdx > 0) cursor.moveToPosition(fromIdx-1);
			while (cursor.moveToNext() && fromIdx < toIdx) {
				JSONObject obj = new JSONObject();
				for (int i = 0; i < cursor.getColumnCount(); i++) {
					String name = cursor.getColumnName(i);
					try {
						switch (cursor.getType(i)) {
							case Cursor.FIELD_TYPE_INTEGER:
								obj.put(name, cursor.getInt(i));
								break;
							case Cursor.FIELD_TYPE_FLOAT:
								obj.put(name, cursor.getDouble(i));
								break;
							case Cursor.FIELD_TYPE_STRING:
								obj.put(name, cursor.getString(i));
								break;
							case Cursor.FIELD_TYPE_NULL:
								obj.put(name, JSONObject.NULL);
								break;
							case Cursor.FIELD_TYPE_BLOB:
								obj.put(name, Base64.encodeToString(cursor.getBlob(i), Base64.DEFAULT));
								break;
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
				}
				jsonArray.put(obj);
				fromIdx++;
			}
			try {
				json.put("count", cursor.getCount());
				json.put("columns", stringArrayToJSONArray(cursor.getColumnNames()));
				json.put("result", jsonArray);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			cursor.close();
		}
		return json;
	}

	public static JSONArray stringArrayToJSONArray(String[] strings) {
		if (strings==null) return null;
		if (strings.length==0) return new JSONArray();
		return new JSONArray(Arrays.asList(strings));
	}

	public static String[] jsonArrayToStringArray(JSONArray json) {
		if (json == null ) return null;
		if (json.length()==0) return new String[] {};
		String[] strings = new String[json.length()];
		for (int i = 0; i < json.length(); i++) {
			try {
				strings[i] = json.getString(i);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return strings;
	}
}
