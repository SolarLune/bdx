package com.nilunder.bdx.inputs;

import java.util.*;
import javax.vecmath.*;

import com.badlogic.gdx.controllers.*;
import com.badlogic.gdx.utils.*;

import com.nilunder.bdx.*;

public class Gamepad {

public static class Profile{
	public String name;
	public HashMap<String,Integer> btnToCode;
	public HashMap<Integer,GdxProcessor.UpDownLog> codeToLog;

	public HashMap<String,Integer> axisToCode;
	public HashMap<Integer,Float> axisValues;

	public HashMap<String,Integer> hatToCode;

	public int hatCode;

	public Profile(String name){
		this.name = name;
		btnToCode = new HashMap<String,Integer>();
		axisToCode = new HashMap<String,Integer>();
		hatToCode = new HashMap<String,Integer>();
		hatCode = 0;
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

		p.hatToCode.put("e", PovDirection.east.ordinal());
		p.hatToCode.put("ne", PovDirection.northEast.ordinal());
		p.hatToCode.put("n", PovDirection.north.ordinal());
		p.hatToCode.put("nw", PovDirection.northWest.ordinal());
		p.hatToCode.put("w", PovDirection.west.ordinal());
		p.hatToCode.put("sw", PovDirection.southWest.ordinal());
		p.hatToCode.put("s", PovDirection.south.ordinal());
		p.hatToCode.put("se", PovDirection.southEast.ordinal());

		profiles.put(p.name, p);

		p = new Profile("XBOX360");

		p.btnToCode.put("X", 2);
		p.btnToCode.put("Y", 3);
		p.btnToCode.put("A", 0);
		p.btnToCode.put("B", 1);
		p.btnToCode.put("LB", 4);
		p.btnToCode.put("RB", 5);
		p.btnToCode.put("back", 6);
		p.btnToCode.put("start", 7);
		p.btnToCode.put("RS", 8);
		p.btnToCode.put("LS", 9);

		p.axisToCode.put("lx", 1);
		p.axisToCode.put("ly", 0);
		p.axisToCode.put("rx", 2);
		p.axisToCode.put("ry", 3);
		p.axisToCode.put("LT", 4);
		p.axisToCode.put("RT", 5);

		p.hatToCode = profiles.get("XBOX").hatToCode;

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

	public boolean hat(String name){
		return profile.hatCode == profile.hatToCode.get(name);
	}

	public Vector3f dpad(){
		String hats[] = {"w", "nw", "n", "ne", "e", "se", "s", "sw"};
		Vector3f dir = new Vector3f(-1, 0, 0);
		Matrix3f qpi = Matrix3f.rotation(new Vector3f(0, 0, 1), -3.141592f/4);
		for (String h : hats){
			if (hat(h))
				return dir;
			qpi.transform(dir);
		}
		dir.scale(0);
		return dir;
	}

}
