package com.nilunder.bdx;

import java.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.files.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.nilunder.bdx.audio.Audio;
import com.nilunder.bdx.gl.*;
import com.nilunder.bdx.inputs.*;
import com.nilunder.bdx.utils.*;
import com.nilunder.bdx.utils.Color;

import javax.vecmath.Vector2f;

public class Bdx{

	public static class Display{
		public boolean changed;
		public Color clearColor = new Color(0, 0, 0, 0);
		private boolean vsync;

		public void vsync(boolean on) {
			Gdx.graphics.setVSync(on);
			vsync = on;
		}
		public boolean vsync(){
			return vsync;
		}
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
	public static Audio audio;
	public static Mouse mouse;
	public static ArrayListNamed<Gamepad> gamepads;
	public static InputMaps imaps;
	public static Keyboard keyboard;
	public static ArrayList<Finger> fingers;
	public static ArrayList<Component> components;
	public static BDXShaderProvider shaderProvider;
	public static float physicsSpeed;
	public static float timeSpeed;
	public static boolean restartOnExport = false;

	private static boolean advancedLightingOn;
	private static ArrayList<Finger> allocatedFingers;
	private static ModelBatch modelBatch;
	private static ModelBatch depthBatch;
	private static RenderBuffer frameBuffer;
	private static RenderBuffer depthBuffer;
	private static SpriteBatch spriteBatch;
	private static BDXDepthShaderProvider depthShaderProvider;
	private static HashMap<Float, RenderBuffer> availableTempBuffers;
	private static boolean requestedRestart;

	private static long startMillis = System.currentTimeMillis();
	private static UniformSet defaultScreenShaderUniformSet;

	public static void init(){
		time = 0;
		physicsSpeed = 1;
		timeSpeed = 1;
		profiler = new Profiler();
		display = new Display();
		scenes = new ArrayListScenes();
		audio = new Audio();
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

		advancedLightingOn = true;

		Gdx.input.setInputProcessor(new GdxProcessor(keyboard, mouse, allocatedFingers, gamepads));

		availableTempBuffers = new HashMap<Float, RenderBuffer>();
		requestedRestart = false;

		profiler.start("__gpu wait");

	}

	public static void main(){

		// --------- Auto reloading -------- //

		profiler.stop("__gpu wait");

		if (profiler.subsystemsVisible())
			profiler.deltaTimes.put("__gpu wait", (long) Math.max(profiler.deltaTimes.get("__gpu wait") - (TICK_TIME * 1000000000), 0));

		if (restartOnExport && Gdx.files.internal("finishedExport").lastModified() > startMillis) {
			startMillis = System.currentTimeMillis();
			restart();
		}

		boolean screenShadersUsed = false;

		for (Scene scene : scenes) {
			if (scene.screenShaders.size() > 0) {
				screenShadersUsed = true;
				break;
			}
		}

		profiler.start("__render");
		Gdx.gl.glClearColor(display.clearColor.r, display.clearColor.g, display.clearColor.b, display.clearColor.a);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glClearColor(0, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);
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

		ArrayListScenes newSceneList = (ArrayListScenes) scenes.clone();

		boolean depthBufferCleared = false;
		boolean colorBufferCleared = false;

		ArrayList<Scene> depthRenderScenes = new ArrayList<Scene>();

		for (int s = 0; s < scenes.size(); s++) {
			Scene scene = scenes.get(s);
			for (ScreenShader filter : scene.screenShaders) {
				if (filter.usingDepthTexture()) {
					depthRenderScenes.add(scene);
					for (int ns = s; ns >= 0; ns--) {
						if (scenes.get(ns).renderPassthrough)
							depthRenderScenes.add(scenes.get(ns));
						else
							break;
					}
					break;
				}
			}
		}

		for (int i = 0; i < newSceneList.size(); i++){

			final Scene scene = newSceneList.get(i);
			boolean prevSceneRenderPassthrough = false;
			boolean nextSceneRenderPassthrough = false;

			if (i > 0)
				prevSceneRenderPassthrough = newSceneList.get(i - 1).renderPassthrough;

			if (i < newSceneList.size() - 1)
				nextSceneRenderPassthrough = newSceneList.get(i + 1).renderPassthrough;

			if (!prevSceneRenderPassthrough) {
				colorBufferCleared = false;
				depthBufferCleared = false;
			}

			scene.update();
			profiler.stop("__scene");

			if (!scene.valid() || !scene.visible)
				continue;

			// ------- Render Scene --------

			vp = scene.viewport;
			vp.apply();

			final float vpw = vp.w;
			final float vph = vp.h;

			if (defaultScreenShaderUniformSet == null) {

				defaultScreenShaderUniformSet = new UniformSet() {
					@Override
					public void set(ShaderProgram program) {
						program.setUniformf("time", Bdx.time);
						program.setUniformi("lastFrame", 1);
						program.setUniformi("depthTexture", 2);
						program.setUniformf("screenWidth", vpw);
						program.setUniformf("screenHeight", vph);
						program.setUniformf("near", scene.camera.near());
						program.setUniformf("far", scene.camera.far());
					}
				};

			}

			depthShaderProvider.update(scene);
			shaderProvider.update(scene);

			for (Camera cam : scene.cameras){						// Render auxiliary cameras
				if (cam.renderToTexture){
					cam.update();
					if (cam.renderBuffer == null){
						cam.initRenderBuffer();
					}
					cam.renderBuffer.begin();
					Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT | GL20.GL_COLOR_BUFFER_BIT);
					renderWorld(modelBatch, scene, cam);
					cam.renderBuffer.end();
				}
			}

			boolean frameBufferInUse = false;

			if (scene.screenShaders.size() > 0 || (screenShadersUsed && scene.renderPassthrough)) { // If the scene is passing its render output, and screen shaders are used, then it needs to use the framebuffer to pass the render on.
																									// If screen shaders aren't used anywhere, there's no need to render to a framebuffer, as OpenGL will correctly blend normally.
				frameBuffer.begin();
				frameBufferInUse = true;
				if (!colorBufferCleared) {            				// First rendering scene, or previous scene didn't pass a render, so it needs to clear the framebuffer.
					Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);		// We only need to do this once - the rendered data can accumulate until the last scene renders or
					colorBufferCleared = true;						// Another scene stops passing the render data up the stack
				}
			}

			Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT);				// We have to clear the depth buffer no matter what, because closer things on previous scenes should never
																	// overlap further things on overlay scenes

			renderWorld(modelBatch, scene, scene.camera);			// Render main view

			if (frameBufferInUse) {

				frameBuffer.end();

				if (depthRenderScenes.contains(scene)) {			// Render depth texture
					Gdx.gl.glClearColor(1, 1, 1, 1);
					depthBuffer.begin();
					if (!depthBufferCleared) {
						Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
						depthBufferCleared = true;
					}
					Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT );
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

					if (!filter.uniformSets.contains(defaultScreenShaderUniformSet))
						filter.uniformSets.add(defaultScreenShaderUniformSet);

					if (!availableTempBuffers.containsKey(filter.renderScale.x))
						availableTempBuffers.put(filter.renderScale.x, new RenderBuffer(spriteBatch, Math.round(vp.size().x * filter.renderScale.x), Math.round(vp.size().y * filter.renderScale.y)));

					RenderBuffer tempBuffer = availableTempBuffers.get(filter.renderScale.x);

					tempBuffer.clear();

					frameBuffer.drawTo(tempBuffer, filter, 0, 0, frameBuffer.getWidth(), frameBuffer.getHeight());

					if (!filter.overlay)
						frameBuffer.clear();

					tempBuffer.drawTo(frameBuffer);

				}

				if (!scene.renderPassthrough || scene == newSceneList.get(newSceneList.size() - 1) || !nextSceneRenderPassthrough)
					frameBuffer.drawTo(null, null, vp.x, vp.y, vp.w, vp.h); //  Draw to screen, but only if the scene's not passing the render, or if it's the last scene in the list
				scene.lastFrameBuffer.clear();
				frameBuffer.drawTo(scene.lastFrameBuffer);
			}

			scene.executeDrawCommands();

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

		profiler.start("__gpu wait");

	}

	private static void renderWorld(ModelBatch batch, Scene scene, Camera camera){
		batch.begin(camera.data);
		for (GameObject g : scene.objects){
			if (g.visible() && (g.insideFrustum() || !g.frustumCulling) && !camera.ignoreObjects.contains(g))
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
		audio.dispose();

		for (RenderBuffer b : availableTempBuffers.values())
			b.dispose();
		for (Scene s : scenes) {
			s.dispose();
		}

	}

	public static void end(){
		if (profiler.visible())
			profiler.gl.disable();

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
