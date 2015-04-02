package com.nilunder.bdx.inputs;

import com.badlogic.gdx.Gdx;

import com.nilunder.bdx.*;

public class Finger extends Pointer {

	public int hit;
	public int up;
	
	public Finger(){
		this(0);
	}

	public Finger(int id){
		super(id);
		hit = 0;
		up = 0;
	}

	public boolean hit(){
		return hit == GdxProcessor.currentTick;
	}

	public boolean down(){
		return hit > up;
	}

	public boolean up(){
		return up == GdxProcessor.currentTick;
	}

}
