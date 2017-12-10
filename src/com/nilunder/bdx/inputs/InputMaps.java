package com.nilunder.bdx.inputs;

import java.util.*;
import java.util.regex.Pattern;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.nilunder.bdx.*;


public class InputMaps extends HashMap<String, InputMaps.Inputs> {

	public static class Input{

		public boolean forceHit;
		public boolean forceDown;
		public boolean forceUp;
		public String[] parsedDescriptor;
		
		public static class FnHDU{
			public FnBool[] eval(String d1){
				return null;
			}
		}

		public static class FnBool{
			public boolean eval(){
				return false;
			}
		}

		public FnBool[] hdu;

		public Input(){
			hdu = new FnBool[3];
		}

		public Input(String descriptor){
			this();

			final String[] d = descriptor.split(":");

			parsedDescriptor = d;

			if (d[0].contains("g") && d[0].length() == 1)  // There's not a gamepad id number, so add in "0" for the first
				parsedDescriptor = new String[]{d[0] + "0", d[1]};

			if (d[0].equals("k")){
				hdu[0] = new FnBool(){
					public boolean eval(){
						boolean r = Bdx.keyboard.keyHit(d[1]) || forceHit;
						forceHit = false;
						return r;
					}
				};
				hdu[1] = new FnBool(){
					public boolean eval(){
						boolean r = Bdx.keyboard.keyDown(d[1]) || forceDown;
						forceDown = false;
						return r;
					}
				};
				hdu[2] = new FnBool(){
					public boolean eval(){
						boolean r = Bdx.keyboard.keyUp(d[1]) || forceUp;
						forceUp = false;
						return r;
					}
				};
			}else if (d[0].equals("m")){
				hdu[0] = new FnBool(){
					public boolean eval(){
						boolean r = Bdx.mouse.btnHit(d[1]) || forceHit;
						forceHit = false;
						return r;
					}
				};
				hdu[1] = new FnBool(){
					public boolean eval(){
						boolean r = Bdx.mouse.btnDown(d[1]) || forceDown;
						forceDown = false;
						return r;
					}
				};
				hdu[2] = new FnBool(){
					public boolean eval(){
						boolean r = Bdx.mouse.btnUp(d[1]) || forceUp;
						forceUp = false;
						return r;
					}
				};
			}else if (Pattern.compile("^g[0-9]$").matcher(d[0]).find()){
				final int gpIndex;

				// GWT doesn't implement Character.getNumericValue(), so do this instead
				gpIndex = Integer.parseInt("" + parsedDescriptor[0].charAt(1), 36);

				hdu[0] = new FnBool(){
					public boolean eval(){
						if (Bdx.gamepads.size() > gpIndex) {
							boolean r = Bdx.gamepads.get(gpIndex).btnHit(d[1]) || forceHit;
							forceHit = false;
							return r;
						}
						return false;
					}
				};
				hdu[1] = new FnBool(){
					public boolean eval(){
						if (Bdx.gamepads.size() > gpIndex) {
							boolean r = Bdx.gamepads.get(gpIndex).btnDown(d[1]) || forceDown;
							forceDown = false;
							return r;
						}
						return false;
					}
				};
				hdu[2] = new FnBool(){
					public boolean eval(){
						if (Bdx.gamepads.size() > gpIndex) {
							boolean r = Bdx.gamepads.get(gpIndex).btnUp(d[1]) || forceUp;
							forceUp = false;
							return r;
						}
						return false;
					}
				};
			}else if (customDescriptorsHDU.containsKey(d[0])){
				hdu = customDescriptorsHDU.get(d[0]).eval(d[1]);
			}else{
				throw new RuntimeException("Invalid descriptor \"" + descriptor + "\".");
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

		public void forceInput(int inputType){
			for (Input input : this){
				if (inputType == 0)
					input.forceHit = true;
				else if (inputType == 1)
					input.forceDown = true;
				else if (inputType == 2)
					input.forceUp = true;
			}
		}
	}

	public static class Logger extends HashMap<String, Array<String>> {

		public int currentFrame;
		public int maxFrame;

		public void record(){

			Array<String> recordedInputs = new Array<String>();

			for (String input : Bdx.imaps.keySet()){

				if (Bdx.imaps.hit(input))
					recordedInputs.add(input + "_hit");
				if (Bdx.imaps.down(input))
					recordedInputs.add(input + "_down");
				if (Bdx.imaps.up(input))
					recordedInputs.add(input + "_up");
			}

			if (recordedInputs.size > 0) {
				if (currentFrame < maxFrame)
					put(Integer.toString(currentFrame), recordedInputs);
				else
					put(Integer.toString(currentFrame + 1), recordedInputs);
			}

			currentFrame++;
			maxFrame = Math.max(maxFrame, currentFrame);

		}

		public void clear(){
			super.clear();
			currentFrame = 0;
			maxFrame = 0;
		}

		public void play(){

			Array<String> recData = this.get(Integer.toString(currentFrame));

			if (!atLastFrame()) {

				if (recData != null) {

					for (String input : recData) {

						if (input.substring(0, input.lastIndexOf("_")).equals(""))
							continue;

						if (input.contains("_hit"))
							Bdx.imaps.forceHit(input.substring(0, input.indexOf("_hit")));
						if (input.contains("_down"))
							Bdx.imaps.forceDown(input.substring(0, input.indexOf("_down")));
						if (input.contains("_up"))
							Bdx.imaps.forceUp(input.substring(0, input.indexOf("_up")));

					}

				}

				currentFrame++;
			}
		}

		public boolean atLastFrame(){
			return currentFrame >= maxFrame;
		}

		public void save(String filePath){
			Json json = new Json();
			json.toJson(this, Gdx.files.external(filePath));

		}

		public void load(String filePath) {
			Json json = new Json();

			Logger l =  json.fromJson(Logger.class, Gdx.files.external(filePath));

			ArrayList<Integer> values = new ArrayList<Integer>();

			for (String k : l.keySet()) {
				values.add(Integer.valueOf(k));
			}
			Collections.sort(values);

			l.maxFrame = values.get(values.size() - 1);

			Bdx.imaps.logger = l;
		}

		public void remap(String inputName, String newInputName) {

			for (Array<String> keys : this.values()) {

				for (String k : new Array<String>(keys)) {

					keys.removeValue(k, true);
					String n = k.replace(inputName, newInputName);
					keys.add(n);

				}

			}

		}

	}

	public static Logger logger;
	public static HashMap<String, Input.FnHDU> customDescriptorsHDU;

	public InputMaps(){
		logger = new Logger();
		customDescriptorsHDU = new HashMap<String, Input.FnHDU>();
	}

	public void put(String name, String... descriptors){
		Inputs ic = new Inputs();

		for (String d : descriptors){
			ic.add(new Input(d));
		}

		put(name, ic);
	}

	public void put(String name, List<String> descriptors) {
		put(name, descriptors.toArray(new String[descriptors.size()]));
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

	public int iHit(String name){
		return hit(name)?1:0;
	}

	public int iDown(String name){
		return down(name)?1:0;
	}

	public int iUp(String name){
		return up(name)?1:0;
	}
	
	public void forceHit(String name){
		get(name).forceInput(0);
	}
	public void forceDown(String name){
		get(name).forceInput(1);
	}
	public void forceUp(String name){
		get(name).forceInput(2);
	}

}
