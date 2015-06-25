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
	
	private final int TIC_RATE = 60;
	private LinkedHashMap<String, Long> startTimes;
	private long totalStartTime;
	private long lastStopTime;
	private LinkedHashMap<String, Long> nanos;
	private LinkedHashMap<String, Float> percents;
	private ArrayList<String> names;
	private ArrayList<Long> ticTimes;
	private GameObject display;
	private GameObject background;
	private Text ticInfo;
	private LinkedHashMap<String, Text> texts;
	private LinkedHashMap<String, GameObject> bars;
	private Vector3f screenSize;
	private float spacing;
	private String exceptionMessage;
	private boolean initialized;

	public float avgTicRate;
	public float avgTicTime;
	public boolean visible;
	public Scene scene;

	public void init(boolean framerateProfile){
		if (initialized) return;
		visible = framerateProfile;
		startTimes = new LinkedHashMap<String, Long>();
		totalStartTime = TimeUtils.nanoTime();
		nanos = new LinkedHashMap<String, Long>();
		percents = new LinkedHashMap<String, Float>();
		avgTicRate = TIC_RATE;
		avgTicTime = 1000 / TIC_RATE;
		exceptionMessage = "User created subsystem names should not start with: \"__\"";
		initialized = true;
		if (visible) show();
	}

	private void show(){
		screenSize = new Vector3f(448, 448, 1);
		spacing = 0.6f;
		names = new ArrayList<String>();
		ticTimes = new ArrayList<Long>();
		for (int i = 0; i < TIC_RATE; i++) ticTimes.add(1000000000L / TIC_RATE);
		texts = new LinkedHashMap<String, Text>();
		bars = new LinkedHashMap<String, GameObject>();
		scene = new Scene("__Profiler");
		scene.init();
		display = scene.add("__PDisplay");
		background = display.children.get(0);
		background.color(0.125f, 0.125f, 0.125f, 0.5f);
		ticInfo = (Text)scene.add("__PText");
		ticInfo.position(spacing, -spacing * 1.5f, 0);
		ticInfo.parent(display);
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
	
	public void remove(String name){
		if (name.startsWith("__")) throw new RuntimeException(exceptionMessage);
		startTimes.remove(name);
		nanos.remove(name);
		percents.remove(name);
		super.remove(name);
		if (visible){
			names.remove(name);
			texts.get(name).end();
			bars.get(name).end();
			int size = names.size();
			float offset = names.get(size - 1).startsWith("__") ? spacing * 3f : spacing * 3.5f;
			Vector3f backgroundScale = new Vector3f(spacing * 23, spacing * size + offset, 1);
			background.scale(backgroundScale.mul(display.scale()));
		}
	}
	
	private void updateAvgTicVars(long delta){
		ticTimes.remove(0);
		ticTimes.add(delta);
		long sumTicTimes = 0;
		for (long l : ticTimes) sumTicTimes += l;
		avgTicTime = 1000000000000f / (TIC_RATE * sumTicTimes);
		avgTicRate = 1000 / avgTicTime;
	}

	private void updateDisplay(){
		ticInfo.set(formatForDisplay("tic info", avgTicTime, "ms", avgTicRate, "fps"));
		for (String name : nanos.keySet()){
			if (!names.contains(name)){
				names.add(name);
				int i = names.indexOf(name);
				float offset = (name.startsWith("__")) ? spacing * 3 : spacing * 3.5f;
				Vector3f position = new Vector3f(spacing, -(spacing * i + offset), 0);
				Vector3f displayScale = display.scale();
				Text text = (Text)scene.add("__PText");
				text.scale(displayScale);
				text.position(position.mul(displayScale));
				text.parent(display);
				texts.put(name, text);
				position.x = spacing * 18;
				GameObject bar = scene.add("__PBar");
				bar.scale(displayScale);
				bar.position(position.mul(displayScale));
				bar.parent(display);
				bars.put(name, bar);
				Vector3f backgroundScale = new Vector3f(spacing * 23, spacing * (i + 1) + offset, 1);
				background.scale(backgroundScale.mul(displayScale));
			}
			String n = name.startsWith("__") ? name.split("__")[1] : name;
			float m = nanos.get(name) * 0.000001f;
			float p = percents.get(name);
			Vector3f barScale = new Vector3f(0.04f * spacing * p, 0.4f, 1);
			bars.get(name).scale(barScale.mul(display.scale()));
			texts.get(name).set(formatForDisplay(n, m, "ms", p, "%"));
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
	
	private static String formatForDisplay(String name, float avgTicTime, String timeUnits, float avgTicRate, String valueUnits) {
		// "%-14s %4.1f %-3s %4.1f %s"
		StringBuffer buffer = new StringBuffer();
		
		addStringWithCharacterPadding(buffer, name, 14, false, ' ');
		buffer.append(" ");
		addFloat(buffer, avgTicTime, 4, 1, ' ');
		buffer.append(" ");
		addStringWithCharacterPadding(buffer, timeUnits, 3, false, ' ');
		buffer.append(" ");
		addFloat(buffer, avgTicRate, 4, 1, ' ');
		buffer.append(" ");
		buffer.append(valueUnits);
		
		return buffer.toString();
	}
	
	private static void addFloat(StringBuffer buffer, float value, int fieldPadding, int fractionPadding, char character) {
		String converted = Float.toString(value);
		String [] split = converted.split("\\.");
		
		addStringWithCharacterPadding(buffer, split.length > 0 ? split[0] : "0", fieldPadding - (fractionPadding + 1), true, character);
		if (fractionPadding > 0) {
			buffer.append(".");
			addStringWithCharacterPadding(buffer, split.length > 1 ? split[1] : "0", fractionPadding, false, '0');	
		}
	}
	
	private static void addStringWithCharacterPadding(StringBuffer buffer, String value, int padding, boolean padLeft, char character) {
		if (value != null) {
			if (value.length() > padding) {
				buffer.append(value.substring(0, padding));
			} else {
				if (padLeft) {
					padWithCharacter(buffer, padding - value.length(), character);
					buffer.append(value);
				} else {
					buffer.append(value);
					padWithCharacter(buffer, padding - value.length(), character);
				}
			}
		} else {
			padWithCharacter(buffer, padding, character);
		}
	}
	
	private static void padWithCharacter(StringBuffer buffer, int padding, char character) {
		for (int i = 0; i < padding; i++) {
			buffer.append(character);
		}
	}
}
