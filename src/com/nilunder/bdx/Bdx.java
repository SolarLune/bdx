package com.nilunder.bdx;

import java.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.glutils.*;

import com.nilunder.bdx.*;
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
		public Scene add(String name){
			Scene scene = new Scene(name);
			add(scene);
			return scene;
		}
		public Scene add(int index, String name){
			Scene scene = new Scene(name);
			add(index, scene);
			return scene;
		}
		public Scene set(int index, Scene scene){
			Scene old = remove(index);
			add(index, scene);
			return old;
		}
		public Scene set(int index, String name){
			Scene scene = new Scene(name);
			set(index, scene);
			return scene;
		}
		public Scene remove(String name){
			int index = indexOf(get(name));
			return remove(index);
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
	public static Gamepad gamepad;
	public static InputMaps imaps;
	public static Keyboard keyboard;
	public static ArrayList<Finger> fingers;
	public static ArrayList<Component> components;

	private static ArrayList<Finger> allocatedFingers;
	private static ModelBatch modelBatch;
	private static RenderBuffer frameBuffer;
	private static SpriteBatch spriteBatch;

	public static void init(){
		time = 0;
		profiler = new Profiler();
		display = new Display();
		scenes = new ArrayListScenes();
		sounds = new Sounds();
		music = new Music();
		mouse = new Mouse();
		gamepad = new Gamepad();
		imaps = new InputMaps();
		keyboard = new Keyboard();
		fingers = new ArrayList<Finger>(); 
		components = new ArrayList<Component>();

		allocatedFingers = new ArrayList<Finger>();
		for (int i = 0; i < 10; ++i){
			allocatedFingers.add(new Finger(i));
		}

		Gdx.input.setInputProcessor(new GdxProcessor(keyboard, mouse, allocatedFingers, gamepad));

		modelBatch = new ModelBatch();

		ShaderProgram.pedantic = false;
		
		spriteBatch = new SpriteBatch();

		frameBuffer = new RenderBuffer(spriteBatch);

	}


	public static void main(){
		profiler.start("__graphics");
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		profiler.stop("__graphics");

		// -------- Update Input --------
		time += TICK_TIME;
		++GdxProcessor.currentTick;
		fingers.clear();
		for (Finger f : allocatedFingers){
			if (f.down() || f.up())
				fingers.add(f);
		}
		// ------------------------------
		
		for (Component c : components){
			if (c.state != null)
				c.state.main();
		}
		
		profiler.stop("__input");

		for (Scene scene : (ArrayListScenes)scenes.clone()){
			scene.update();

			profiler.start("__render");

			// ------- Render Scene --------

			if (scene.filters.size() > 0){
				frameBuffer.begin();
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			}

			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
			modelBatch.begin(scene.cam);
			for (GameObject g : scene.objects){
				if (g.visible()){
					modelBatch.render(g.modelInstance, scene.environment);
				}
			}
			modelBatch.end();

			if (scene.filters.size() > 0){
				
				frameBuffer.end();
				
				scene.lastFrameBuffer.getColorBufferTexture().bind(1);
				Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
								
				for (Filter filter : scene.filters) {

					filter.begin();
					filter.setUniformf("time", Bdx.time);
					filter.setUniformi("lastFrame", 1);
					filter.end();
							
					frameBuffer.drawTo(frameBuffer, filter);

				}
				frameBuffer.drawTo(null); //  Draw to screen
				scene.lastFrameBuffer.clear();
				frameBuffer.drawTo(scene.lastFrameBuffer);		
				
			}

			// ------- Render physics debug view --------

			Bullet.DebugDrawer debugDrawer = (Bullet.DebugDrawer)scene.world.getDebugDrawer();
			debugDrawer.drawWorld(scene.world, scene.cam);
			
			profiler.stop("__render");
		}
		
		if (profiler.visible){
			profiler.update();

			// ------- Render profiler scene --------
			
			Scene profilerScene = profiler.scene;
			profilerScene.update();
			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
			modelBatch.begin(profilerScene.cam);
			for (GameObject g : profilerScene.objects){
				if (g.visible()){
					modelBatch.render(g.modelInstance, profilerScene.environment);
				}
			}
			modelBatch.end();
		}
	}
	
	public static void dispose(){
		modelBatch.dispose();
		spriteBatch.dispose();
		frameBuffer.dispose();
	}
	
	public static void end(){
		System.exit(0);
	}
}
