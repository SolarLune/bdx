package com.nilunder.bdx.inputs;

import java.util.*;
import javax.vecmath.*;

import com.badlogic.gdx.controllers.*;
import com.badlogic.gdx.utils.*;

import com.nilunder.bdx.*;

public class Gamepad {

public static class Axis{
	public int code;
	public float deadZone;
	public float value; 

	public Axis(int code, float deadZone){
		this.code = code;
		this.deadZone = deadZone;
	}

	public Axis(int code){
		this(code, 0.25f);
	}
}

public static class Stick{
	public Axis x;
	public Axis y;

	public Stick(Axis x, Axis y){
		this.x = x;
		this.y = y;
	}

	public Vector3f pos(){
		return new Vector3f(x.value, -y.value, 0);
	}

	public void deadZone(float dz){
		x.deadZone = dz;
		y.deadZone = dz;
	}
}

public static class Profile{

	public static class FnProcessAxis{
		public float[] eval(int axis, float value){
			return null;
		}
	}

	public FnProcessAxis processAxis;

	public String name;
	public HashMap<String,Integer> btnToCode;
	public HashMap<Integer,GdxProcessor.UpDownLog> codeToLog;

	public HashMap<String,Axis> axes;
	public HashMap<Integer,Axis> codeToAxis;

	public HashMap<String,Stick> sticks;

	public Profile(String name){
		this.name = name;
		btnToCode = new HashMap<String,Integer>();
		axes = new HashMap<String,Axis>();
		sticks = new HashMap<String,Stick>();
	}

	public GdxProcessor.UpDownLog btnLog(String btn){
		return codeToLog.get(btnToCode.get(btn));
	}
}

	public Controller controller;
	public Profile profile;
	public HashMap<String,Axis> axes;
	public HashMap<String,Stick> sticks;

	public HashMap<String,Profile> profiles;

	public Gamepad(){
		Array controllers = Controllers.getControllers();
		if (controllers.size > 0)
			controller = (Controller)controllers.get(0);

		profiles = new HashMap<String,Profile>();

		Profile p = new Profile("XBOX");

		p.btnToCode.put("x", 3);
		p.btnToCode.put("y", 4);
		p.btnToCode.put("a", 0);
		p.btnToCode.put("b", 1);
		p.btnToCode.put("white", 5);
		p.btnToCode.put("black", 2);
		p.btnToCode.put("back", 6);
		p.btnToCode.put("start", 7);
		p.btnToCode.put("rs", 8);
		p.btnToCode.put("ls", 9);

		p.axes.put("lx", new Axis(0));
		p.axes.put("ly", new Axis(1));
		p.axes.put("rx", new Axis(3));
		p.axes.put("ry", new Axis(4));
		p.axes.put("lt", new Axis(2));
		p.axes.put("rt", new Axis(5));

		p.sticks.put("left", new Stick(p.axes.get("lx"), p.axes.get("ly")));
		p.sticks.put("right", new Stick(p.axes.get("rx"), p.axes.get("ry")));

		// Each profile has a processAxis reference, which can be
		// set to a new FnProcessAxis function object, to convert
		// incoming axis codes and values before they're actually set. 
		// Here, we use it to convert awkward xbox trigger axis values 
		// (-1 when released, 1 when fully pressed) to something more
		// sensible (0 when released, 1 when fully pressed):
		//
		p.processAxis = new Profile.FnProcessAxis(){
			public float[] eval(int axis, float value){

				if (axis == 2 || axis == 5){ // "LT" or "RT"
					value = (value + 1) / 2;
				}

				return new float[]{axis, value};
			}
		};

		// The system will "buttonize" the dpad to 1XX button codes,
		// which can be mapped like this:
		//
		p.btnToCode.put("left", 100 + PovDirection.west.ordinal());
		p.btnToCode.put("right", 100 + PovDirection.east.ordinal());
		p.btnToCode.put("up", 100 + PovDirection.north.ordinal());
		p.btnToCode.put("down", 100 + PovDirection.south.ordinal());

		// Similarly for available axes, but with +/- 2XX button codes:
		//
		p.btnToCode.put("ls-left", -200 - p.axes.get("lx").code);
		p.btnToCode.put("ls-right", 200 + p.axes.get("lx").code);
		p.btnToCode.put("ls-up", -200 - p.axes.get("ly").code);
		p.btnToCode.put("ls-down", 200 + p.axes.get("ly").code);

		p.btnToCode.put("rs-left", -200 - p.axes.get("rx").code);
		p.btnToCode.put("rs-right", 200 + p.axes.get("rx").code);
		p.btnToCode.put("rs-up", -200 - p.axes.get("ry").code);
		p.btnToCode.put("rs-down", 200 + p.axes.get("ry").code);

		p.btnToCode.put("rt", 200 + p.axes.get("rt").code);
		p.btnToCode.put("lt", 200 + p.axes.get("lt").code);

		profiles.put(p.name, p);

		p = new Profile("XBOX360");

		p.btnToCode = new HashMap<String,Integer>(profiles.get("XBOX").btnToCode);
		p.btnToCode.remove("white");
		p.btnToCode.remove("black");

		p.btnToCode.put("x", 2);
		p.btnToCode.put("y", 3);
		p.btnToCode.put("lb", 4);
		p.btnToCode.put("rb", 5);

		p.axes.put("lx", new Axis(1));
		p.axes.put("ly", new Axis(0));
		p.axes.put("rx", new Axis(2));
		p.axes.put("ry", new Axis(3));
		p.axes.put("lt", new Axis(4));
		p.axes.put("rt", new Axis(5));

		p.sticks.put("left", new Stick(p.axes.get("lx"), p.axes.get("ly")));
		p.sticks.put("right", new Stick(p.axes.get("rx"), p.axes.get("ry")));

		p.btnToCode.put("ls-left", -200 - p.axes.get("lx").code);
		p.btnToCode.put("ls-right", 200 + p.axes.get("lx").code);
		p.btnToCode.put("ls-up", -200 - p.axes.get("ly").code);
		p.btnToCode.put("ls-down", 200 + p.axes.get("ly").code);

		p.btnToCode.put("rs-left", -200 - p.axes.get("rx").code);
		p.btnToCode.put("rs-right", 200 + p.axes.get("rx").code);
		p.btnToCode.put("rs-up", -200 - p.axes.get("ry").code);
		p.btnToCode.put("rs-down", 200 + p.axes.get("ry").code);

		p.btnToCode.put("rt", 200 + p.axes.get("rt").code);
		p.btnToCode.put("lt", 200 + p.axes.get("lt").code);

		// Unlike the original xbox, which uses one axis per trigger, 
		// xbox360 gamepads use a single axis for both triggers, where RT
		// values are positive, while LT values are negative. We convert 
		// to effectively place RT on a different axis index (5), which enables us 
		// to use positive values for both triggers.
		// 
		p.processAxis = new Profile.FnProcessAxis(){
			public float[] eval(int axis, float value){
				if (axis == 4 && value < 0){
					axis = 5; // RT
					value = -value;
				}
				return new float[]{axis, value};
			}
		};

		profiles.put(p.name, p);

		profile("XBOX360"); // probably most common, so it's the default

	}

	public void profile(String name){
		profile = profiles.get(name);
		if (profile.codeToLog == null){
			profile.codeToLog = new HashMap<>();
			for (Integer code : profile.btnToCode.values())
				profile.codeToLog.put(code, new GdxProcessor.UpDownLog());

			profile.codeToAxis = new HashMap<>();
			for (Axis axis : profile.axes.values())
				profile.codeToAxis.put(axis.code, axis);

			axes = profile.axes;
			sticks = profile.sticks;
		}
	}


	public boolean btnHit(String btn){
		GdxProcessor.UpDownLog b = profile.btnLog(btn);
		return b.hit == GdxProcessor.currentTick;
	}

	public boolean btnDown(String btn){
		GdxProcessor.UpDownLog b = profile.btnLog(btn);
		return b.hit > b.up;
	}

	public boolean btnUp(String btn){
		GdxProcessor.UpDownLog b = profile.btnLog(btn);
		return b.up == GdxProcessor.currentTick;
	}
}
