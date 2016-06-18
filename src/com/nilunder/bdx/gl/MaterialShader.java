package com.nilunder.bdx.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;

public class MaterialShader implements Disposable{

	public String vertexShader;
	public String fragmentShader;
	private String prefix;

	public com.badlogic.gdx.graphics.glutils.ShaderProgram programData;

	public MaterialShader(String vertexShader, String fragmentShader) {
		this.vertexShader = vertexShader;
		this.fragmentShader = fragmentShader;
	}

	public MaterialShader(FileHandle vertexShader, FileHandle fragmentShader) {
		this(vertexShader.readString(), fragmentShader.readString());
	}

	public static MaterialShader load(String vertexPath, String fragmentPath) {
		return new MaterialShader(Gdx.files.internal(vertexPath), Gdx.files.internal(fragmentPath));
	}

	public MaterialShader compile(){

		dispose();

		programData = new ShaderProgram(prefix + vertexShader, prefix + fragmentShader);

		if (!programData.isCompiled())
			throw new RuntimeException("Shader compilation error: " + programData.getLog());

		return this;
	}

	public boolean compiled(){
		return programData != null;
	}

	public void dispose(){
		if (compiled()) {
			programData.dispose();
			programData = null;
		}
	}

	public MaterialShader setPrefix(String prefix){
		this.prefix = prefix;
		return this;
	}

}
