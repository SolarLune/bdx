package com.nilunder.bdx.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Disposable;
import com.nilunder.bdx.*;

import java.util.ArrayList;

public class Sounds extends AudioStore<Sound> implements Disposable{

	public Sounds(){
		super("bdx/audio/sounds");
	}

	@Override
	public Sound loadAudio(String fileName){
		return Gdx.audio.newSound(findFile(fileName));
	}

	public void dispose(){
		for (Sound s : values())
			s.dispose();
	}

	public ArrayList<String> available(){
		ArrayList<String> tracks = new ArrayList<String>();

		FileHandle[] files = Gdx.files.internal("bdx/audio/sounds/").list("");
		for (FileHandle file : files)
			tracks.add(file.nameWithoutExtension());

		return tracks;
	}

}
