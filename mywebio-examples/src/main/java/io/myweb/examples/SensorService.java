package io.myweb.examples;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;
import java.util.List;

import io.myweb.LocalService;

public class SensorService extends LocalService<SensorService> implements SensorEventListener {
	private SensorManager sensorManager;
	private volatile SensorEvent sensorValue;
	private int currentType = 0;

    public SensorService() {
    }

	@Override
	public void onCreate() {
		super.onCreate();
		sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		sensorManager.unregisterListener(this);
	}

	public List<Integer> getAvailableSensorTypes() {
		List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
		List<Integer> typeList = new ArrayList<Integer>(sensorList.size());
		for (Sensor s: sensorList) {
			typeList.add(s.getType());
		}
		return typeList;
	}

	public Sensor getDefaultSensor(int type) {
		return sensorManager.getDefaultSensor(type);
	}

	public SensorEvent getLastSensorEvent(int type) {
		Sensor s = sensorManager.getDefaultSensor(type);
		if (s == null) return null;
		if (currentType != type) {
			sensorManager.unregisterListener(this);
			sensorValue = null;
			sensorManager.registerListener(this, s, 1000000);
			currentType = type;
			synchronized (this) {
				try {
					wait(4000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		return sensorValue;
	}

	@Override
	public synchronized void onSensorChanged(SensorEvent sensorEvent) {
		sensorValue = sensorEvent;
		notifyAll();
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int i) {

	}
}
