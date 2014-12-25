package com.nilunder.bdx.inputs;

import java.util.*;

import com.badlogic.gdx.Gdx;

import com.nilunder.bdx.*;

public class Keyboard {

	public static class Log{

		public int hit;
		public int up;

		public Log(){
			hit = 0;
			up = 0;
		}
		
	}

	public static int t;

	public HashMap<Integer, Log> codeToLog;

	private HashMap<String,Integer> keyToCode;

	public Keyboard(){
		t = 0;

		keyToCode = new HashMap<>();
		keyToCode.put("*", -1); // any key
		keyToCode.put("0", 7);
		keyToCode.put("1", 8);
		keyToCode.put("2", 9);
		keyToCode.put("3", 10);
		keyToCode.put("4", 11);
		keyToCode.put("5", 12);
		keyToCode.put("6", 13);
		keyToCode.put("7", 14);
		keyToCode.put("8", 15);
		keyToCode.put("9", 16);
		keyToCode.put("a", 29);
		keyToCode.put("lalt", 57);
		keyToCode.put("ralt", 58);
		keyToCode.put("b", 30);
		keyToCode.put("\\", 73);
		keyToCode.put("c", 31);
		keyToCode.put(",", 55);
		keyToCode.put("d", 32);
		keyToCode.put("backspace", 67);
		keyToCode.put("down", 20);
		keyToCode.put("left", 21);
		keyToCode.put("right", 22);
		keyToCode.put("up", 19);
		keyToCode.put("e", 33);
		keyToCode.put("enter", 66);
		keyToCode.put("=", 70);
		keyToCode.put("f", 34);
		keyToCode.put("g", 35);
		keyToCode.put("`", 68);
		keyToCode.put("h", 36);
		keyToCode.put("i", 37);
		keyToCode.put("j", 38);
		keyToCode.put("k", 39);
		keyToCode.put("l", 40);
		keyToCode.put("m", 41);
		keyToCode.put("-", 69);
		keyToCode.put("n", 42);
		keyToCode.put("o", 43);
		keyToCode.put("p", 44);
		keyToCode.put("q", 45);
		keyToCode.put("r", 46);
		keyToCode.put("s", 47);
		keyToCode.put(";", 74);
		keyToCode.put("lshift", 59);
		keyToCode.put("rshift", 60);
		keyToCode.put("/", 76);
		keyToCode.put("space", 62);
		keyToCode.put("t", 48);
		keyToCode.put("tab", 61);
		keyToCode.put("u", 49);
		keyToCode.put("v", 50);
		keyToCode.put("w", 51);
		keyToCode.put("x", 52);
		keyToCode.put("y", 53);
		keyToCode.put("z", 54);
		keyToCode.put("lctrl", 129);
		keyToCode.put("rctrl", 130);
		keyToCode.put("esc", 131);
		keyToCode.put("f1", 244);
		keyToCode.put("f2", 245);
		keyToCode.put("f3", 246);
		keyToCode.put("f4", 247);
		keyToCode.put("f5", 248);
		keyToCode.put("f6", 249);
		keyToCode.put("f7", 250);
		keyToCode.put("f8", 251);
		keyToCode.put("f9", 252);
		keyToCode.put("f10", 253);
		keyToCode.put("f11", 254);
		keyToCode.put("f12", 255);
		keyToCode.put("numpad0", 144);
		keyToCode.put("numpad1", 145);
		keyToCode.put("numpad2", 146);
		keyToCode.put("numpad3", 147);
		keyToCode.put("numpad4", 148);
		keyToCode.put("numpad5", 149);
		keyToCode.put("numpad6", 150);
		keyToCode.put("numpad7", 151);
		keyToCode.put("numpad8", 152);
		keyToCode.put("numpad9", 153);
		keyToCode.put("[", 71);
		keyToCode.put("]", 72);
		keyToCode.put(".", 56);
		keyToCode.put("+", 81);

		codeToLog = new HashMap<>();
		for (Integer code : keyToCode.values())
			codeToLog.put(code, new Log());

	}

	public ArrayList<String> downKeys() {

		ArrayList<String> keyNames = new ArrayList<String>();

		for (String keyName : keyToCode.keySet()){

			if (Gdx.input.isKeyPressed(keyToCode.get(keyName)))	keyNames.add(keyName);

		}

		return keyNames;

	}

	public ArrayList<String> hitKeys() {

		ArrayList<String> keyNames = new ArrayList<String>();

		for (String keyName : keyToCode.keySet()){

			if (Gdx.input.isKeyJustPressed(keyToCode.get(keyName)))	keyNames.add(keyName);

		}

		return keyNames;

	}

	public boolean keyHit(String key){
		Log k = codeToLog.get(keyToCode.get(key));
		return k.hit == t;
	}

	public boolean keyDown(String key){
		Log k = codeToLog.get(keyToCode.get(key));
		return k.hit > k.up;
	}

	public boolean keyUp(String key){
		Log k = codeToLog.get(keyToCode.get(key));
		return k.up == t;
	}

	public int kHit(String key){
		return keyHit(key)?1:0;
	}

	public int kDown(String key){
		return keyDown(key)?1:0;
	}

	public int kUp(String key){
		return keyUp(key)?1:0;
	}

}
