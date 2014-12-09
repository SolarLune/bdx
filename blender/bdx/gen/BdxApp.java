package com.comp.project;

import java.util.HashMap;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.Instantiator;
import com.nilunder.bdx.utils.*;

public class BdxApp implements ApplicationListener {

	public PerspectiveCamera cam;
	public ModelBatch modelBatch;

	@Override
	public void create() {
		modelBatch = new ModelBatch();
		
		Bdx.init();
		Bdx.sounds.pathRoot("bdx/audio");

		Scene.instantiators = new HashMap<String, Instantiator>();
		Scene.instantiators.put("name", null);

		Bdx.scenes.add(new Scene("name"));
	}

	@Override
	public void dispose() {
		modelBatch.dispose();
	}

	@Override
	public void render() {
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		for (Scene s : (ArrayListNamed<Scene>)Bdx.scenes.clone()){
			s.update();
			renderScene(s);
		}
	}
	
	
	public void renderScene(Scene scene){
		Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
		modelBatch.begin(scene.cam);
		for (GameObject g : scene.objects){
			if (g.visible && g.modelInstance != null){
				modelBatch.render(g.modelInstance);
			}
		}
		modelBatch.end();
	}

	@Override
	public void resize(int width, int height) {
	}

	@Override
	public void pause() {
	}

	@Override
	public void resume() {
	}
}
