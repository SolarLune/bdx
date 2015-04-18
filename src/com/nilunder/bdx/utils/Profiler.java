package com.nilunder.bdx.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.TimeUtils;

import com.nilunder.bdx.Scene;
import com.nilunder.bdx.Text;
import com.nilunder.bdx.GameObject;

public class Profiler extends LinkedHashMap<String, Long>{
	private LinkedHashMap<String, Long> startTimes = new LinkedHashMap<String, Long>();
	private long totalStartTime = TimeUtils.nanoTime();
	private long lastStopTime;
	private LinkedHashMap<String, Long> nanos = new LinkedHashMap<String, Long>();
	private LinkedHashMap<String, Float> percents = new LinkedHashMap<String, Float>();
	private Text ticRateText;
	private ArrayList<String> names;
	private LinkedHashMap<String, Text[]> texts;
	private LinkedHashMap<String, GameObject> bars;
	public boolean visible = false;
	public float ticRate;
	public Scene scene;
	
	public void show(){
		visible = true;
		names = new ArrayList<String>();
		texts = new LinkedHashMap<String, Text[]>();
		bars = new LinkedHashMap<String, GameObject>();
		scene = new Scene("__Profiler");
		scene.init();
		ticRateText = (Text)scene.add("__PText");
		ticRateText.position(60, 0, 0);
	}
	
	public void start(String name){
		startTimes.put(name, TimeUtils.nanoTime());
	}

	public float stop(String name){
		long stopTime = TimeUtils.nanoTime();
		long startTime = startTimes.containsKey(name) ? startTimes.get(name) : lastStopTime;
		long deltaTime = stopTime - startTime;
		if (visible){
			long storedDeltaTime = deltaTime;
			if (containsKey(name)) storedDeltaTime += get(name);
			put(name, storedDeltaTime);
			lastStopTime = stopTime;
		}
		return deltaTime * 0.000001f;
	}
	
	private void updateDisplay(){
		ticRateText.set(String.format("%.1f", ticRate));
		for (String name : nanos.keySet()){
			if (!names.contains(name)){
				names.add(name);
				int i = names.indexOf(name);
				Text category = (Text)scene.add("__PText");
				Text numbers = (Text)scene.add("__PText");
				Text[] text = new Text[]{category, numbers};
				GameObject bar = scene.add("__PBar");
				texts.put(name, text);
				bars.put(name, bar);
				String n = name.contains("__") ? name.split("__")[1] : name;
				category.set(n);
				category.position(0, -i, 0);
				numbers.position(8, -i, 0);
				bar.position(16, -i, 0);
			}
			float m = nanos.get(name) * 0.000001f;
			float p = percents.get(name);
			texts.get(name)[1].set(String.format("%.1f %s %.1f %s", m, "ms ", p, "%"));
			bars.get(name).scale(p, 1, 1);
		}
	}

	public void update(){
		long totalEndTime = TimeUtils.nanoTime();
		long totalDeltaTime = totalEndTime - totalStartTime;
		ticRate = 1000000000f / totalDeltaTime;
		totalStartTime = totalEndTime;
		long deltaTimes = 0;
		LinkedHashMap<String, Long> userNanos = new LinkedHashMap<String, Long>();
		LinkedHashMap<String, Float> userPercents = new LinkedHashMap<String, Float>();
		for (Entry<String, Long> e : entrySet()){
			long deltaTime = e.getValue();
			String name = e.getKey();
			float deltaTimePercent = 100f * deltaTime / totalDeltaTime;
			if (name.startsWith("__")){
				deltaTimes += deltaTime;
				percents.put(name, deltaTimePercent);
				nanos.put(name, deltaTime);
			}else{
				userPercents.put(name, deltaTimePercent);
				userNanos.put(name, deltaTime);
			}
		}
		long outsideDeltaTime = totalDeltaTime - deltaTimes;
		float outsideTimePercent = 100f * outsideDeltaTime / totalDeltaTime;
		nanos.put("__outside", outsideDeltaTime);
		percents.put("__outside", outsideTimePercent);
		nanos.putAll(userNanos);
		percents.putAll(userPercents);
		startTimes.clear();
		clear();
		updateDisplay();
	}
	
}
