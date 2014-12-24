package com.nilunder.bdx.inputs;

import java.util.*;

import com.badlogic.gdx.Gdx;

import com.nilunder.bdx.*;

public class Mouse extends Finger{

	public Keyboard.Log[] codeToLog;

	private HashMap<String,Integer> btnToCode;

	public Mouse(){
		codeToLog = new Keyboard.Log[5];
		for (int i = 0; i < 5; ++i)
			codeToLog[i] = new Keyboard.Log();

		btnToCode = new HashMap<>();
		btnToCode.put("left", 0);
		btnToCode.put("right", 1);
		btnToCode.put("middle", 2);
		btnToCode.put("back", 3);
		btnToCode.put("forward", 4);
	}

	public boolean btnHit(String btn){
		Keyboard.Log b = codeToLog[btnToCode.get(btn)];
		return b.hit == Keyboard.t;
	}

	public boolean btnDown(String btn){
		Keyboard.Log b = codeToLog[btnToCode.get(btn)];
		return b.hit > b.up;
	}

	public boolean btnUp(String btn){
		Keyboard.Log b = codeToLog[btnToCode.get(btn)];
		return b.up == Keyboard.t;
	}
	

	public boolean clicked(GameObject g){
		return clicked(g, "left");
	} 

	public boolean clicked(GameObject g, String btn){
		if (btnHit(btn)){
			RayHit rh = ray();
			if (rh != null && rh.object == g){
				return true;
			}
		}

		return false;
	} 
	
}
