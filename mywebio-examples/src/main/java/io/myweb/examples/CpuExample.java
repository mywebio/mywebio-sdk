package io.myweb.examples;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;
import java.util.regex.Pattern;

import io.myweb.api.GET;

public class CpuExample {
	private static int numCores = 0;

	@GET("/stat")
	public JSONObject cpustat() throws IOException, JSONException {
		JSONObject result =  new JSONObject();
		result.put("available", getNumCores());
		result.put("timestamp", System.currentTimeMillis());
		BufferedReader buf = new BufferedReader(new FileReader("/proc/stat"));
		String line = buf.readLine();
		while (line.startsWith("cpu")) {
			processCpuLine(line, result);
			line = buf.readLine();
		}
		buf.close();
		return result;
	}

	private void processCpuLine(String line, JSONObject target) throws JSONException {
		String[] values = line.substring(5).trim().split("\\s+");
		JSONArray cpu = new JSONArray();
		for (String val: values) cpu.put(Integer.parseInt(val));
		target.put(line.substring(0,4).trim(), cpu);
	}

	/**
	 * Slightly modified example from StackOverflow:
	 *
	 * Gets the number of cores available in this device, across all processors.
	 * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
	 * @return The number of cores, or 1 if failed to get result
	 */
	private static int getNumCores() {
		if (numCores > 0 ) return numCores;
		//Private Class to display only CPU devices in the directory listing
		class CpuFilter implements FileFilter {
			@Override
			public boolean accept(File pathname) {
				//Check if filename is "cpu", followed by a single digit number
				if(Pattern.matches("cpu[0-9]+", pathname.getName())) {
					return true;
				}
				return false;
			}
		}

		try {
			//Get directory containing CPU info
			File dir = new File("/sys/devices/system/cpu/");
			//Filter to only list the devices we care about
			File[] files = dir.listFiles(new CpuFilter());
			//Return the number of cores (virtual CPU devices)
			numCores = files.length;
		} catch(Exception e) {
			numCores = 1;
		}

		return numCores;
	}
}
