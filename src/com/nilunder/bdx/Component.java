package com.nilunder.bdx;

import com.nilunder.bdx.utils.Named;
import com.nilunder.bdx.utils.Random;

public class Component<T extends GameObject> implements Named {

	public State state;
	public String name;
	protected T g;
	public float logicFrequency;
	public float logicCounter;

	public Component(T g){
		this.g = g;
		name = this.getClass().getSimpleName();
		logicFrequency = Bdx.TICK_RATE;
		logicCounter = 1 + Random.random();
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
