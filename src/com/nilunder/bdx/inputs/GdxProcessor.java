package com.nilunder.bdx.inputs;

import java.util.*;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.controllers.*;

public class GdxProcessor extends InputAdapter{

public static int currentTick = 0;

public static class UpDownLog{

	public int hit;
	public int up;

	public UpDownLog(){
		hit = 0;
		up = 0;
	}
	
}

private static class GamepadAdapter extends ControllerAdapter{

	private Gamepad gamepad;

	public GamepadAdapter(Gamepad gamepad){
		this.gamepad = gamepad;
	}

	public boolean buttonDown(Controller controller, int code){
		GdxProcessor.UpDownLog b = gamepad.profile.codeToLog.get(code);
		if (b != null){
			b.hit = GdxProcessor.tick();
		}
		return true;
	}

	public boolean buttonUp(Controller controller, int code){
		GdxProcessor.UpDownLog b = gamepad.profile.codeToLog.get(code);
		if (b != null){
			b.up = GdxProcessor.tick();
		}
		return true;
	}

	public boolean axisMoved(Controller controller, int code, float value){
		if (gamepad.profile.name.equals("XBOX360")){
			if (code == 4 && value < 0)
				++code;
		}
		gamepad.profile.axisValues.put(code, value);
		return true;
	}

	public boolean povMoved(Controller controller, int code, PovDirection dir){
		gamepad.profile.hatCode = dir.ordinal();
		return true;
	}
}

	private Keyboard keyboard;
	private Mouse mouse;
	private ArrayList<Finger> allocatedFingers;

	public GdxProcessor(Keyboard keyboard, Mouse mouse, ArrayList<Finger> allocatedFingers, Gamepad gamepad){
		this.keyboard = keyboard;
		this.mouse = mouse;
		this.allocatedFingers = allocatedFingers;
		if (gamepad.controller != null)
			gamepad.controller.addListener(new GamepadAdapter(gamepad));
	}

	public boolean keyDown(int code){
		UpDownLog k = keyboard.codeToLog.get(code);
		if (k != null)
			k.hit = tick();
		return true;
	}

	public boolean keyUp(int code){
		UpDownLog k = keyboard.codeToLog.get(code);
		if (k != null)
			k.up = tick();
		return true;
	}

	public boolean touchDown(int x, int y, int id, int btn){
		allocatedFingers.get(id).hit = tick();
		mouse.codeToLog[btn].hit = tick();
		return true;
	}

	public boolean touchUp(int x, int y, int id, int btn){
		allocatedFingers.get(id).up = tick();
		mouse.codeToLog[btn].up = tick();
		return true;
	}

	private static int tick(){
		return GdxProcessor.currentTick + 1;
	}
}
