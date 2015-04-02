package com.nilunder.bdx.inputs;

import java.util.*;

import com.nilunder.bdx.*;
import com.nilunder.bdx.inputs.*;


public class InputMaps extends HashMap<String, InputMaps.Inputs>{

public static class Input{

	private static class FnBool{
		public boolean eval(){
			return false;
		}
	}

	public FnBool[] hdu;

	public Input(String descriptor){
		hdu = new FnBool[3];

		final String[] d = descriptor.split(":");

		if (d[0].equals("k")){
			hdu[0] = new FnBool(){
				public boolean eval(){
					return Bdx.keyboard.keyHit(d[1]);
				}
			};
			hdu[1] = new FnBool(){
				public boolean eval(){
					return Bdx.keyboard.keyDown(d[1]);
				}
			};
			hdu[2] = new FnBool(){
				public boolean eval(){
					return Bdx.keyboard.keyUp(d[1]);
				}
			};
		}else if (d[0].equals("m")){
			hdu[0] = new FnBool(){
				public boolean eval(){
					return Bdx.mouse.btnHit(d[1]);
				}
			};
			hdu[1] = new FnBool(){
				public boolean eval(){
					return Bdx.mouse.btnDown(d[1]);
				}
			};
			hdu[2] = new FnBool(){
				public boolean eval(){
					return Bdx.mouse.btnUp(d[1]);
				}
			};
		}else if (d[0].equals("g")){
			hdu[0] = new FnBool(){
				public boolean eval(){
					return Bdx.gamepad.btnHit(d[1]);
				}
			};
			hdu[1] = new FnBool(){
				public boolean eval(){
					return Bdx.gamepad.btnDown(d[1]);
				}
			};
			hdu[2] = new FnBool(){
				public boolean eval(){
					return Bdx.gamepad.btnUp(d[1]);
				}
			};
		}else{
			throw new RuntimeException(String.format("Invalid descriptor \"%s\".", descriptor));
		}
	}

}

public static class Inputs extends ArrayList<Input>{

	public boolean status(int fnIdx){
		for (Input input : this){
			if (input.hdu[fnIdx].eval())
				return true;
		}
		return false;

	}
}

	public void put(String name, String... descriptors){
		Inputs ic = new Inputs();

		for (String d : descriptors){
			ic.add(new Input(d));
		}

		put(name, ic);
	}

	public boolean hit(String name){
		return get(name).status(0);
	}

	public boolean down(String name){
		return get(name).status(1);
	}

	public boolean up(String name){
		return get(name).status(2);
	}

}
