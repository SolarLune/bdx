package com.nilunder.bdx;

public class Component {

	public State state;
	protected GameObject g;
	
	public Component(GameObject g){
		this.g = g;
	}
}
