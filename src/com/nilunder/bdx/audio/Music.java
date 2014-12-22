package com.nilunder.bdx.audio;

import com.badlogic.gdx.Gdx;

import com.nilunder.bdx.*;

public class Music extends AudioStore<com.badlogic.gdx.audio.Music>{

	public Music(){
		super("bdx/audio/music");
	}

	@Override
	public com.badlogic.gdx.audio.Music loadAudio(String fileName){
		return Gdx.audio.newMusic(findFile(fileName));
	}
}
