package com.comp.project;

import java.util.*;

import com.badlogic.gdx.*;

import com.nilunder.bdx.*;

public class BdxApp implements ApplicationListener {

	public int TARGET_FPS = 60;

	@Override
	public void create(){
		Bdx.TARGET_FPS = this.TARGET_FPS;
		Bdx.init();

		Scene.instantiators = new HashMap<String, Instantiator>();
		Scene.instantiators.put("name", null);

		Bdx.scenes.add(new Scene("name"));
	}

	@Override
	public void dispose(){
		Bdx.dispose();
	}

	@Override
	public void render(){
		Bdx.main();
	}

	@Override
	public void resize(int width, int height){
		Bdx.resize(width, height);
	}

	@Override
	public void pause(){
	}

	@Override
	public void resume(){
	}
}
