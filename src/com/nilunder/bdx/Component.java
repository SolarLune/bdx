package com.nilunder.bdx;

import com.nilunder.bdx.utils.Named;

public class Component<T extends GameObject> implements Named {

	public State state;
	public String name;
	protected T g;
	public int logicFrequency;
	public float logicCounter;

	public Component(T g){
		this.g = g;
		name = this.getClass().getSimpleName();
		logicFrequency = Bdx.TICK_RATE;
		logicCounter = 1.0f;
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

	public void onGameObjectEnd(){}

}
