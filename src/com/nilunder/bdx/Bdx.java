package com.nilunder.bdx;

import com.nilunder.bdx.inputs.Mouse;
import com.nilunder.bdx.inputs.Keyboard;
import com.nilunder.bdx.services.Services;
import com.nilunder.bdx.utils.ArrayListNamed;

public class Bdx{
	public static final float tick_time = 1f/60f;
	public static ArrayListNamed<Scene> scenes;
	public static Sounds sounds;
	public static Mouse mouse;
	public static Keyboard keyboard;
	public static Services services;
	

	public static void init(){
		scenes = new ArrayListNamed<Scene>();
		sounds = new Sounds();
		mouse = new Mouse();
		keyboard = new Keyboard();
		if (services == null){
			services = new Services(true);
		}
		
	}
}
