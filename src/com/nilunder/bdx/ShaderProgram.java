package com.nilunder.bdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader.Config;

/**
 * Created by SolarLune on 3/18/2015.
 */
public class ShaderProgram extends com.badlogic.gdx.graphics.glutils.ShaderProgram {
	
	private DefaultShader shader;
	
	public ShaderProgram(String vertexShader, String fragmentShader) {
		super(vertexShader, fragmentShader);

		if (!isCompiled()) {
			throw new RuntimeException("Shader compilation error: " + getLog());
		}
	}
	public ShaderProgram(FileHandle vertexShader, FileHandle fragmentShader) {
		this(vertexShader.readString(), fragmentShader.readString());
	}
	
	public static ShaderProgram load(String vertexPath, String fragmentPath) {
		return new ShaderProgram(Gdx.files.internal(vertexPath), Gdx.files.internal(fragmentPath));
	}
	
	public DefaultShader getShader(Renderable renderable){
		
		if (shader == null) {
			shader = new DefaultShader(renderable, new Config(), this);
			shader.init();
		}
		
		return shader;
	}

	public void disposeAll(){
		if (shader != null)
			shader.dispose();
		else
			dispose();
	}
	
}
