package com.comp.project;

import java.util.HashMap;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.ModelBatch;

import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.Instantiator;
import com.nilunder.bdx.utils.*;
import com.nilunder.bdx.inputs.*;

public class BdxApp implements ApplicationListener {

	public PerspectiveCamera cam;
	public ModelBatch modelBatch;
	public FrameBuffer frameBuffer;
	public SpriteBatch spriteBatch;

	@Override
	public void create() {
		modelBatch = new ModelBatch();

		Bdx.init();
		Gdx.input.setInputProcessor(new GdxProcessor(Bdx.keyboard, Bdx.mouse, Bdx.allocatedFingers));

		Scene.instantiators = new HashMap<String, Instantiator>();
		Scene.instantiators.put("name", null);

		Bdx.scenes.add(new Scene("name"));

		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		spriteBatch = new SpriteBatch();
	}

	@Override
	public void dispose() {
		modelBatch.dispose();
		spriteBatch.dispose();
	}

	@Override
	public void render() {
		Bdx.profiler.start("__graphics");
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Bdx.profiler.stop("__graphics");

		Bdx.updateInput();
		Bdx.profiler.stop("__input");

		for (Scene s : (ArrayListNamed<Scene>)Bdx.scenes.clone()){
			s.update();
			Bdx.profiler.start("__render");
			renderScene(s);
			Bdx.profiler.stop("__render");
		}
		Bdx.profiler.update();
	}


	public void renderScene(Scene scene){

		if (scene.filter != null) {
			frameBuffer.begin();
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		}

		Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
		modelBatch.begin(scene.cam);
		for (GameObject g : scene.objects){
			if (g.visible()){
				modelBatch.render(g.modelInstance);
			}
		}
		modelBatch.end();

		if (scene.filter != null) {

			frameBuffer.end();
			Texture t = frameBuffer.getColorBufferTexture();
			t.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

			TextureRegion r = new TextureRegion(t);
			r.flip(false, true);

			spriteBatch.begin();
			spriteBatch.setShader(scene.filter);
			spriteBatch.draw(r, 0, 0);
			spriteBatch.end();

		}
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
