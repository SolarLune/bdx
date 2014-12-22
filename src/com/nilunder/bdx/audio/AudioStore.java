package com.nilunder.bdx.audio;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

abstract class AudioStore<T> extends HashMap<String,T>{
	
	private String pathRoot;
	
	public AudioStore(String pathRoot){
		pathRoot(pathRoot);
	}

	public void pathRoot(String pathRoot){
		this.pathRoot = pathRoot.endsWith("/") ? pathRoot : pathRoot + "/";
	}

	public T get(String name){
		T audio = super.get(name);
		if (audio == null){
			audio = loadAudio(name);
			this.put(name, audio);
		}
		return audio;
	}
	
	protected T loadAudio(String fileName){
		// return Gdx.audio.newSound(findFile(fileName));
		// -- OR --
		// return Gdx.audio.newMusic(findFile(fileName));
		return null;
	}
	
	protected FileHandle findFile(String name){
		String[] supported = {".wav", ".mp3", ".ogg"};
		String files = "";
		for (String ext : supported){
			FileHandle f = Gdx.files.internal(pathRoot + name + ext);
			if (f.exists())
				return f;
			files += name + ext + (ext.equals(".ogg") ? "" : " or ");
		}
		throw new GdxRuntimeException("Could not find " + files + " in " + pathRoot);
	}
}
