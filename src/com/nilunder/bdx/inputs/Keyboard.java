package com.nilunder.bdx.inputs;

import java.util.*;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

import com.nilunder.bdx.*;

public class Keyboard {

	private HashMap<String,Integer> keyCodes;

	public Keyboard(){
		keyCodes = new HashMap<>();
		keyCodes.put("*", -1); // any key
		keyCodes.put("0", 7);
		keyCodes.put("1", 8);
		keyCodes.put("2", 9);
		keyCodes.put("3", 10);
		keyCodes.put("4", 11);
		keyCodes.put("5", 12);
		keyCodes.put("6", 13);
		keyCodes.put("7", 14);
		keyCodes.put("8", 15);
		keyCodes.put("9", 16);
		keyCodes.put("a", 29);
		keyCodes.put("lalt", 57);
		keyCodes.put("ralt", 58);
		keyCodes.put("b", 30);
		keyCodes.put("\\", 73);
		keyCodes.put("c", 31);
		keyCodes.put(",", 55);
		keyCodes.put("d", 32);
		keyCodes.put("backspace", 67);
		keyCodes.put("down", 20);
		keyCodes.put("left", 21);
		keyCodes.put("right", 22);
		keyCodes.put("up", 19);
		keyCodes.put("e", 33);
		keyCodes.put("enter", 66);
		keyCodes.put("=", 70);
		keyCodes.put("f", 34);
		keyCodes.put("g", 35);
		keyCodes.put("`", 68);
		keyCodes.put("h", 36);
		keyCodes.put("i", 37);
		keyCodes.put("j", 38);
		keyCodes.put("k", 39);
		keyCodes.put("l", 40);
		keyCodes.put("m", 41);
		keyCodes.put("-", 69);
		keyCodes.put("n", 42);
		keyCodes.put("o", 43);
		keyCodes.put("p", 44);
		keyCodes.put("q", 45);
		keyCodes.put("r", 46);
		keyCodes.put("s", 47);
		keyCodes.put(";", 74);
		keyCodes.put("lshift", 59);
		keyCodes.put("rshift", 60);
		keyCodes.put("/", 76);
		keyCodes.put("space", 62);
		keyCodes.put("t", 48);
		keyCodes.put("tab", 61);
		keyCodes.put("u", 49);
		keyCodes.put("v", 50);
		keyCodes.put("w", 51);
		keyCodes.put("x", 52);
		keyCodes.put("y", 53);
		keyCodes.put("z", 54);
		keyCodes.put("lctrl", 129);
		keyCodes.put("rctrl", 130);
		keyCodes.put("esc", 131);
	}

	public boolean keyHit(String key){
		return Gdx.input.isKeyJustPressed(keyCodes.get(key));
	}

	public boolean keyDown(String key){
		return Gdx.input.isKeyPressed(keyCodes.get(key));
	}

	public int kHit(String key){
		return Gdx.input.isKeyJustPressed(keyCodes.get(key))?1:0;
	}

	public int kDown(String key){
		return Gdx.input.isKeyPressed(keyCodes.get(key))?1:0;
	}
}
