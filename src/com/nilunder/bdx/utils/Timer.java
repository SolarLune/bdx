package com.nilunder.bdx.utils;

import com.nilunder.bdx.Bdx;

public class Timer {
	
	public float interval;

	private boolean paused;
	private float delta;

	private float timeLast;

	public Timer(){
		this(1f);
	}
	
	public Timer(float interval){
		set(interval);
	}

	public void set(float interval){
		this.interval = interval;
		restart();
	}

	public float time(){
		float f = Bdx.time - timeLast;
		if (paused) f = delta;
		return Math.round(f * 10000.0f) / 10000.0f;
	}

	public float timeLeft(){
		return interval - time();
	}

	public void restart(){
		timeLast = Bdx.time;
	}

	public void pause(){
		delta = time();
		paused = true;
	}

	public boolean paused(){
		return paused;
	}


	public void resume(){
		if (paused){
			timeLast = Bdx.time - delta;
			paused = false;
		}
	}

	public boolean done(){
		return time() >= interval;
	}

	public void done(boolean done){
		if (done)
			timeLast -= interval + 1;
		else
			restart();
	}

	public boolean tick(){
		boolean d = done();
		if (d) restart();
		return d;
	}
}
