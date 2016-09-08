package com.nilunder.bdx.audio;

import com.badlogic.gdx.Gdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import com.nilunder.bdx.*;

import java.util.ArrayList;

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

	public ArrayList<String> available(){
		ArrayList<String> tracks = new ArrayList<String>();

		FileHandle[] files = Gdx.files.internal("bdx/audio/music/").list("");
		for (FileHandle file : files)
			tracks.add(file.nameWithoutExtension());

		return tracks;
	}

}
