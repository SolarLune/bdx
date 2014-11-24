package com.nilunder.bdx;

import java.util.HashMap;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class Sounds extends HashMap<String,Sound>{
	
	private String pathRoot;
	
	public void pathRoot(String pathRoot){
		this.pathRoot = pathRoot.endsWith("/") ? pathRoot : pathRoot + "/";
	}

	public Sound get(String name){
		Sound s = super.get(name);
		if (s == null){
			s = loadSound(name);
			this.put(name, s);
		}
		return s;
	}
	
	private Sound loadSound(String soundName){
		return Gdx.audio.newSound(findFile(soundName));
	}
	
	private FileHandle findFile(String soundName){
		String[] supported = {".wav", ".mp3", ".ogg"};
		String files = "";
		for (String ext : supported){
			FileHandle f = Gdx.files.internal(pathRoot + soundName + ext);
			if (f.exists())
				return f;
			files += soundName + ext + (ext.equals(".ogg") ? "" : " or ");
		}
		throw new GdxRuntimeException("Could not find " + files + " in " + pathRoot);
	}
}
