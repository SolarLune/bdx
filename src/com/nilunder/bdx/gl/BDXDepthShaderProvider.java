package com.nilunder.bdx.gl;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.shaders.DepthShader;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.nilunder.bdx.gl.Viewport;

class BDXDepthShader extends DepthShader {
	public Viewport viewport;

	public BDXDepthShader(Renderable renderable) {
		super(renderable, new DepthShader.Config());
	}

	public BDXDepthShader(Renderable renderable, Config config) {
		super(renderable, config);
	}

	public void render(Renderable renderable, Attributes combinedAttributes) {
		super.render(renderable, combinedAttributes);
		if (viewport != null) {
			program.setUniformf("far", viewport.camera().far());
			program.setUniformf("near", viewport.camera().near());
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

	public void update(Viewport viewport){
		for (Shader s : shaders) {
			((BDXDepthShader) s).viewport = viewport;
		}
	}

}
