package com.nilunder.bdx.gl;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.nilunder.bdx.Scene;

class BDXDepthShader extends DepthShader {
	public Scene scene;

	public BDXDepthShader(Renderable renderable) {
		super(renderable, new DepthShader.Config());
	}

	public BDXDepthShader(Renderable renderable, Config config) {
		super(renderable, config);
	}

	public void render(Renderable renderable, Attributes combinedAttributes) {
		super.render(renderable, combinedAttributes);
		if (scene != null) {
			program.setUniformf("far", scene.camera.far());
			program.setUniformf("near", scene.camera.near());
		}
	}
}

public class BDXDepthShaderProvider extends DepthShaderProvider {

	public BDXDepthShaderProvider(FileHandle vertexShader, FileHandle fragmentShader) {
		super(vertexShader.readString(), fragmentShader.readString());
	}

	protected Shader createShader(Renderable renderable) {
		return new BDXDepthShader(renderable, this.config);
	}
	public void updateScene(Scene scene){
		for (Shader s : shaders) {
			((BDXDepthShader) s).scene = scene;
		}
	}

}
