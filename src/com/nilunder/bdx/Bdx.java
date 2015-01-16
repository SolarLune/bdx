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
	public static float time;
	public static ArrayListScenes scenes;
	public static Sounds sounds;
	public static Music music;
	public static Mouse mouse;
	public static Keyboard keyboard;
	public static ArrayList<Finger> fingers;
	public static ArrayList<Finger> allocatedFingers;

	public static void init(){
		time = 0;
		scenes = new ArrayListScenes();
		sounds = new Sounds();
		music = new Music();
		mouse = new Mouse();
		keyboard = new Keyboard();
		fingers = new ArrayList<Finger>(); 

		allocatedFingers = new ArrayList<Finger>();
		for (int i = 0; i < 10; ++i){
			allocatedFingers.add(new Finger(i));
		}
	}

	public static void updateInput(){
		time += TICK_TIME;
		++Keyboard.t;
		fingers.clear();
		for (Finger f : allocatedFingers){
			if (f.down() || f.up())
				fingers.add(f);
		}
	}
}
