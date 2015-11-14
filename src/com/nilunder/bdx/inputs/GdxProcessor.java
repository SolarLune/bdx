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
	private Integer[] lastPressedHats;
	private HashMap<Integer,Float> lastAxisBtnState;
	private HashMap<Integer,Integer[]> hatMap;

	public GamepadAdapter(Gamepad gamepad){
		this.gamepad = gamepad;
		hatMap = new HashMap<Integer,Integer[]>();
		lastAxisBtnState = new HashMap<Integer,Float>();

		int e = PovDirection.east.ordinal();
		int ne = PovDirection.northEast.ordinal();
		int n = PovDirection.north.ordinal();
		int nw = PovDirection.northWest.ordinal();
		int w = PovDirection.west.ordinal();
		int sw = PovDirection.southWest.ordinal();
		int s = PovDirection.south.ordinal();
		int se = PovDirection.southEast.ordinal();

		hatMap.put(ne, new Integer[]{n, e});
		hatMap.put(nw, new Integer[]{n, w});
		hatMap.put(sw, new Integer[]{s, w});
		hatMap.put(se, new Integer[]{s, e});
		hatMap.put(n, new Integer[]{n});
		hatMap.put(s, new Integer[]{s});
		hatMap.put(e, new Integer[]{e});
		hatMap.put(w, new Integer[]{w});
		hatMap.put(0, new Integer[]{0});

		lastPressedHats = hatMap.get(0);
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
		if (gamepad.profile.processAxis != null){
			float[] code_value = gamepad.profile.processAxis.eval(code, value);
			code = (int)code_value[0];
			value = code_value[1];
		}

		Gamepad.Axis axis = gamepad.profile.codeToAxis.get(code);
		if (axis != null){
			float absVal = Math.abs(value);
			if (absVal > axis.deadZone){
				float nv = (absVal - axis.deadZone) / (1 - axis.deadZone);
				value = value < 0 ? -nv : nv;
			}else{
				value = 0;
			}
			axis.value = value;
		}

		code = value >= 0 ? 200 + code : -200 - code;
		GdxProcessor.UpDownLog b = gamepad.profile.codeToLog.get(code);
		if (b != null){
			float trigger = 0.35f;

			value = Math.abs(value);
			Float lastVal = lastAxisBtnState.get(code);

			if (lastVal == null)
				lastVal = new Float(0);

			if (value != 0){
				if (lastVal == 0)
					b.hit = GdxProcessor.tick();
			}else{
				if (lastVal != 0)
					b.up = GdxProcessor.tick();
			}

			lastAxisBtnState.put(code, value);

			GdxProcessor.UpDownLog bb = gamepad.profile.codeToLog.get(-code);
			if (bb != null){
				if (bb.hit > bb.up){
					bb.up = GdxProcessor.tick();
					lastAxisBtnState.put(-code, 0f);
				}
			}
		}
		return true;
	}

	public boolean povMoved(Controller controller, int code, PovDirection dir){
		Integer[] pressedHats = hatMap.get(dir.ordinal());

		GdxProcessor.UpDownLog b;

		for (int hat : pressedHats){
			if (!Arrays.asList(lastPressedHats).contains(hat)){
				b = gamepad.profile.codeToLog.get(100 + hat);
				if (b != null){
					b.hit = GdxProcessor.tick();
				}
			}
		}
		
		for (int hat : lastPressedHats){
			if (!Arrays.asList(pressedHats).contains(hat)){
				b = gamepad.profile.codeToLog.get(100 + hat);
				if (b != null){
					b.up = GdxProcessor.tick();
				}
			}
		}

		lastPressedHats = pressedHats;
		return true;
	}
}

	private Keyboard keyboard;
	private Mouse mouse;
	private ArrayList<Finger> allocatedFingers;

	public GdxProcessor(Keyboard keyboard, Mouse mouse, ArrayList<Finger> allocatedFingers, ArrayList<Gamepad> gamepads){
		this.keyboard = keyboard;
		this.mouse = mouse;
		this.allocatedFingers = allocatedFingers;

		for (Gamepad g : gamepads){
			if (g.controller != null)
				g.controller.addListener(new GamepadAdapter(g));
		}

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

	public boolean scrolled(int amount){
		mouse.wheelMove = amount;
		return true;
	}

	private static int tick(){
		return GdxProcessor.currentTick + 1;
	}
}
