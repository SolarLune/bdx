package com.nilunder.bdx.inputs;

import java.util.*;

import com.badlogic.gdx.InputAdapter;

public class GdxProcessor extends InputAdapter{

	private Keyboard keyboard;
	private Mouse mouse;
	private ArrayList<Finger> allocatedFingers;

	public GdxProcessor(Keyboard keyboard, Mouse mouse, ArrayList<Finger> allocatedFingers){
		this.keyboard = keyboard;
		this.mouse = mouse;
		this.allocatedFingers = allocatedFingers;
	}

	public boolean keyDown(int code){
		Keyboard.Log k = keyboard.codeToLog.get(code);
		if (k != null)
			k.hit = Keyboard.t + 1;
		return true;
	}

	public boolean keyUp(int code){
		Keyboard.Log k = keyboard.codeToLog.get(code);
		if (k != null)
			k.up = Keyboard.t + 1;
		return true;
	}

	public boolean touchDown(int x, int y, int id, int btn){
		allocatedFingers.get(id).hit = Keyboard.t + 1;
		mouse.codeToLog[btn].hit = Keyboard.t + 1;
		return true;
	}

	public boolean touchUp(int x, int y, int id, int btn){
		allocatedFingers.get(id).up = Keyboard.t + 1;
		mouse.codeToLog[btn].up = Keyboard.t + 1;
		return true;
	}
}
