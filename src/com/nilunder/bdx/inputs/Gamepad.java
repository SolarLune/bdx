package com.nilunder.bdx.inputs;

import java.util.*;
import javax.vecmath.*;

import com.badlogic.gdx.controllers.*;
import com.badlogic.gdx.utils.*;

import com.nilunder.bdx.*;

public class Gamepad {

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

	public HashMap<String,Integer> axisToCode;
	public HashMap<Integer,Float> axisValues;

	public Profile(String name){
		this.name = name;
		btnToCode = new HashMap<String,Integer>();
		axisToCode = new HashMap<String,Integer>();
	}

	public GdxProcessor.UpDownLog btnLog(String btn){
		return codeToLog.get(btnToCode.get(btn));
	}
}

	public Controller controller;
	public Profile profile;

	private HashMap<String,Profile> profiles;

	public Gamepad(){
		Array controllers = Controllers.getControllers();
		if (controllers.size > 0)
			controller = (Controller)controllers.get(0);

		profiles = new HashMap<String,Profile>();

		Profile p = new Profile("XBOX");

		p.btnToCode.put("X", 3);
		p.btnToCode.put("Y", 4);
		p.btnToCode.put("A", 0);
		p.btnToCode.put("B", 1);
		p.btnToCode.put("white", 5);
		p.btnToCode.put("black", 2);
		p.btnToCode.put("back", 6);
		p.btnToCode.put("start", 7);
		p.btnToCode.put("RS", 8);
		p.btnToCode.put("LS", 9);

		p.axisToCode.put("lx", 0);
		p.axisToCode.put("ly", 1);
		p.axisToCode.put("rx", 3);
		p.axisToCode.put("ry", 4);
		p.axisToCode.put("LT", 2);
		p.axisToCode.put("RT", 5);

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
		p.btnToCode.put("ls-left", -200 - p.axisToCode.get("lx"));
		p.btnToCode.put("ls-right", 200 + p.axisToCode.get("lx"));
		p.btnToCode.put("ls-up", -200 - p.axisToCode.get("ly"));
		p.btnToCode.put("ls-down", 200 + p.axisToCode.get("ly"));

		p.btnToCode.put("rs-left", -200 - p.axisToCode.get("rx"));
		p.btnToCode.put("rs-right", 200 + p.axisToCode.get("rx"));
		p.btnToCode.put("rs-up", -200 - p.axisToCode.get("ry"));
		p.btnToCode.put("rs-down", 200 + p.axisToCode.get("ry"));

		p.btnToCode.put("RT", 200 + p.axisToCode.get("RT"));
		p.btnToCode.put("LT", 200 + p.axisToCode.get("LT"));

		profiles.put(p.name, p);

		p = new Profile("XBOX360");

		p.btnToCode = new HashMap<String,Integer>(profiles.get("XBOX").btnToCode);
		p.btnToCode.remove("white");
		p.btnToCode.remove("black");

		p.btnToCode.put("X", 2);
		p.btnToCode.put("Y", 3);
		p.btnToCode.put("LB", 4);
		p.btnToCode.put("RB", 5);

		p.axisToCode.put("lx", 1);
		p.axisToCode.put("ly", 0);
		p.axisToCode.put("rx", 2);
		p.axisToCode.put("ry", 3);
		p.axisToCode.put("LT", 4);
		p.axisToCode.put("RT", 5);

		p.btnToCode.put("ls-left", -200 - p.axisToCode.get("lx"));
		p.btnToCode.put("ls-right", 200 + p.axisToCode.get("lx"));
		p.btnToCode.put("ls-up", -200 - p.axisToCode.get("ly"));
		p.btnToCode.put("ls-down", 200 + p.axisToCode.get("ly"));

		p.btnToCode.put("rs-left", -200 - p.axisToCode.get("rx"));
		p.btnToCode.put("rs-right", 200 + p.axisToCode.get("rx"));
		p.btnToCode.put("rs-up", -200 - p.axisToCode.get("ry"));
		p.btnToCode.put("rs-down", 200 + p.axisToCode.get("ry"));

		p.btnToCode.put("RT", 200 + p.axisToCode.get("RT"));
		p.btnToCode.put("LT", 200 + p.axisToCode.get("LT"));

		// Unlike the original xbox, which uses one axis per trigger, 
		// xbox360 gamepads use a single axis for both triggers, where RT
		// values are positive, while LT values are negative. We convert 
		// to effectively place RT a different axis index (5), which enables us 
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

			profile.axisValues = new HashMap<>();
			for (Integer code : profile.axisToCode.values())
				profile.axisValues.put(code, 0.f);
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

	public float axis(String name){
		return profile.axisValues.get(profile.axisToCode.get(name));
	}

	public Vector3f stick(String name){
		if (name.equals("left"))
			return new Vector3f(axis("lx"), -axis("ly"), 0);
		return new Vector3f(axis("rx"), -axis("ry"), 0);
	}

}
