package com.nilunder.bdx;

import com.nilunder.bdx.utils.Named;
import com.nilunder.bdx.utils.Random;

public class Component<T extends GameObject> implements Named {

	public State state;
	public String name;
	protected T g;
	public float logicFrequency;
	public float logicCounter;
	private static java.util.Random logicCounterRandom;

	public Component(T g){
		this.g = g;
		name = this.getClass().getSimpleName();
		logicFrequency = Bdx.TARGET_FPS;
		if (logicCounterRandom == null)
			logicCounterRandom = new java.util.Random();
		logicCounter = 1 + logicCounterRandom.nextFloat();
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
