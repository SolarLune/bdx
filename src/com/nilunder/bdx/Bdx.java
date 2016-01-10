package com.nilunder.bdx;

import java.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.nilunder.bdx.gl.BDXShaderProvider;
import com.nilunder.bdx.gl.RenderBuffer;
import com.nilunder.bdx.gl.ShaderProgram;
import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.audio.*;
import com.nilunder.bdx.utils.*;

import javax.vecmath.Vector2f;

public class Bdx{

	public static class Display{
		public void size(int width, int height){
			Gdx.graphics.setDisplayMode(width, height, fullscreen());
		}
		public void size(Vector2f vec){
			size((int)vec.x, (int)vec.y);
		}
		public Vector2f size(){
			return new Vector2f(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
		public Vector2f center(){
			return new Vector2f(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
		}
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
	public static String firstScene;
	public static Profiler profiler;
	public static ArrayListScenes scenes;
	public static Display display;
	public static Sounds sounds;
	public static Music music;
	public static Mouse mouse;
	public static ArrayListNamed<Gamepad> gamepads;
	public static InputMaps imaps;
	public static Keyboard keyboard;
	public static ArrayList<Finger> fingers;
	public static ArrayList<Component> components;
	public static HashMap<String, ShaderProgram> matShaders;
	public static BDXShaderProvider shaderProvider;

	private static ArrayList<Finger> allocatedFingers;
	private static ModelBatch modelBatch;
	private static RenderBuffer frameBuffer;
	private static RenderBuffer tempBuffer;
	private static SpriteBatch spriteBatch;
	
	public static void init(){
		time = 0;
		profiler = new Profiler();
		display = new Display();
		scenes = new ArrayListScenes();
		sounds = new Sounds();
		music = new Music();
		mouse = new Mouse();
		gamepads = new ArrayListNamed<Gamepad>();

		for (int i = 0; i < Controllers.getControllers().size; i++) {
			gamepads.add(new Gamepad(i));
		}

		imaps = new InputMaps();
		keyboard = new Keyboard();
		fingers = new ArrayList<Finger>(); 
		components = new ArrayList<Component>();
		matShaders = new HashMap<String, ShaderProgram>();

		allocatedFingers = new ArrayList<Finger>();
		for (int i = 0; i < 10; ++i){
			allocatedFingers.add(new Finger(i));
		}

		Gdx.input.setInputProcessor(new GdxProcessor(keyboard, mouse, allocatedFingers, gamepads));

		com.badlogic.gdx.graphics.glutils.ShaderProgram.pedantic = false;

		shaderProvider = new BDXShaderProvider();
		modelBatch = new ModelBatch(shaderProvider);
		spriteBatch = new SpriteBatch();
		spriteBatch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
		frameBuffer = new RenderBuffer(spriteBatch);
		tempBuffer = new RenderBuffer(spriteBatch);
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
				if (g.visible() && g.insideFrustum()){
					modelBatch.render(g.modelInstance, scene.environment);
				}
			}
			modelBatch.end();

			if (scene.filters.size() > 0){
				
				frameBuffer.end();
				
				scene.lastFrameBuffer.getColorBufferTexture().bind(1);
				Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

				for (ShaderProgram filter : scene.filters) {
					
					filter.begin();
					filter.setUniformf("time", Bdx.time);
					filter.setUniformi("lastFrame", 1);
					filter.setUniformf("screenWidth", Bdx.display.size().x);
					filter.setUniformf("screenHeight", Bdx.display.size().y);
					filter.end();
											
					tempBuffer.clear();
					
					int width = (int) (Gdx.graphics.getWidth() * filter.renderScale.x);
					int height = (int) (Gdx.graphics.getHeight() * filter.renderScale.y);
					
					if (tempBuffer.getWidth() != width || tempBuffer.getHeight() != height)
						tempBuffer = new RenderBuffer(spriteBatch, width, height);
					
					frameBuffer.drawTo(tempBuffer, filter);
					
					if (!filter.overlay)
						frameBuffer.clear();	
					
					tempBuffer.drawTo(frameBuffer);
					
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
		mouse.wheelMove = 0;
		
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
		tempBuffer.dispose();
		shaderProvider.dispose();
		for (ShaderProgram s : Bdx.matShaders.values()) {
			s.dispose();
		}
	}
	
	public static void end(){
		Gdx.app.exit();
	}
	
	public static void resize(int width, int height) {
		spriteBatch.getProjectionMatrix().setToOrtho2D(0, 0, width, height);
		
		if (frameBuffer != null)
			frameBuffer.dispose();
		if (tempBuffer != null)
			tempBuffer.dispose();
		
		frameBuffer = new RenderBuffer(spriteBatch);		// Have to recreate all render buffers and adjust the projection matrix as the window size has changed
		tempBuffer = new RenderBuffer(spriteBatch);
		for (Scene scene : scenes) {

			if (scene.lastFrameBuffer != null)
				scene.lastFrameBuffer.dispose();

			scene.lastFrameBuffer = new RenderBuffer(null);
		}
	}

	public static void restart(){
		dispose();
		for (Scene scene : new ArrayList<Scene>(scenes)) {
			scenes.remove(scene);
			scene.end();
		}
		if (profiler.scene != null)
			profiler.scene.end();
		init();
		scenes.add(firstScene);
	}

}
