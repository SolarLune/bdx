package com.nilunder.bdx;

import java.util.*;

import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.audio.*;
import com.nilunder.bdx.utils.ArrayListNamed;

public class Bdx{

	public static class ArrayListScenes extends ArrayListNamed<Scene>{
		public boolean add(Scene scene){
			boolean ret = super.add(scene);
			if (scene.objects == null)
				scene.init();
			return ret;
		}
		public void add(int index, Scene scene){
			super.add(index, scene);
			if (scene.objects == null)
				scene.init();
		}
	}

	public static final float TICK_TIME = 1f/60f;
	public static ArrayListScenes scenes;
	public static Sounds sounds;
	public static Music music;
	public static Mouse mouse;
	public static Keyboard keyboard;
	public static ArrayList<Finger> fingers;

	private static ArrayList<Finger> fingersLast;
	private static ArrayList<Finger> _fingers;
	
	public static void init(){
		scenes = new ArrayListScenes();
		sounds = new Sounds();
		music = new Music();
		mouse = new Mouse();
		keyboard = new Keyboard();
		fingers = new ArrayList<Finger>(); 
		fingersLast = new ArrayList<Finger>(); 

		_fingers = new ArrayList<Finger>();
		for (int i = 0; i < 10; ++i){
			_fingers.add(new Finger(i));
		}
	}

	public static void updateInput(){
		ArrayList<Finger> fl = fingersLast;
		fingersLast = fingers;
		fingers = fl;
		fingers.clear();

		for (Finger f : _fingers){
			if (f.down()){
				fingers.add(f);
				if (fingersLast.contains(f)){
					f.hit = false;
				}else{
					f.hit = true;
				}
			}
		}
	}
}
