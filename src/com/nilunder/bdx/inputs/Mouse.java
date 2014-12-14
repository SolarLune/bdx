package com.nilunder.bdx.inputs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;

import com.nilunder.bdx.*;

public class Mouse extends Pointer{

	public boolean btnHit(String btn){
		if (!Gdx.input.justTouched()){
			return false;
		}
		return btnDown(btn);
	}

	public boolean btnDown(String btn){
		if (btn.equals("left")){
			return Gdx.input.isButtonPressed(Buttons.LEFT);
		}else if (btn.equals("right")){
			return Gdx.input.isButtonPressed(Buttons.RIGHT);
		}
		return false;
	}
	

	public boolean clicked(GameObject g){
		return clicked(g, "left");
	} 

	public boolean clicked(GameObject g, String btn){
		if (btnHit(btn)){
			RayHit rh = ray();
			if (rh != null && rh.object == g){
				return true;
			}
		}

		return false;
	} 
	
}
