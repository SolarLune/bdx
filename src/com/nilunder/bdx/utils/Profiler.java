package com.nilunder.bdx.utils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.Gdx;

import com.nilunder.bdx.Scene;
import com.nilunder.bdx.Text;
import com.nilunder.bdx.GameObject;

import javax.vecmath.Vector3f;


public class Profiler extends LinkedHashMap<String, Long>{
	private LinkedHashMap<String, Long> startTimes = new LinkedHashMap<String, Long>();
	private long totalStartTime = TimeUtils.nanoTime();
	private long lastStopTime;
	private LinkedHashMap<String, Long> nanos = new LinkedHashMap<String, Long>();
	private LinkedHashMap<String, Float> percents = new LinkedHashMap<String, Float>();
	private ArrayList<String> names;
	private ArrayList<Long> ticTimes;
	private GameObject display;
	private GameObject background;
	private Text ticInfo;
	private LinkedHashMap<String, Text> texts;
	private LinkedHashMap<String, GameObject> bars;
	private Vector3f screenSize;
	private final int ticRate = 60;

	public float avgTicRate = 60f;
	public float avgTicTime = 1000/60f;
	public boolean visible = false;

	public Scene scene;

	public void show(){
		visible = true;
		names = new ArrayList<String>();
		ticTimes = new ArrayList<Long>();
		for (int i = 0; i < ticRate; i++)
			ticTimes.add(1000000000L / ticRate);
		texts = new LinkedHashMap<String, Text>();
		bars = new LinkedHashMap<String, GameObject>();
		scene = new Scene("__Profiler");
		scene.init();
		display = scene.add("__PDisplay");
		background = display.children.get(0);
		ticInfo = (Text)scene.add("__PText");
		ticInfo.position(0.6f, -1.2f, 0);
		ticInfo.parent(display);
		screenSize = new Vector3f(512, 512, 1);
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
	
	private void updateAvgTicVars(long delta){
		ticTimes.remove(0);
		ticTimes.add(delta);
		long sumTicTimes = 0;
		for (long l : ticTimes)
			sumTicTimes += l;
		avgTicTime = 1000000000000f / (ticRate * sumTicTimes);
		avgTicRate = 1000 / avgTicTime;
	}

	private void updateDisplay(){
		ticInfo.set(String.format("%-14s %4.1f %-3s %4.1f %s", "tic info", avgTicTime, "ms", avgTicRate, "fps"));
		for (String name : nanos.keySet()){
			if (!names.contains(name)){
				names.add(name);
				int i = names.indexOf(name);
				float offset = (name.contains("__")) ? 2.4f : 3.0f;
				Vector3f p = new Vector3f(0.6f, -(0.6f * i + offset), 0);
				Vector3f displayScale = display.scale();
				Text text = (Text)scene.add("__PText");
				text.scale(displayScale);
				text.position(p.mul(displayScale));
				text.parent(display);
				texts.put(name, text);
				p.x = 10.8f;
				GameObject bar = scene.add("__PBar");
				bar.scale(displayScale);
				bar.position(p.mul(displayScale));
				bar.parent(display);
				bars.put(name, bar);
				Vector3f backgroundScale = new Vector3f(14.4f, 0.6f * (i + 1.2f) + offset, 1);
				background.scale(backgroundScale.mul(displayScale));
			}
			String n = name.contains("__") ? name.split("__")[1] : name;
			float m = nanos.get(name) * 0.000001f;
			float p = percents.get(name);
			Vector3f barScale = new Vector3f(p, 1, 1);
			texts.get(name).set(String.format("%-14s %4.1f %-3s %4.1f %s", n, m, "ms", p, "%"));
			bars.get(name).scale(barScale.mul(display.scale()));
		}
		Vector3f currScreenSize = new Vector3f(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), 1);
		if (!screenSize.equals(currScreenSize))
			display.scale(screenSize.div(currScreenSize));
	}
	
	public void update(){
		long totalEndTime = TimeUtils.nanoTime();
		long totalDeltaTime = totalEndTime - totalStartTime;
		updateAvgTicVars(totalDeltaTime);
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
