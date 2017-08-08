package com.nilunder.bdx.audio;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;

public class Sounds extends AudioStore<BDXSound> implements Disposable{

	private float volume = 1;
	private float pan = 0;
	private float pitch = 1;

	public Sounds(){
		super("bdx/audio/sounds");
	}

	@Override
	public BDXSound loadAudio(String fileName){
		return new BDXSound(findFile(fileName));
	}

	public void dispose(){
		for (BDXSound s : values())
			s.dispose();
		clear();
	}

	public void volume(float volume) {
		this.volume = volume;
	}

	public float volume(){
		return volume;
	}

	public void pan(float pan) {
		this.pan = pan;
	}

	public float pan() {
		return pan;
	}

	public void pitch(float pitch) {
		this.pitch = pitch;
	}

	public float pitch() {
		return pitch;
	}

}
