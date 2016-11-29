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

import javax.vecmath.Vector2f;

public class Bdx{

	public static class Display{
		public boolean changed;
		
		public int width(){
			return Gdx.graphics.getWidth();
		}
		public void width(int width){
			Gdx.graphics.setWindowedMode(width, Gdx.graphics.getHeight());
		}
		public int height(){
			return Gdx.graphics.getHeight();
		}
		public void height(int height){
			Gdx.graphics.setWindowedMode(Gdx.graphics.getWidth(), height);
		}
		public Vector2f size(){
			return new Vector2f(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		}
		public void size(int width, int height){
			Gdx.graphics.setWindowedMode(width, height);
		}
		public void size(Vector2f size){
			size(Math.round(size.x), Math.round(size.y));
		}
		public Vector2f center(){
			return new Vector2f(Gdx.graphics.getWidth() / 2, Gdx.graphics.getHeight() / 2);
		}
		public void fullscreen(boolean full){
			if (full){
				Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
			}else{
				size(size());
			}
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
		@Override
		public Object clone(){
			// GWT (2.6.0) doesn't provide a default implementation of Object.clone()
			ArrayListScenes cloned = new ArrayListScenes();
			for (Scene scene : this){
				cloned.add(scene);
			}
			return cloned;
		}
		@Override
		public boolean add(Scene scene){
			boolean ret = super.add(scene);
			if (scene.objects == null)
				scene.init();
			return ret;
		}
		@Override
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
			for (FileHandle file : files)
				scenes.add(file.nameWithoutExtension());
			return scenes;
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
	public static BDXShaderProvider shaderProvider;
	public static float physicsSpeed;
	public static float timeSpeed;

	private static boolean advancedLightingOn;
	private static ArrayList<Finger> allocatedFingers;
	private static ModelBatch modelBatch;
	private static ModelBatch depthBatch;
	private static RenderBuffer frameBuffer;
	private static RenderBuffer depthBuffer;
	private static SpriteBatch spriteBatch;
	private static Color clearColor;
	private static BDXDepthShaderProvider depthShaderProvider;
	private static HashMap<Float, RenderBuffer> availableTempBuffers;
	private static boolean requestedRestart;

	public static void init(){
		time = 0;
		physicsSpeed = 1;
		timeSpeed = 1;
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

		allocatedFingers = new ArrayList<Finger>();
		for (int i = 0; i < 10; ++i){
			allocatedFingers.add(new Finger(i));
		}

		gamepads = new ArrayListNamed<Gamepad>();
		for (int i = 0; i < Controllers.getControllers().size; i++)
			gamepads.add(new Gamepad(i));

		com.badlogic.gdx.graphics.glutils.ShaderProgram.pedantic = false;

		shaderProvider = new BDXShaderProvider();
		modelBatch = new ModelBatch(shaderProvider);
		spriteBatch = new SpriteBatch();
		spriteBatch.setBlendFunction(Gdx.gl.GL_SRC_ALPHA, Gdx.gl.GL_ONE_MINUS_SRC_ALPHA);
		frameBuffer = new RenderBuffer(spriteBatch);
		depthBuffer = new RenderBuffer(spriteBatch);
		depthShaderProvider = new BDXDepthShaderProvider(Gdx.files.internal("bdx/shaders/3d/depthExtract.vert"), Gdx.files.internal("bdx/shaders/3d/depthExtract.frag"));

		depthBatch = new ModelBatch(depthShaderProvider);
		clearColor = new Color();

		advancedLightingOn = true;

		Gdx.input.setInputProcessor(new GdxProcessor(keyboard, mouse, allocatedFingers, gamepads));

		availableTempBuffers = new HashMap<Float, RenderBuffer>();
		requestedRestart = false;
	}

	public static void main(){

		profiler.start("__render");
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		profiler.stop("__render");

		// -------- Update Input --------

		time += TICK_TIME * timeSpeed;
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

		Viewport vp;

		for (Scene scene : (ArrayListScenes)scenes.clone()){

			scene.update();
			profiler.stop("__scene");

			if (!scene.valid())
				continue;

			// ------- Render Scene --------

			vp = scene.viewport;
			vp.apply();

			depthShaderProvider.update(scene);
			shaderProvider.update(scene);
			
			if (scene.screenShaders.size() > 0){
				frameBuffer.begin();
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			}

			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);

			renderWorld(modelBatch, scene, scene.camera);			// Render main view

			ArrayList<Camera> renderCams = new ArrayList<Camera>();

			for (Camera cam : scene.cameras){
				if (cam.renderingToTexture) {
					cam.update();
					renderCams.add(cam);
				}
			}

			for (Light light : scene.lights) {
				for (Camera cam : light.shadowCams) {
					if (light.shadowOn())
						renderCams.add(cam);		// Lights handle updating their cameras
				}
			}

			for (Camera cam : renderCams) {
				if (cam.renderBuffer == null)
					cam.initRenderBuffer();
				cam.renderBuffer.begin();
				Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
				Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
				renderWorld(modelBatch, scene, cam);
				cam.renderBuffer.end();
			}

			if (scene.screenShaders.size() > 0){

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
					renderWorld(depthBatch, scene, scene.camera);
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
					filter.setUniformf("screenWidth", vp.w);
					filter.setUniformf("screenHeight", vp.h);
					filter.setUniformf("near", scene.camera.near());
					filter.setUniformf("far", scene.camera.far());
					filter.end();

					if (!availableTempBuffers.containsKey(filter.renderScale.x))
						availableTempBuffers.put(filter.renderScale.x, new RenderBuffer(spriteBatch, Math.round(vp.size().x * filter.renderScale.x), Math.round(vp.size().y * filter.renderScale.y)));

					RenderBuffer tempBuffer = availableTempBuffers.get(filter.renderScale.x);

					tempBuffer.clear();

					frameBuffer.drawTo(tempBuffer, filter, 0, 0, frameBuffer.getWidth(), frameBuffer.getHeight());

					if (!filter.overlay)
						frameBuffer.clear();

					tempBuffer.drawTo(frameBuffer);

				}

				frameBuffer.drawTo(null, null, vp.x, vp.y, vp.w, vp.h); //  Draw to screen
				scene.lastFrameBuffer.clear();
				frameBuffer.drawTo(scene.lastFrameBuffer);
			}

			scene.executeDrawCommands();

			display.clearColor(display.clearColor());

			// ------- Render physics debug view --------

			Bullet.DebugDrawer debugDrawer = (Bullet.DebugDrawer)scene.world.getDebugDrawer();
			debugDrawer.drawWorld(scene.world, scene.camera.data);

			profiler.stop("__render");
		}
		
		mouse.wheelMove = 0;
		Bdx.display.changed = false;
		
		profiler.stop("__scene");
		
		profiler.updateVariables();
		if (profiler.visible()){
			profiler.updateVisible();

			// ------- Render profiler scene --------

			profiler.scene.update();
			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
			renderWorld(modelBatch, profiler.scene, profiler.scene.camera);
			profiler.scene.executeDrawCommands();
		}
		if (profiler.gl.isEnabled()){
			profiler.gl.updateFields();
		}

		if (requestedRestart) {
			requestedRestart = false;
			Scene.clearColorDefaultSet = false;
			dispose();
			for (Scene scene : new ArrayList<Scene>(scenes)) {
				scenes.remove(scene);
				scene.end();
			}
			if (profiler.visible()){
				profiler.scene.end();
			}
			init();
			scenes.add(firstScene);
		}
	}

	private static void renderWorld(ModelBatch batch, Scene scene, Camera camera){
		batch.begin(camera.data);
		for (GameObject g : scene.objects){
			if (g.visible() && g.insideFrustum() && !camera.ignoreObjects.contains(g))
				batch.render(g.modelInstance, scene.environment);
		}
		batch.end();
	}

	public static void dispose(){
		modelBatch.dispose();
		depthBatch.dispose();
		spriteBatch.dispose();
		frameBuffer.dispose();
		depthBuffer.dispose();
		shaderProvider.dispose();
		Bdx.sounds.dispose();
		Bdx.music.dispose();

		for (RenderBuffer b : availableTempBuffers.values())
			b.dispose();
		for (Scene s : scenes) {
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
		if (depthBuffer != null)
			depthBuffer.dispose();

		frameBuffer = new RenderBuffer(spriteBatch);		// Have to recreate all render buffers and adjust the projection matrix as the window size has changed
		depthBuffer = new RenderBuffer(spriteBatch);

		for (RenderBuffer b : availableTempBuffers.values())
			b.dispose();

		availableTempBuffers.clear();

		for (Scene scene : scenes) {

			for (Camera cam : scene.cameras) {                // Have to do this, as the RenderBuffers need to be resized for the new window size
				if (cam.renderBuffer != null)
					cam.renderBuffer.dispose();
				cam.renderBuffer = null;
			}

			if (scene.lastFrameBuffer != null)
				scene.lastFrameBuffer.dispose();
			scene.lastFrameBuffer = new RenderBuffer(null);

			scene.viewport.update(width, height);
		}

		if (profiler.visible()){
			profiler.updateViewport(width, height);
		}

		Bdx.display.changed = true;
	}

	public static void restart(){
		requestedRestart = true;
	}

}
