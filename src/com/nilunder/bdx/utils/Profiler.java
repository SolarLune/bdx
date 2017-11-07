package com.nilunder.bdx.utils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.graphics.profiling.GLErrorListener;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.Text;
import com.nilunder.bdx.gl.Viewport;
import com.nilunder.bdx.gl.Mesh;

import javax.vecmath.Matrix4f;

public class Profiler{
	
	public class Gl extends com.badlogic.gdx.graphics.profiling.GLProfiler{
		
		protected LinkedHashMap<String, Integer> stats;
		private HashMap<String, Integer> fields;
		
		public Gl(){
			listener = GLErrorListener.THROWING_LISTENER;
			stats = new LinkedHashMap<String, Integer>();
			fields = new HashMap<String, Integer>();
			updateFields();
			updateStats();
		}
		
		public boolean active(){
			return isEnabled();
		}
		
		public void active(boolean active){
			if (active){
				enable();
				reset();
				updateFields();
			}else{
				disable();
			}
		}
		
		public void updateFields(){
			fields.put("calls", calls);
			fields.put("draw calls", drawCalls);
			fields.put("shader switches", shaderSwitches);
			fields.put("texture bindings", textureBindings);
			fields.put("vertex count", (int)vertexCount.total);
			fields.put("triangle count", (int)vertexCount.total / 3);
		}
		
		public void updateStats(){
			stats.put("calls", calls - fields.get("calls"));
			stats.put("draw calls", drawCalls - fields.get("draw calls"));
			stats.put("shader switches", shaderSwitches - fields.get("shader switches"));
			stats.put("texture bindings", textureBindings - fields.get("texture bindings"));
			stats.put("vertex count", (int)vertexCount.total - fields.get("vertex count"));
			stats.put("triangle count", (int)vertexCount.total / 3 - fields.get("triangle count"));
		}
		
		public int calls(){
			return stats.get("calls");
		}
		
		public int drawCalls(){
			return stats.get("draw calls");
		}
		
		public int shaderSwitches(){
			return stats.get("shader switches");
		}
		
		public int textureBindings(){
			return stats.get("texture bindings");
		}
		
		public int vertexCount(){
			return stats.get("vertex count");
		}
		
		public int triangleCount(){
			return stats.get("triangle count");
		}
		
	}
	
	private final String EXC_MSG = "User created subsystem names should not start with: \"__\"";
	private final Color BG_COLOR = new Color(0.125f, 0.125f, 0.125f, 0.75f);
	private final String SPACE = "  ";
	private final int TEXT_CAPACITY_BUFFER = 20;
	private final int MARGIN = 2;
	private final int OFFSET_RIGHT = 22;
	private final int BAR_WIDTH = 5;
	private final float CTE = 0.0225f;
	
	private float fontWidth;
	private float fontHeight;
	
	private int offsetLeft;
	
	private long totalStartTime;
	private long totalDeltaTime;
	private long lastStopTime;
	private HashMap<String, Long> deltaTimes;
	private HashMap<String, Long> startTimes;
	private ArrayList<Long> tickTimes;
	private float counter;
	private float scale;
	
	private boolean active;
	private boolean visible;
	private boolean tickInfoVisible;
	private boolean subsystemsVisible;
	private boolean glVisible;
	private boolean propsVisible;
	
	private StringBuffer textBuffer;
	private StringBuffer subsystemsBuffer;
	private StringBuffer propsBuffer;
	private StringBuffer glBuffer;
	
	private GameObject display;
	private Text text;
	private Text textProps;
	private GameObject bars;
	private GameObject background;
	
	public LinkedHashMap<String, Long> nanos;
	public LinkedHashMap<String, Float> percents;
	
	public int frequency;
	public float avgTickRate;
	public float avgTickTime;
	
	public Gl gl;
	public HashMap<String, Object> props;
	
	public Scene scene;
	
	public Profiler(){
		totalStartTime = TimeUtils.nanoTime();
		deltaTimes = new HashMap<String, Long>();
		startTimes = new HashMap<String, Long>();
		tickTimes = new ArrayList<Long>();
		for (int i = 0; i < Bdx.TICK_RATE; i++){
			tickTimes.add((long) Bdx.TICK_TIME);
		}
		counter = 1;
		scale = 1;
		
		tickInfoVisible = true;
		subsystemsVisible = true;
		glVisible = false;
		propsVisible = true;
		
		textBuffer = new StringBuffer();
		subsystemsBuffer = new StringBuffer();
		propsBuffer = new StringBuffer();
		glBuffer = new StringBuffer();
		
		nanos = new LinkedHashMap<String, Long>();
		percents = new LinkedHashMap<String, Float>();
		String[] subsystems = {
			"__render",
			"__logic",
			"__scene",
			"__physics",
			"__gpu wait",
			"__outside"
		};
		for (String name : subsystems){
			nanos.put(name, 0L);
			percents.put(name, 0f);
		}
		
		frequency = Bdx.TICK_RATE;
		avgTickRate = Bdx.TICK_RATE;
		avgTickTime = Bdx.TICK_TIME;
		
		gl = new Gl();
		props = new HashMap<String, Object>();
		
		scene = null;
	}
	
	public void init(){
		if (Bdx.profiler.scene != null){
			return;
		}
		scene = new Scene("__Profiler");
		scene.init();
		
		display = scene.objects.get("__PDisplay");
		
		text = (Text) scene.add("__PText");
		text.parent(display);
		text.text("");
		
		textProps = (Text) scene.add("__PText");
		textProps.parent(display);
		initTextProps();
		
		fontWidth = CTE * text.font.get("char").get("0").get("xadvance").asInt();
		fontHeight = CTE * text.font.get("common").get("lineHeight").asInt();
		updateOffsetLeft();
		
		bars = display.children.get("__PBars");
		initBars();

		background = scene.objects.get("__PBackground");
		background.mesh().materials.color(BG_COLOR);
		
		scene.viewport.type(Viewport.Type.SCREEN);
		updateViewport();
	}
	
	private void initTextProps(){
		textProps.capacity(TEXT_CAPACITY_BUFFER);
		textProps.text("");
	}
	
	private void updateTextProps(){
		textProps.position(0, -text.text().split("\n").length * fontHeight * scale, 0);
	}
	
	private void initBars(){
		if (!subsystemsVisible){
			return;
		}
		HashMap<Mesh, ArrayList<Matrix4f>> data = new HashMap<Mesh, ArrayList<Matrix4f>>();
		ArrayList<Matrix4f> transforms = new ArrayList<Matrix4f>();
		
		Matrix4f m = Matrix4f.identity();
		float fontHeightScaled = fontHeight * scale;
		float offsetX = (MARGIN + offsetLeft + OFFSET_RIGHT) * fontWidth * scale;
		float offsetY = -2 * fontHeightScaled;
		if (tickInfoVisible()){
			offsetY -= 2 * fontHeightScaled;
		}
		
		int i = 0;
		boolean separated = false;
		for (String name : nanos.keySet()){
			if (!separated && !name.startsWith("__")){
				i--;
				separated = true;
			}
			m.setM00(fontHeightScaled);
			m.setM11(fontHeightScaled);
			m.setM22(fontHeightScaled);
			m.setM03(offsetX);
			m.setM13(offsetY + fontHeightScaled * i--);
			transforms.add(new Matrix4f(m));
		}
		
		data.put(scene.meshes.get("__PBar"), transforms);
		bars.join(data);
	}
	
	private void updateBars(){
		if (!subsystemsVisible){
			return;
		}
		Mesh mesh = bars.mesh();
		float[] va = mesh.vertices();
		float f = fontWidth * BAR_WIDTH * 0.01f;
		int i = 0;
		for (float p : percents.values()){
			float offset = va[i] + p * f;
			i += Bdx.VERT_STRIDE;
			for (int j = 0; j < 3; j++){
				va[i] = offset;
				i += Bdx.VERT_STRIDE;
			}
			i += Bdx.VERT_STRIDE * 2;
		}
		mesh.vertices(va);
	}
	
	private void updateBackground(){
		int x = 0;
		int y = 0;
		String[] lines = text.text().split("\n");
		for (String line : lines){
			x = Math.max(x, line.length());
			y++;
		}
		if (propsVisible){
			lines = textProps.text().split("\n");
			for (String line : lines){
				x = Math.max(x, line.length());
				y++;
			}
		}
		if (subsystemsVisible){
			x = Math.max(x, MARGIN + offsetLeft + OFFSET_RIGHT + BAR_WIDTH);
		}
		float width = (x + MARGIN) * fontWidth * scale;
		float height = (y + 0.5f) * fontHeight * scale;
		background.scale(width, height, 1);
	}
	
	private void updateOffsetLeft(String name){
		offsetLeft = (int) Math.max(offsetLeft, name.length());
	}
	
	private void updateOffsetLeft(){
		offsetLeft = 0;
		for (String name : nanos.keySet()){
			updateOffsetLeft(name);
		}
	}
	
	public void updateViewport(float width, float height){
		scene.viewport.positionNormalized(0, 1 - scene.viewport.resolution().y * scene.viewport.sizeNormalized().y / height);
	}
	
	public void updateViewport(){
		updateViewport(Bdx.display.width(), Bdx.display.height());
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
		avgTickRate = Bdx.TICK_RATE * 1000000000f / sumTickTimes;
		avgTickTime = 1000 / avgTickRate;
	}
	
	public void updateSubsystems(){
		long sumDeltaTimes = 0;
		HashMap<String, Long> userNanos = new HashMap<String, Long>();
		HashMap<String, Float> userPercents = new HashMap<String, Float>();
		for (Map.Entry<String, Long> e : deltaTimes.entrySet()){
			long deltaTime = e.getValue();
			String name = e.getKey();
			if (name.equals("__gpu wait")){
				deltaTime = (long) Math.max(deltaTime - (Bdx.TICK_TIME * 1000000000), 0);
			}
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
	}
	
	private void updateText(){
		textBuffer.setLength(0);
		textBuffer.append("\n");
		if (tickInfoVisible){
			textBuffer.append("\n" + tickInfoAsString());
		}
		if (subsystemsVisible){
			textBuffer.append("\n" + subsystemsAsString());
		}
		if (glVisible){
			textBuffer.append("\n" + glAsString());
		}
		text.text(textBuffer.toString(), true);
		
		if (propsVisible){
			textBuffer.setLength(0);
			textBuffer.append("\n" + propsAsString());
			String s = textBuffer.toString();
			if (s.length() > textProps.capacity()){
				textProps.capacity(s.length() + TEXT_CAPACITY_BUFFER);
			}
			textProps.text(s);
		}
	}
	
	public void updateVisible(){
		if (counter >= 1){
			counter -= 1;
			updateText();
			updateBars();
			updateTextProps();
			updateBackground();
		}
		counter += frequency * Bdx.TICK_TIME;
		scene.viewport.apply();
	}
	
	private void addCustomSubsystem(String name){
		if (name.startsWith("__")){
			throw new RuntimeException(EXC_MSG);
		}
		nanos.put(name, 0L);
		updateOffsetLeft(name);
		initBars();
	}
	
	public void start(String name){
		if (!active){
			return;
		}
		if (!nanos.containsKey(name)){
			addCustomSubsystem(name);
		}
		startTimes.put(name, TimeUtils.nanoTime());
	}
	
	public float stop(String name){
		if (!active){
			return 0;
		}
		long stopTime = TimeUtils.nanoTime();
		long startTime;
		if (!startTimes.containsKey(name)){
			if (!nanos.containsKey(name)){
				addCustomSubsystem(name);
			}
			startTime = lastStopTime;
		}else{
			startTime = startTimes.get(name);
			startTimes.remove(name);
		}
		long deltaTime = stopTime - startTime;
		
		long storedDeltaTime = (long) (deltaTime * (float) frequency / Bdx.TICK_RATE);
		if (deltaTimes.containsKey(name)){
			storedDeltaTime += deltaTimes.get(name);
		}
		deltaTimes.put(name, storedDeltaTime);
		lastStopTime = stopTime;
		
		return deltaTime * 0.000001f;
	}
	
	public void remove(String name){
		if (!active){
			return;
		}
		if (name.startsWith("__")){
			throw new RuntimeException(EXC_MSG);
		}
		deltaTimes.remove(name);
		startTimes.remove(name);
		nanos.remove(name);
		percents.remove(name);
		updateOffsetLeft();
		initBars();
	}
	
	public boolean active(){
		return active;
	}

	public void active(boolean active){
		if (this.active == active){
			return;
		}
		this.active = active;
		if (active && Bdx.profiler.scene == null){
			init();
		}
	}
	
	public boolean visible(){
		return visible;
	}

	public void visible(boolean visible){
		if (this.visible == visible){
			return;
		}
		this.visible = visible;
		active(visible);
		text.visible(visible);
		if (subsystemsVisible){
			bars.visible(visible);
		}
		background.visible(visible);
	}

	public boolean tickInfoVisible(){
		return tickInfoVisible;
	}

	public void tickInfoVisible(boolean tickInfoVisible){
		if (!visible){
			return;
		}
		if (this.tickInfoVisible == tickInfoVisible){
			return;
		}
		this.tickInfoVisible = tickInfoVisible;
		initBars();
	}

	public boolean subsystemsVisible(){
		return subsystemsVisible;
	}
	
	public void subsystemsVisible(boolean subsystemsVisible){
		if (!visible){
			return;
		}
		if (this.subsystemsVisible == subsystemsVisible){
			return;
		}
		this.subsystemsVisible = subsystemsVisible;
		bars.visible(subsystemsVisible);
		initBars();
	}

	public boolean glVisible(){
		return glVisible;
	}
	
	public void glVisible(boolean glVisible){
		if (!visible){
			return;
		}
		if (this.glVisible == glVisible){
			return;
		}
		this.glVisible = glVisible;
		gl.active(glVisible);
	}

	public boolean propsVisible(){
		return propsVisible;
	}
	
	public void propsVisible(boolean propsVisible){
		if (!visible){
			return;
		}
		if (this.propsVisible == propsVisible){
			return;
		}
		this.propsVisible = propsVisible;
		initTextProps();
	}
	
	public float scale(){
		return scale;
	}
	
	public void scale(float scale){
		if (!visible){
			return;
		}
		if (this.scale == scale){
			return;
		}
		this.scale = scale;
		display.scale(scale);
		initBars();
	}
	
	public void end(){
		if (gl.active())
			gl.active(false);
		if (scene != null)
			scene.end();
	}
	
	public String tickInfoAsString(){
		return format("tick info", avgTickTime, "ms", avgTickRate, "fps") + "\n";
	}
	
	public String subsystemsAsString(){
		subsystemsBuffer.setLength(0);
		boolean separated = false;
		for (String name : nanos.keySet()){
			String n = name;
			if (name.startsWith("__")){
				n = n.split("__")[1];
			}else if (!separated){
				subsystemsBuffer.append("\n");
				separated = true;
			}
			float m = nanos.get(name) * 0.000001f;
			float p = percents.get(name);
			subsystemsBuffer.append(format(n, m, "ms", p, "%") + "\n");
		}
		return subsystemsBuffer.toString();
	}
	
	public String glAsString(){
		glBuffer.setLength(0);
		for (Map.Entry<String, Integer> e : gl.stats.entrySet()){
			glBuffer.append(format("gl " + e.getKey(), e.getValue()) + "\n");
		}
		return glBuffer.toString();
	}
	
	public String propsAsString(){
		propsBuffer.setLength(0);
		int padding = 0;
		for (String name : props.keySet()){
			padding = (int) Math.max(padding, name.length());
		}
		for (Map.Entry<String, Object> e : props.entrySet()){
			String s = e.getKey();
			propsBuffer.append(format(s, padding, String.valueOf(e.getValue())) + "\n");
		}
		return propsBuffer.toString();
	}
	
	private String format(String key, int padding, String value){
		StringBuffer buffer = new StringBuffer();
		if (!value.contains("\n")){
			buffer.append(SPACE);
			addString(buffer, key, padding + MARGIN, false, ' ');
			addString(buffer, value, value.length(), false, ' ');
		}else{
			String[] l = value.split("\n");
			int len = l.length;
			for (int i = 0; i < len; i++){
				buffer.append(SPACE);
				if (i == 0){
					addString(buffer, key, padding + MARGIN, false, ' ');
				}else{
					addString(buffer, "", padding + MARGIN, false, ' ');
				}
				String s = l[i];
				addString(buffer, s, s.length(), false, ' ');
				if (i != len - 1){
					buffer.append("\n");
				}
			}
		}
		return buffer.toString();
	}
	
	private String format(String name, int value){
		StringBuffer buffer = new StringBuffer(SPACE);
		addString(buffer, name, offsetLeft + 9, false, ' ');
		buffer.append(SPACE);
		addInt(buffer, value, 7, ' ');
		return buffer.toString();
	}
	
	private String format(String name, float avgTickTime, String timeUnits, float avgTickRate, String valueUnits){
		StringBuffer buffer = new StringBuffer(SPACE);
		addString(buffer, name, offsetLeft, false, ' ');
		buffer.append(SPACE);
		addFloat(buffer, avgTickTime, 4, 1, ' ');
		buffer.append(SPACE);
		addString(buffer, timeUnits, 3, false, ' ');
		buffer.append(SPACE);
		addFloat(buffer, avgTickRate, 4, 1, ' ');
		buffer.append(SPACE);
		buffer.append(valueUnits);
		return buffer.toString();
	}
	
	private void addInt(StringBuffer buffer, int value, int fieldPadding, char character){
		addString(buffer, Integer.toString(value), fieldPadding - 1, true, character);
	}
	
	private void addFloat(StringBuffer buffer, float value, int fieldPadding, int fractionPadding, char character){
		String converted = Float.toString(value);
		String [] split = converted.split("\\.");
		addString(buffer, split.length > 0 ? split[0] : "0", fieldPadding - (fractionPadding + 1), true, character);
		if (fractionPadding > 0){
			buffer.append(".");
			addString(buffer, split.length > 1 ? split[1] : "0", fractionPadding, false, '0');	
		}
	}
	
	private void addString(StringBuffer buffer, String value, int padding, boolean padLeft, char character){
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
	
	private void padWithCharacter(StringBuffer buffer, int padding, char character){
		for (int i = 0; i < padding; i++){
			buffer.append(character);
		}
	}
}
