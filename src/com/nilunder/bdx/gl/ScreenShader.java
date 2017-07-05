package com.nilunder.bdx.gl;

import javax.vecmath.Vector2f;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class ScreenShader extends Shader {

	public Vector2f renderScale;
	public boolean overlay;
	static public boolean nearestFiltering = false;

	private boolean usingDepthTexture = false;
	private boolean checkedShaderProgram = false;

	public ScreenShader(String vertexShader, String fragmentShader) {
		super(vertexShader, fragmentShader);
	}

	public ScreenShader(FileHandle vertexShader, FileHandle fragmentShader) {
		super(vertexShader, fragmentShader);
	}

	public void init() {
		super.init();
		renderScale = new Vector2f(1, 1);
		overlay = false;
	}

	public static ScreenShader load(String vertexPath, String fragmentPath) {
		return new ScreenShader(Gdx.files.internal("bdx/shaders/2d/" + vertexPath), Gdx.files.internal("bdx/shaders/2d/" + fragmentPath));
	}

	public static ScreenShader load(String fragmentPath) {
		return load("default.vert", fragmentPath);
	}

	public ShaderProgram compile() {
		ShaderProgram out = super.compile();
		if (out != null)
			checkedShaderProgram = false;
		return out;
	}

	public boolean usingDepthTexture() {
		checkBuffers();
		return usingDepthTexture;
	}

	private void checkBuffers() {
		if (!checkedShaderProgram && compiled()) {
			usingDepthTexture = program.getFragmentShaderSource().contains("depthTexture");
			checkedShaderProgram = true;
		}
	}
	
}
