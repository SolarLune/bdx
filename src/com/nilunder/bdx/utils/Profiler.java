package com.nilunder.bdx.utils;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.TimeUtils;

public class Profiler extends LinkedHashMap<String, Long>{
	private LinkedHashMap<String, Float> percents = new LinkedHashMap<String, Float>();
	private long totalStartTime = TimeUtils.millis();
	private long startTime = totalStartTime;
	
	public LinkedHashMap<String, Float> percents(){
		return percents;
	}
	
	public void start(){
		startTime = TimeUtils.millis();
	}

	public Long stop(String name){
		long deltaTime = TimeUtils.millis() - startTime;
		long storedDeltaTime = deltaTime;
		if (containsKey(name))
			storedDeltaTime = get(name) + deltaTime;
		put(name, storedDeltaTime);
		start();
		return deltaTime;
	}

	public void update(){
		long totalEndTime = TimeUtils.millis();
		float totalDeltaTime = totalEndTime - totalStartTime;
		totalStartTime = totalEndTime;
		if (totalDeltaTime == 0)
			return;
		float deltaTimes = 0;
		for (Entry<String, Long> e : entrySet()){
			long deltaTime = e.getValue();
			deltaTimes += deltaTime;
			float deltaTimePercent = 100 * deltaTime / totalDeltaTime;
			percents.put(e.getKey(), deltaTimePercent);
		}
		float outsideTimePercent = 100 * (1 - deltaTimes / totalDeltaTime);
		percents.put("outside", outsideTimePercent);
		clear();
	}
	
}
