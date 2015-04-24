package com.nilunder.bdx;

import com.nilunder.bdx.utils.Named;

public class Component<T extends GameObject> implements Named {

	public State state;
	protected T g;
	
	public Component(T g){
		this.g = g;
	}

	@Override
	public String name() {
		return this.getClass().getSimpleName();
	}
	
	public void state(State newState){
		if (state != null)
			state.exit();
		state = newState;
		if (state != null)
			state.enter();
	}
	
}
