package com.nilunder.bdx.utils;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.Gdx;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.Text;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;


public class Profiler extends LinkedHashMap<String, Long>{
	
	private final int TICK_RATE = Bdx.TICK_RATE;
	private final Vector3f SCREEN_SIZE = new Vector3f(448, 448, 1);
	private final Vector4f BG_COLOR = new Vector4f(0.125f, 0.125f, 0.125f, 0.5f);
	private final float SPACING = 0.6f;
	private final float BAR_HEIGHT = 0.4f;
	private final float BAR_WIDTH = SPACING * 4;
	private final float BAR_POSITION = SPACING * 18;
	private final float BG_WIDTH = SPACING * 23;
	private final String EXC_MSG = "User created subsystem names should not start with: \"__\"";
	private final String ERR_MSG = "warning: \"Show Framerate and Profile\" is not enabled";
	
	private long totalStartTime;
	private long totalDeltaTime;
	private long lastStopTime;
	private LinkedHashMap<String, Long> startTimes;
	private LinkedHashMap<String, Long> nanos;
	private LinkedHashMap<String, Float> percents;
	private ArrayList<String> names;
	private ArrayList<Long> tickTimes;
	
	private GameObject display;
	private GameObject background;
	private Text tickInfo;
	private LinkedHashMap<String, Text> texts;
	private LinkedHashMap<String, GameObject> bars;
	
	private float scale;
	private boolean visible;
	private boolean initialized;
	private Vector2f lastDisplaySize;
	
	public Scene scene;
	
	public float avgTickRate;
	public float avgTickTime;
	
	{
		totalStartTime = TimeUtils.nanoTime();
		startTimes = new LinkedHashMap<String, Long>();
		nanos = new LinkedHashMap<String, Long>();
		percents = new LinkedHashMap<String, Float>();
		tickTimes = new ArrayList<Long>();
		for (int i=0; i < TICK_RATE; i++){
			tickTimes.add(1000000000L / TICK_RATE);
		}
		
		scale = 1f;
		initialized = false;
		avgTickRate = TICK_RATE;
		avgTickTime = 1000 / TICK_RATE;
	}
	
	public void init(boolean framerateProfile){
		if (initialized){
			return;
		}
		
		initialized = true;
		visible = framerateProfile;
		if (visible){
			lastDisplaySize = Bdx.display.size();
			
			scene = new Scene("__Profiler");
			scene.init();
			
			display = scene.add("__PDisplay");
			background = display.children.get("__PBackground");
			background.color(BG_COLOR);
			background.parent(display);
			
			texts = new LinkedHashMap<String, Text>();
			bars = new LinkedHashMap<String, GameObject>();
			names = new ArrayList<String>();
			
			tickInfo = (Text)add("__PText", new Vector3f(SPACING, -SPACING * 1.5f, 0));
			
			updateScale();
		}
	}
	
	public boolean visible(){
		return visible;
	}
	
	private void updateScale(){
		display.scale(SCREEN_SIZE.div(new Vector3f(lastDisplaySize.x, lastDisplaySize.y, 1)).mul(scale));
	}
	
	public void scale(float f){
		if (!visible){
			System.err.println(ERR_MSG);
			return;
		}
		scale = f;
		updateScale();
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
			if (containsKey(name)){
				storedDeltaTime += get(name);
			}
			put(name, storedDeltaTime);
			lastStopTime = stopTime;
		}
		return deltaTime * 0.000001f;
	}
	
	public void remove(String name){
		if (name.startsWith("__")){
			throw new RuntimeException(EXC_MSG);
		}
		startTimes.remove(name);
		nanos.remove(name);
		percents.remove(name);
		super.remove(name);
		if (visible){
			names.remove(name);
			texts.get(name).end();
			bars.get(name).end();
			int size = names.size();
			float offset = names.get(size - 1).startsWith("__") ? SPACING * 3f : SPACING * 3.5f;
			Vector3f backgroundScale = new Vector3f(BG_WIDTH, SPACING * size + offset, 1);
			background.scale(backgroundScale.mul(display.scale()));
		}
	}
	
	public void updateVariables(){
		long totalEndTime = TimeUtils.nanoTime();
		totalDeltaTime = totalEndTime - totalStartTime;
		totalStartTime = totalEndTime;
		tickTimes.remove(0);
		tickTimes.add(totalDeltaTime);
		long sumTickTimes = 0;
		for (long l : tickTimes){
			sumTickTimes += l;
		}
		avgTickTime = 1000000000000f / (TICK_RATE * sumTickTimes);
		avgTickRate = TICK_RATE * 1000000000f / sumTickTimes;
	}

	private GameObject add(String name, Vector3f position){
		GameObject obj = scene.add(name);
		Vector3f scale = display.scale();
		obj.scale(scale);
		obj.position(position.mul(scale));
		obj.parent(display);
		return obj;
	}
	
	private void updateTextsAndBars(){
		for (String name : nanos.keySet()){
			if (!names.contains(name)){
				names.add(name);
				int i = names.indexOf(name);
				float offset = (name.startsWith("__")) ? SPACING * 3 : SPACING * 3.5f;
				Vector3f position = new Vector3f(SPACING, -(SPACING * i + offset), 0);
				texts.put(name, (Text)add("__PText", position));
				position.x = BAR_POSITION;
				bars.put(name, add("__PBar", position));
				Vector3f backgroundScale = new Vector3f(BG_WIDTH, SPACING * (i + 1) + offset, 1);
				background.scale(backgroundScale.mul(display.scale()));
			}
			String n = name.startsWith("__") ? name.split("__")[1] : name;
			float m = nanos.get(name) * 0.000001f;
			float p = percents.get(name);
			Vector3f barScale = new Vector3f(BAR_WIDTH * p * 0.01f, BAR_HEIGHT, 1);
			bars.get(name).scale(barScale.mul(display.scale()));
			texts.get(name).set(formatForDisplay(n, m, "ms", p, "%"));
		}
	}
	
	public void updateVisible(){
		tickInfo.set(formatForDisplay("tick info", avgTickTime, "ms", avgTickRate, "fps"));
		
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
		updateTextsAndBars();
		
		Vector2f ds = Bdx.display.size();
		if (!lastDisplaySize.equals(ds)){
			lastDisplaySize = ds;
			updateScale();
		}
	}
	
	private static String formatForDisplay(String name, float avgTickTime, String timeUnits, float avgTickRate, String valueUnits){
		// "%-14s %4.1f %-3s %4.1f %s"
		StringBuffer buffer = new StringBuffer();
		
		addString(buffer, name, 14, false, ' ');
		buffer.append(" ");
		addFloat(buffer, avgTickTime, 4, 1, ' ');
		buffer.append(" ");
		addString(buffer, timeUnits, 3, false, ' ');
		buffer.append(" ");
		addFloat(buffer, avgTickRate, 4, 1, ' ');
		buffer.append(" ");
		buffer.append(valueUnits);
		
		return buffer.toString();
	}
	
	private static void addFloat(StringBuffer buffer, float value, int fieldPadding, int fractionPadding, char character){
		String converted = Float.toString(value);
		String [] split = converted.split("\\.");
		
		addString(buffer, split.length > 0 ? split[0] : "0", fieldPadding - (fractionPadding + 1), true, character);
		if (fractionPadding > 0){
			buffer.append(".");
			addString(buffer, split.length > 1 ? split[1] : "0", fractionPadding, false, '0');	
		}
	}
	
	private static void addString(StringBuffer buffer, String value, int padding, boolean padLeft, char character){
		if (value != null){
			if (value.length() > padding){
				buffer.append(value.substring(0, padding));
			}else if (padLeft){
				padWithCharacter(buffer, padding - value.length(), character);
				buffer.append(value);
			}else{
				buffer.append(value);
				padWithCharacter(buffer, padding - value.length(), character);
			}
		}else{
			padWithCharacter(buffer, padding, character);
		}
	}
	
	private static void padWithCharacter(StringBuffer buffer, int padding, char character){
		for (int i=0; i < padding; i++){
			buffer.append(character);
		}
	}
}
