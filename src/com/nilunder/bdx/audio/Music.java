package com.nilunder.bdx.audio;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.utils.Disposable;
import com.nilunder.bdx.*;

public class Music extends AudioStore<com.badlogic.gdx.audio.Music> implements Disposable {

	public Music(){
		super("bdx/audio/music");
	}

	@Override
	public com.badlogic.gdx.audio.Music loadAudio(String fileName){
		return Gdx.audio.newMusic(findFile(fileName));
	}

	public void dispose(){
		for (com.badlogic.gdx.audio.Music m : values())
			m.dispose();
	}

}
