package com.nilunder.bdx;

import com.nilunder.bdx.utils.Named;

public class Component<T extends GameObject> implements Named {

	public State state;
	public String name;
	protected T g;
	
	public Component(T g){
		this.g = g;
		name = this.getClass().getSimpleName();
	}

	@Override
	public String name() {
		return name;
	}
	
	public void state(State newState){
		if (state != null)
			state.exit();
		state = newState;
		if (state != null)
			state.enter();
	}
	
}
