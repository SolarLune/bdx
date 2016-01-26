package com.nilunder.bdx.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.graphics.profiling.GLErrorListener;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.Text;

import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;


public class Profiler{
	
	public class Gl extends com.badlogic.gdx.graphics.profiling.GLProfiler{
		
		private HashMap<String, Text> texts;
		private HashMap<String, Integer> stats;
		private HashMap<String, Integer> fields;
		private String[] names;
		
		{
			names = new String[]{
				"calls",
				"drawCalls",
				"shaderSwitches",
				"textureBindings",
				"vertexCount"
			};
			
			stats = new HashMap<String, Integer>();
			fields = new HashMap<String, Integer>();
			for (String name : names){
				stats.put(name, 0);
				fields.put(name, 0);
			}
		}
		
		protected void initTexts(){
			texts = new HashMap<String, Text>();
			Vector3f position = new Vector3f(SPACING, verticalOffset, 0);
			for (String name : names){
				texts.put(name, (Text)add("__PText", position));
				position.y = verticalOffset(1);
			}
			texts.put("triangleCount", (Text)add("__PText", position));
			verticalOffset(1.5f);
		}
		
		public int calls(){
			return stats.get("calls");
		}
		
		public int drawCalls(){
			return stats.get("drawCalls");
		}
		
		public int shaderSwitches(){
			return stats.get("shaderSwitches");
		}
		
		public int textureBindings(){
			return stats.get("textureBindings");
		}
		
		public int vertexCount(){
			return stats.get("vertexCount");
		}
		
		public int triangleCount(){
			return vertexCount() / 3;
		}
		
		public void print(){
			StringBuffer buffer = new StringBuffer("\n");
			buffer.append(formatForGl("gl calls", calls()) + "\n");
			buffer.append(formatForGl("gl draw calls", drawCalls()) + "\n");
			buffer.append(formatForGl("gl shader switches", shaderSwitches()) + "\n");
			buffer.append(formatForGl("gl texture bindings", textureBindings()) + "\n");
			buffer.append(formatForGl("gl vertex count", vertexCount()) + "\n");
			buffer.append(formatForGl("gl triangle count", triangleCount()) + "\n");
			System.out.println(buffer.toString());
		}
		
		protected void updateTexts(){
			texts.get("calls").set(formatForGl("gl calls", calls()));
			texts.get("drawCalls").set(formatForGl("gl draw calls", drawCalls()));
			texts.get("shaderSwitches").set(formatForGl("gl shader switches", shaderSwitches()));
			texts.get("textureBindings").set(formatForGl("gl texture bindings", textureBindings()));
			texts.get("vertexCount").set(formatForGl("gl vertex count", vertexCount()));
			texts.get("triangleCount").set(formatForGl("gl triangle count", triangleCount()));
		}
		
		protected void updateStats(){
			stats.put("calls", calls - fields.get("calls"));
			stats.put("drawCalls", drawCalls - fields.get("drawCalls"));
			stats.put("shaderSwitches", shaderSwitches - fields.get("shaderSwitches"));
			stats.put("textureBindings", textureBindings - fields.get("textureBindings"));
			stats.put("vertexCount", (int)vertexCount.total - fields.get("vertexCount"));
		}
		
		public void updateFields(){
			fields.put("calls", calls);
			fields.put("drawCalls", drawCalls);
			fields.put("shaderSwitches", shaderSwitches);
			fields.put("textureBindings", textureBindings);
			fields.put("vertexCount", (int)vertexCount.total);
		}
		
	}
	
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
	private HashMap<String, Long> deltaTimes;
	private HashMap<String, Long> startTimes;
	private HashMap<String, Long> nanos;
	private HashMap<String, Float> percents;
	private ArrayList<Long> tickTimes;
	
	private GameObject display;
	private Text tickInfo;
	private HashMap<String, Text> texts;
	private HashMap<String, GameObject> bars;
	
	private float scale;
	private boolean visible;
	private boolean subsystemsVisible;
	private boolean glVisible;
	private boolean initialized;
	private Vector2f lastDisplaySize;
	private float verticalOffset;
	
	public Scene scene;
	
	public float avgTickRate;
	public float avgTickTime;
	public Gl gl;
	
	{
		totalStartTime = TimeUtils.nanoTime();
		deltaTimes = new HashMap<String, Long>();
		startTimes = new HashMap<String, Long>();
		nanos = new HashMap<String, Long>();
		percents = new HashMap<String, Float>();
		tickTimes = new ArrayList<Long>();
		for (int i=0; i < TICK_RATE; i++){
			tickTimes.add(1000000000L / TICK_RATE);
		}
		
		scale = 1f;
		initialized = false;
		avgTickRate = TICK_RATE;
		avgTickTime = 1000 / TICK_RATE;
		
		subsystemsVisible = true;
		glVisible = false;
		
		scene = null;

		gl = new Gl();
		gl.listener = GLErrorListener.THROWING_LISTENER;
	}
	
	private float verticalOffset(float factor){
		verticalOffset -= SPACING * factor;
		return verticalOffset;
	}
	
	private void addTextAndBar(String name){
		Vector3f position = new Vector3f(SPACING, verticalOffset, 0);
		texts.put(name, (Text)add("__PText", position));
		position.x = BAR_POSITION;
		bars.put(name, add("__PBar", position));
	}
	
	private void scaleBackground(){
		Vector3f sc = new Vector3f(BG_WIDTH, verticalOffset + SPACING * 0.5f, 1);
		display.children.get("__PBackground").scale(sc.mul(display.scale()));
	}
	
	private void initSubsystems(){
		String[] names = {
			"__graphics",
			"__input",
			"__logic",
			"__visuals",
			"__camera",
			"__worldstep",
			"__children",
			"__collisions",
			"__render",
			"__outside",
		};
		texts = new HashMap<String, Text>();
		bars = new HashMap<String, GameObject>();
		for (String name : names){
			addTextAndBar(name);
			verticalOffset(1);
		}
		verticalOffset(0.5f);
	}
	
	public void init(boolean framerateProfile){
		if (initialized){
			return;
		}
		
		initialized = true;
		verticalOffset = 0;
		visible = framerateProfile;
		if (visible){
			lastDisplaySize = Bdx.display.size();
			
			if (scene == null){
				scene = new Scene("__Profiler");
				scene.init();
				
				display = scene.add("__PDisplay");
				GameObject background = display.children.get("__PBackground");
				background.color(BG_COLOR);
				background.parent(display);
				
				verticalOffset(1.5f);
				tickInfo = (Text)add("__PText", new Vector3f(SPACING, verticalOffset, 0));
				verticalOffset(1.5f);
				
				updateScale();
			}
			
			if (glVisible){
				gl.enable();
				gl.initTexts();
			}
			
			if (subsystemsVisible){
				initSubsystems();
			}
			
			scaleBackground();
		}
	}
	
	public boolean visible(){
		return visible;
	}
	
	private void reinitialize(){
		if (scene != null){
			texts.clear();
			bars.clear();
			scene.end();
			scene = null;
		}
		initialized = false;
	}
		
	public void subsystemsVisible(boolean visible){
		if (!this.visible){
			System.err.println(ERR_MSG);
			return;
		}
		if (subsystemsVisible == visible){
			return;
		}
		subsystemsVisible = visible;
		reinitialize();
	}
	
	public void glVisible(boolean visible){
		if (!this.visible){
			System.err.println(ERR_MSG);
			return;
		}
		if (glVisible == visible){
			return;
		}
		glVisible = visible;
		reinitialize();
	}
	
	public void start(String name){
		startTimes.put(name, TimeUtils.nanoTime());
	}
	
	public float stop(String name){
		long stopTime = TimeUtils.nanoTime();
		long startTime = startTimes.containsKey(name) ? startTimes.get(name) : lastStopTime;
		long deltaTime = stopTime - startTime;
		
		if (subsystemsVisible){
			long storedDeltaTime = deltaTime;
			if (deltaTimes.containsKey(name)){
				storedDeltaTime += deltaTimes.get(name);
			}
			deltaTimes.put(name, storedDeltaTime);
			lastStopTime = stopTime;
		}
		return deltaTime * 0.000001f;
	}
	
	public void remove(String name){
		if (name.startsWith("__")){
			throw new RuntimeException(EXC_MSG);
		}
		deltaTimes.remove(name);
		startTimes.remove(name);
		nanos.remove(name);
		percents.remove(name);
		
		if (visible && subsystemsVisible){
			texts.get(name).end();
			texts.remove(name);
			bars.get(name).end();
			bars.remove(name);
			verticalOffset(-1);
			scaleBackground();
		}
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
		
		if (gl.isEnabled()){
			gl.updateStats();
		}
	}
	
	private GameObject add(String name, Vector3f position){
		GameObject obj = scene.add(name);
		Vector3f scale = display.scale();
		obj.scale(scale);
		obj.position(position.mul(scale));
		obj.parent(display);
		return obj;
	}
	
	private void updateSubsystems(){
		long sumDeltaTimes = 0;
		HashMap<String, Long> userNanos = new HashMap<String, Long>();
		HashMap<String, Float> userPercents = new HashMap<String, Float>();
		for (Entry<String, Long> e : deltaTimes.entrySet()){
			long deltaTime = e.getValue();
			String name = e.getKey();
			float deltaTimePercent = 100f * deltaTime / totalDeltaTime;
			if (name.startsWith("__")){
				sumDeltaTimes += deltaTime;
				percents.put(name, deltaTimePercent);
				nanos.put(name, deltaTime);
			}else{
				userPercents.put(name, deltaTimePercent);
				userNanos.put(name, deltaTime);
			}
		}
		long outsideDeltaTime = totalDeltaTime - sumDeltaTimes;
		float outsideTimePercent = 100f * outsideDeltaTime / totalDeltaTime;
		nanos.put("__outside", outsideDeltaTime);
		percents.put("__outside", outsideTimePercent);
		nanos.putAll(userNanos);
		percents.putAll(userPercents);
		startTimes.clear();
		deltaTimes.clear();
		
		for (String name : nanos.keySet()){
			if (!texts.containsKey(name)){
				addTextAndBar(name);
				verticalOffset(1);
				scaleBackground();
			}
			String n = name.startsWith("__") ? name.split("__")[1] : name;
			float m = nanos.get(name) * 0.000001f;
			float p = percents.get(name);
			Vector3f barScale = new Vector3f(BAR_WIDTH * p * 0.01f, BAR_HEIGHT, 1);
			bars.get(name).scale(barScale.mul(display.scale()));
			texts.get(name).set(formatForSubsystems(n, m, "ms", p, "%"));
		}
	}
	
	public void updateVisible(){
		if (!initialized){
			init(true);
		}
		
		tickInfo.set(formatForSubsystems("tick info", avgTickTime, "ms", avgTickRate, "fps"));

		if (subsystemsVisible){
			updateSubsystems();
		}
		
		if (glVisible){
			gl.updateTexts();
		}
		
		Vector2f ds = Bdx.display.size();
		if (!lastDisplaySize.equals(ds)){
			lastDisplaySize = ds;
			updateScale();
		}
	}
	
	private static String formatForGl(String name, int value){
		// "%-21s %7f"
		StringBuffer buffer = new StringBuffer();
		addString(buffer, name, 21, false, ' ');
		buffer.append(" ");
		addInt(buffer, value, 7, ' ');
		return buffer.toString();
	}
	
	private static String formatForSubsystems(String name, float avgTickTime, String timeUnits, float avgTickRate, String valueUnits){
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
	
	private static void addInt(StringBuffer buffer, int value, int fieldPadding, char character){
		addString(buffer, Integer.toString(value), fieldPadding - 1, true, character);
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
