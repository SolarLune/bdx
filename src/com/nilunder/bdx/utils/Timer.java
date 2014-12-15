package com.nilunder.bdx.utils;

import com.badlogic.gdx.utils.TimeUtils;



public class Timer {
	
	public long timeLast;

	private long delta;

	public Timer(){
		this(1f);
	}
	
	public Timer(float secondsDelta){
		delta(secondsDelta);
		timeLast = 0;
	}

	public void delta(float secondsDelta){
		delta = (long)(secondsDelta * 1000);
	}

	public static double runningTime(){

		return TimeUtils.millis() / 1000d;

	}

	public boolean time(){
		long timeNow = TimeUtils.millis();
		if (timeNow - timeLast > delta){
			timeLast = timeNow;
			return true;
		}
		return false;
	}
}
