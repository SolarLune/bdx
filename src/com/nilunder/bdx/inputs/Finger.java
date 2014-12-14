package com.nilunder.bdx.inputs;

import com.badlogic.gdx.Gdx;

import com.nilunder.bdx.*;

public class Finger extends Pointer {

	public boolean hit;
	
	public Finger(int id){
		super(id);
	}

	public boolean hit(){
		return hit;
	}

	public boolean down(){
		return Gdx.input.isTouched(id);
	}
}
