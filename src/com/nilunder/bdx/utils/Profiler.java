package com.nilunder.bdx.utils;

import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.TimeUtils;

public class Profiler extends LinkedHashMap<String, Long>{
	private LinkedHashMap<String, Long> startTimes = new LinkedHashMap<String, Long>();
	private long totalStartTime = TimeUtils.millis();
	private long lastStopTime;
	public LinkedHashMap<String, Long> millis = new LinkedHashMap<String, Long>();
	public LinkedHashMap<String, Float> percents = new LinkedHashMap<String, Float>();
	
	public void start(String name){
		startTimes.put(name, TimeUtils.millis());
	}

	public Long stop(String name){
		long stopTime = TimeUtils.millis();
		long startTime = lastStopTime;
		if (startTimes.containsKey(name))
			startTime = startTimes.get(name);
		long deltaTime = stopTime - startTime;
		long storedDeltaTime = deltaTime;
		if (containsKey(name))
			storedDeltaTime += get(name);
		put(name, storedDeltaTime);
		lastStopTime = stopTime;
		return deltaTime;
	}

	public void update(){
		long totalEndTime = TimeUtils.millis();
		long totalDeltaTime = totalEndTime - totalStartTime;
		totalStartTime = totalEndTime;
		long deltaTimes = 0;
		LinkedHashMap<String, Long> userMillis = new LinkedHashMap<String, Long>();
		LinkedHashMap<String, Float> userPercents = new LinkedHashMap<String, Float>();
		for (Entry<String, Long> e : entrySet()){
			long deltaTime = e.getValue();
			String name = e.getKey();
			float deltaTimePercent = 100f * deltaTime / totalDeltaTime;
			if (name.startsWith("__")){
				deltaTimes += deltaTime;
				percents.put(name, deltaTimePercent);
				millis.put(name, deltaTime);
			}else{
				userPercents.put(name, deltaTimePercent);
				userMillis.put(name, deltaTime);
			}
		}
		long outsideDeltaTime = totalDeltaTime - deltaTimes;
		float outsideTimePercent = 100f * outsideDeltaTime / totalDeltaTime;
		millis.put("__outside", outsideDeltaTime);
		percents.put("__outside", outsideTimePercent);
		millis.putAll(userMillis);
		percents.putAll(userPercents);
		startTimes.clear();
		clear();
	}
	
}
