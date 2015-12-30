package com.nilunder.bdx.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider;
import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;

class BDXDefaultShader extends DefaultShader {

	public final int u_shadeless = register("u_shadeless");
	public final int u_tintColor = register("u_tintColor");
	public final int u_emitColor = register("u_emitColor");

	public boolean customized = false;

	public BDXDefaultShader(Renderable renderable, Config config) {
		super(renderable, config);
	}

	public BDXDefaultShader(Renderable renderable, DefaultShader.Config config, ShaderProgram shaderProgram) {
		super(renderable, config, shaderProgram);
	}

	public void render(Renderable renderable, Attributes combinedAttributes)
	{
		BlendingAttribute ba = (BlendingAttribute) renderable.material.get(BlendingAttribute.Type);

		Gdx.gl.glBlendFuncSeparate(ba.sourceFunction, ba.destFunction, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

		IntAttribute shadeless = (IntAttribute) renderable.material.get(Scene.BDXIntAttribute.Shadeless);

		set(u_shadeless, 0);
		if (shadeless != null)
			set(u_shadeless, shadeless.value);

		ColorAttribute tint = (ColorAttribute) renderable.material.get(Scene.BDXColorAttribute.Tint);

		set(u_tintColor, 0, 0, 0);
		if (tint != null)
			set(u_tintColor, tint.color);

		ColorAttribute emit = (ColorAttribute) renderable.material.get(Scene.BDXColorAttribute.Emit);

		set(u_emitColor, 0, 0, 0);
		if (emit != null)
			set(u_emitColor, emit.color);

		super.render(renderable, combinedAttributes);
	}

	public boolean canRender(Renderable renderable) {

		String matName = renderable.material.id;

		boolean hasCustomShader = Bdx.matShaders.containsKey(matName);	// Is there a custom shader for the rendered material?

		if (hasCustomShader) {

			if (Bdx.matShaders.get(matName) == program)					// Is this shader for that rendered material?
				return true;											// If so, it can be used to render
			else
				return false;

		}
		else {															// This material doesn't have a custom shader.
			if (customized)												// So never use a custom shader; always use an un-customized one.
				return false;
			else
				return super.canRender(renderable);
		}

	}



}

public class BDXShaderProvider extends DefaultShaderProvider {

	public Shader createShader(Renderable renderable) {

		if (Bdx.matShaders.containsKey(renderable.material.id)) {
			ShaderProgram sp = Bdx.matShaders.get(renderable.material.id);
			BDXDefaultShader shader = new BDXDefaultShader(renderable, new DefaultShader.Config(), sp);
			shader.customized = true;
			return shader;
		}

		DefaultShader.Config config = new DefaultShader.Config(Gdx.files.internal("bdx/shaders/3d/default.vert").readString(),
				Gdx.files.internal("bdx/shaders/3d/default.frag").readString());

		config.numPointLights = 128;
		config.numDirectionalLights = 128;
		config.numSpotLights = 128;

		return new BDXDefaultShader(renderable, config);
	}
}
