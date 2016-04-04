package com.nilunder.bdx;

import java.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.nilunder.bdx.gl.*;
import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.audio.*;
import com.nilunder.bdx.utils.*;
import com.nilunder.bdx.utils.Color;
import com.nilunder.bdx.utils.Timer;

import javax.vecmath.Vector2f;

public class Bdx{

	public static class Display{

		public void size(int width, int height){
			Gdx.graphics.setDisplayMode(width, height, fullscreen());
			refreshGamepadsTimer.restart();
			refreshGamepadsTimer.resume();
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
			refreshGamepadsTimer.restart();
			refreshGamepadsTimer.resume();
		}
		public boolean fullscreen(){
			return Gdx.graphics.isFullscreen();
		}
		public void clearColor(Color color){
			clearColor.set(color);
			Gdx.gl.glClearColor(color.r, color.g, color.b, color.a);
		}
		public Color clearColor(){
			return clearColor;
		}
		public static void advancedLighting(boolean on){
			advancedLightingOn = on;
			shaderProvider.deleteShaders();
		}
		public static boolean advancedLighting(){
			return advancedLightingOn;
		}
		public static void setMaxLightCount(Light.Type lightType, int count){
			DefaultShader.Config config = shaderProvider.config;
			if (lightType.equals(Light.Type.POINT))
				config.numPointLights = count;
			else if (lightType.equals(Light.Type.SUN))
				config.numDirectionalLights = count;
			else
				config.numSpotLights = count;
			shaderProvider.deleteShaders();			// Get rid of the old shaders, as they need to be recreated for the new light count.
		}
		public static int getMaxLightCount(Light.Type lightType){
			DefaultShader.Config config = shaderProvider.config;
			if (lightType.equals(Light.Type.POINT))
				return config.numPointLights;
			else if (lightType.equals(Light.Type.SUN))
				return config.numDirectionalLights;
			else
				return config.numSpotLights;

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

		@Override
		public Object clone() {
			// GWT (2.6.0) doesn't provide a default implementation of Object.clone()
			ArrayListScenes cloned = new ArrayListScenes();
			for(Scene scene : this) {
				cloned.add(scene);
			}
			return cloned;
		}
	}

	public static final int TICK_RATE = 60;
	public static final float TICK_TIME = 1f/TICK_RATE;
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
	public static HashMap<String, MaterialShader> matShaders;
	public static BDXShaderProvider shaderProvider;

	private static boolean advancedLightingOn;
	private static ArrayList<Finger> allocatedFingers;
	private static ModelBatch modelBatch;
	private static ModelBatch depthBatch;
	private static RenderBuffer frameBuffer;
	private static RenderBuffer tempBuffer;
	private static RenderBuffer depthBuffer;
	private static SpriteBatch spriteBatch;
	private static Timer refreshGamepadsTimer;
	private static Color clearColor;
	private static BDXDepthShaderProvider depthShaderProvider;
	
	public static void init(){
		time = 0;
		profiler = new Profiler();
		display = new Display();
		scenes = new ArrayListScenes();
		sounds = new Sounds();
		music = new Music();
		mouse = new Mouse();

		imaps = new InputMaps();
		keyboard = new Keyboard();
		fingers = new ArrayList<Finger>(); 
		components = new ArrayList<Component>();
		matShaders = new HashMap<String, MaterialShader>();

		allocatedFingers = new ArrayList<Finger>();
		for (int i = 0; i < 10; ++i){
			allocatedFingers.add(new Finger(i));
		}

		refreshGamepadsTimer = new Timer();
		refreshGamepadsTimer.pause();

		refreshGamepads();

		com.badlogic.gdx.graphics.glutils.ShaderProgram.pedantic = false;

		shaderProvider = new BDXShaderProvider();
		modelBatch = new ModelBatch(shaderProvider);
		spriteBatch = new SpriteBatch();
		spriteBatch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
		frameBuffer = new RenderBuffer(spriteBatch);
		tempBuffer = new RenderBuffer(spriteBatch);
		depthBuffer = new RenderBuffer(spriteBatch);
		depthShaderProvider = new BDXDepthShaderProvider(Gdx.files.internal("bdx/shaders/3d/depthExtract.vert"), Gdx.files.internal("bdx/shaders/3d/depthExtract.frag"));

		depthBatch = new ModelBatch(depthShaderProvider);
		clearColor = new Color();

		advancedLightingOn = true;

		Gdx.input.setInputProcessor(new GdxProcessor(keyboard, mouse, allocatedFingers, gamepads));

	}

	public static void main(){

		profiler.start("__render");
		Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		profiler.stop("__render");

		// -------- Update Input --------
		if (refreshGamepadsTimer.done()) {	// Recreate gamepad objects as necessary
			refreshGamepadsTimer.restart();
			refreshGamepadsTimer.pause();
			refreshGamepads();
		}

		time += TICK_TIME;
		++GdxProcessor.currentTick;
		fingers.clear();
		for (Finger f : allocatedFingers){
			if (f.down() || f.up())
				fingers.add(f);
		}
		profiler.stop("__scene");
		// ------------------------------

		for (Component c : components){
			if (c.state != null)
				c.state.main();
		}
		profiler.stop("__logic");

		for (Scene scene : (ArrayListScenes)scenes.clone()) {

			for (Viewport viewport : scene.viewports) {

				viewport.viewportData.apply();

				depthShaderProvider.updateScene(scene);

				scene.update();

				profiler.start("__render");

				// ------- Render Scene --------

				if (scene.screenShaders.size() > 0) {
					frameBuffer.begin();
					Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				}

				Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
				modelBatch.begin(scene.camera.data);
				for (GameObject g : scene.objects) {
					if (g.visible() && g.insideFrustum()) {
						modelBatch.render(g.modelInstance, scene.environment);
					}
				}
				modelBatch.end();

				scene.executeDrawCommands();

				if (scene.screenShaders.size() > 0) {

					frameBuffer.end();

					boolean usingDepth = false;

					for (ScreenShader filter : scene.screenShaders) {
						if (filter.usingDepthTexture())
							usingDepth = true;
					}

					if (usingDepth) {
						Gdx.gl.glClearColor(1, 1, 1, 1);
						depthBuffer.begin();
						Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT);
						depthBatch.begin(scene.camera.data);
						for (GameObject g : scene.objects) {
							if (g.visible() && g.insideFrustum()) {
								depthBatch.render(g.modelInstance);
							}
						}
						depthBatch.end();
						depthBuffer.end();
						depthBuffer.getColorBufferTexture().bind(2);
					}

					scene.lastFrameBuffer.getColorBufferTexture().bind(1);
					Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

					Gdx.gl.glClearColor(0, 0, 0, 0);

					for (ScreenShader filter : scene.screenShaders) {

						if (!filter.active)
							continue;

						filter.begin();
						filter.setUniformf("time", Bdx.time);
						filter.setUniformi("lastFrame", 1);
						filter.setUniformi("depthTexture", 2);
						filter.setUniformf("screenWidth", Bdx.display.size().x);
						filter.setUniformf("screenHeight", Bdx.display.size().y);
						filter.setUniformf("near", scene.camera.near());
						filter.setUniformf("far", scene.camera.far());
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

				display.clearColor(display.clearColor());

				// ------- Render physics debug view --------

				Bullet.DebugDrawer debugDrawer = (Bullet.DebugDrawer) scene.world.getDebugDrawer();
				debugDrawer.drawWorld(scene.world, scene.camera.data);

				profiler.stop("__render");
			}
		}

		mouse.wheelMove = 0;
		
		profiler.updateVariables();
		if (profiler.visible()){
			profiler.updateVisible();
			
			// ------- Render profiler scene --------
			
			Scene profilerScene = profiler.scene;
			profilerScene.update();
			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
			modelBatch.begin(profilerScene.camera.data);
			for (GameObject g : profilerScene.objects){
				if (g.visible()){
					modelBatch.render(g.modelInstance, profilerScene.environment);
				}
			}
			modelBatch.end();
			profilerScene.executeDrawCommands();
		}
		if (profiler.gl.isEnabled()){
			profiler.gl.updateFields();
		}
	}
	
	public static void dispose(){
		modelBatch.dispose();
		depthBatch.dispose();
		spriteBatch.dispose();
		frameBuffer.dispose();
		tempBuffer.dispose();
		depthBuffer.dispose();
		shaderProvider.dispose();
		for (MaterialShader s : Bdx.matShaders.values()) {
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
		if (depthBuffer != null)
			depthBuffer.dispose();
		
		frameBuffer = new RenderBuffer(spriteBatch);		// Have to recreate all render buffers and adjust the projection matrix as the window size has changed
		tempBuffer = new RenderBuffer(spriteBatch);
		depthBuffer = new RenderBuffer(spriteBatch);

		for (Scene scene : scenes) {

			if (scene.lastFrameBuffer != null)
				scene.lastFrameBuffer.dispose();

			scene.lastFrameBuffer = new RenderBuffer(null);

			for (Viewport viewport : scene.viewports){
				viewport.viewportData.update(width, height);
				viewport.position(viewport.position());
				viewport.size(viewport.size());
			}
		}
	}

	public static void restart(){
		Scene.clearColorDefaultSet = false;
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

	public static void refreshGamepads(){

		ArrayListNamed<Gamepad> oldPads = new ArrayListNamed<Gamepad>();
		if (gamepads != null && gamepads.size() > 0)
			oldPads.addAll(gamepads);

		gamepads = new ArrayListNamed<Gamepad>();
		for (int i = 0; i < Controllers.getControllers().size; i++) {

			Gamepad gp = new Gamepad(i);
			gamepads.add(gp);

			if (gp.controller != null)
				gp.controller.addListener(new GdxProcessor.GamepadAdapter(gp));

			if (oldPads.size() > i) {
				gp.profiles.putAll(oldPads.get(i).profiles);
				if (oldPads.get(i).profile != null)
					gp.profile(oldPads.get(i).profile.name);

			}
		}

	}

}
