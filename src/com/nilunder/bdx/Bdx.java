package com.nilunder.bdx;

import java.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.glutils.*;

import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.audio.*;
import com.nilunder.bdx.utils.*;

public class Bdx{

	public static class Display{
		public void fullscreen(boolean full){
			Graphics.DisplayMode dm = Gdx.graphics.getDesktopDisplayMode();
			Gdx.graphics.setDisplayMode(dm.width, dm.height, full);
		}
		public boolean fullscreen(){
			return Gdx.graphics.isFullscreen();
		}
		public void clearColor(float r, float g, float b, float a){
			Gdx.gl.glClearColor(r, g, b, a);
		}
	}

	public static class ArrayListScenes extends ArrayListNamed<Scene>{
		public boolean add(Scene scene){
			boolean ret = super.add(scene);
			if (scene.objects == null)
				scene.init();
			return ret;
		}
		public void add(int index, Scene scene){
			super.add(index, scene);
			if (scene.objects == null)
				scene.init();
		}
		public Scene set(int index, Scene scene){
			Scene old = remove(index);
			add(index, scene);
			return old;
		}
		public ArrayList<String> available(){
			ArrayList<String> scenes = new ArrayList<String>();
			FileHandle[] files = Gdx.files.internal("bdx/scenes/").list("bdx");
			for (FileHandle file : files){
				scenes.add(file.name().replace(".bdx", ""));
			}
			return scenes;
		}

	}

	public static final float TICK_TIME = 1f/60f;
	public static final int VERT_STRIDE = 8;
	public static float time;
	public static Profiler profiler;
	public static ArrayListScenes scenes;
	public static Display display;
	public static Sounds sounds;
	public static Music music;
	public static Mouse mouse;
	public static Keyboard keyboard;
	public static ArrayList<Finger> fingers;

	public static ArrayList<Finger> allocatedFingers;
	private static ModelBatch modelBatch;
	private static FrameBuffer frameBuffer;
	private static SpriteBatch spriteBatch;

	public static void init(){
		time = 0;
		profiler = new Profiler();
		display = new Display();
		scenes = new ArrayListScenes();
		sounds = new Sounds();
		music = new Music();
		mouse = new Mouse();
		keyboard = new Keyboard();
		fingers = new ArrayList<Finger>(); 

		allocatedFingers = new ArrayList<Finger>();
		for (int i = 0; i < 10; ++i){
			allocatedFingers.add(new Finger(i));
		}

		Gdx.input.setInputProcessor(new GdxProcessor(keyboard, mouse, allocatedFingers));

		modelBatch = new ModelBatch();

		ShaderProgram.pedantic = false;
		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		spriteBatch = new SpriteBatch();

	}


	public static void main(){
		profiler.start("__graphics");
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		profiler.stop("__graphics");

		// -------- Update Input --------
		time += TICK_TIME;
		++Keyboard.t;
		fingers.clear();
		for (Finger f : allocatedFingers){
			if (f.down() || f.up())
				fingers.add(f);
		}
		// ------------------------------
		
		profiler.stop("__input");

		for (Scene scene : (ArrayListNamed<Scene>)scenes.clone()){
			scene.update();

			profiler.start("__render");

			// ------- Render Scene --------
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
			// -----------------------------
			
			profiler.stop("__render");
		}

		profiler.update();
	}

	public static void dispose(){
		modelBatch.dispose();
		spriteBatch.dispose();
	}
}
