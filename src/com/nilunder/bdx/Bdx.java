package com.nilunder.bdx;

import java.util.*;

import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.utils.ArrayListNamed;

public class Bdx{
	public static final float tick_time = 1f/60f;
	public static ArrayListNamed<Scene> scenes;
	public static Sounds sounds;
	public static Mouse mouse;
	public static Keyboard keyboard;
	public static ArrayList<Finger> fingers;

	private static ArrayList<Finger> fingers_last;
	private static ArrayList<Finger> _fingers;
	
	public static void init(){
		scenes = new ArrayListNamed<Scene>();
		sounds = new Sounds();
		mouse = new Mouse();
		keyboard = new Keyboard();
		fingers = new ArrayList<Finger>(); 
		fingers_last = new ArrayList<Finger>(); 

		_fingers = new ArrayList<Finger>();
		for (int i = 0; i < 10; ++i){
			_fingers.add(new Finger(i));
		}
	}

	public static void updateInput(){
		ArrayList<Finger> fl = fingers_last;
		fingers_last = fingers;
		fingers = fl;
		fingers.clear();

		for (Finger f : _fingers){
			if (f.down()){
				fingers.add(f);
				if (fingers_last.contains(f)){
					f.hit = false;
				}else{
					f.hit = true;
				}
			}
		}
	}
}
