package com.nilunder.bdx.audio;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;

import java.util.ArrayList;

public class Music extends AudioStore<BDXMusic> implements Disposable {

	private float volume = 1;
	private float pan = 0;

	public Music(){
		super("bdx/audio/music");
	}

	@Override
	public BDXMusic loadAudio(String fileName){
		return new BDXMusic(findFile(fileName));
	}

	public void dispose(){
		for (BDXMusic m : values())
			m.dispose();
		clear();
	}

	public void volume(float volume){
		this.volume = volume;
		for (BDXMusic music : values())
			music.volume(music.volume());
	}

	public float volume() {
		return volume;
	}

	public void pan(float pan){
		this.pan = pan;
		for (BDXMusic music : values())
			music.pan(music.pan());
	}

	public float pan() {
		return pan;
	}

}
