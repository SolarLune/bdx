package com.nilunder.bdx.gl;

import javax.vecmath.Vector2f;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

public class ShaderProgram extends com.badlogic.gdx.graphics.glutils.ShaderProgram {

	public Vector2f renderScale;
	public boolean overlay;
	static public boolean nearestFiltering = false;
	private boolean usingDepthTexture = false;
	private boolean checkedShaderProgram = false;
	
	public ShaderProgram(String vertexShader, String fragmentShader) {

		super(vertexShader, fragmentShader);

		if (!isCompiled()) {
			throw new RuntimeException("Shader compilation error: " + getLog());
		}
		
		renderScale = new Vector2f(1, 1);
		overlay = false;
	}
	public ShaderProgram(FileHandle vertexShader, FileHandle fragmentShader) {
		this(vertexShader.readString(), fragmentShader.readString());
	}
	
	public static ShaderProgram load(String vertexPath, String fragmentPath) {
		return new ShaderProgram(Gdx.files.internal(vertexPath), Gdx.files.internal(fragmentPath));
	}

	public boolean usingDepthTexture(){
		checkBuffers();
		return usingDepthTexture;
	}

	public void checkBuffers(){
		if (!checkedShaderProgram) {

			if (getFragmentShaderSource().contains("depthTexture"))
				usingDepthTexture = true;

			checkedShaderProgram = true;
		}
	}
	
}
