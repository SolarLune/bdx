package com.nilunder.bdx.inputs;

import java.util.*;

import javax.vecmath.Vector2f;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

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

	public Vector2f position(){
		return new Vector2f(Gdx.input.getX(), Gdx.input.getY());
	}

	public void position(int x, int y) {
		Gdx.input.setCursorPosition(x, y);	
	}

	public void cursorLock(boolean confine) {
		Gdx.input.setCursorCatched(confine);	
	}

	public boolean cursorLock() {
		return Gdx.input.isCursorCatched();
	}

	public void setCursorImage(String textureName, int offsetX, int offsetY) {
		Pixmap pm = new Pixmap(Gdx.files.internal("bdx/textures/"+ textureName));
		Gdx.input.setCursorImage(pm, offsetX, offsetY);
		pm.dispose();
	}	
}
