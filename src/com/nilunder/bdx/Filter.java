package com.nilunder.bdx;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

/**
 * Created by SolarLune on 3/18/2015.
 */
public class Filter extends ShaderProgram {
	public Filter(String vertexShader, String fragmentShader) {
		super(vertexShader, fragmentShader);

		if (!isCompiled()) {
			throw new RuntimeException("Shader compilation error: " + getLog());
		}
	}
	public Filter(FileHandle vertexShader, FileHandle fragmentShader) {
		this(vertexShader.readString(), fragmentShader.readString());
	}
}
