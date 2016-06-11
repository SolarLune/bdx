package com.nilunder.bdx.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
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
import com.nilunder.bdx.gl.Viewport;

class BDXDefaultShader extends DefaultShader {

	public final int u_shadeless = register("u_shadeless");
	public final int u_tintColor = register("u_tintColor");
	public final int u_emitColor = register("u_emitColor");
	public final int u_fogRange = register("u_fogRange");
	public final int u_camRange = register("u_camRange");

	BDXShaderProvider shaderProvider;

	public String materialName = null;

	public BDXDefaultShader(Renderable renderable, Config config) {
		super(renderable, config);
	}

	public BDXDefaultShader(Renderable renderable, DefaultShader.Config config, MaterialShader shaderProgram) {
		super(renderable,
				config,
				shaderProgram.set(createPrefix(renderable, config) + shaderProgram.vertexShader,
						createPrefix(renderable, config) + shaderProgram.fragmentShader).compile().programData);
	}

	public void render(Renderable renderable, Attributes combinedAttributes)
	{
		if(renderable.material.has(BlendingAttribute.Type)) {

			BlendingAttribute ba = (BlendingAttribute) renderable.material.get(BlendingAttribute.Type);

			Gdx.gl.glBlendFuncSeparate(ba.sourceFunction, ba.destFunction, GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);

		}

		IntAttribute shadeless = (IntAttribute) renderable.material.get(Scene.BDXIntAttribute.Shadeless);

		if (shadeless == null)
			set(u_shadeless, 0);
		else
			set(u_shadeless, shadeless.value);

		ColorAttribute tint = (ColorAttribute) renderable.material.get(Scene.BDXColorAttribute.Tint);

		if (tint == null)
			set(u_tintColor, new Color());
		else
			set(u_tintColor, tint.color);

		ColorAttribute emit = (ColorAttribute) renderable.material.get(Scene.BDXColorAttribute.Emit);

		if (emit == null)
			set(u_emitColor, new Color());
		else
			set(u_emitColor, emit.color);

		if (shaderProvider.viewport != null) {

			ColorAttribute fog = (ColorAttribute) renderable.environment.get(ColorAttribute.Fog);
			if (fog == null)
				set(u_fogRange, 0f, 0f);
			else
				set(u_fogRange, shaderProvider.viewport.scene().fogRange().x, shaderProvider.viewport.scene().fogRange().y);

			set(u_camRange, shaderProvider.viewport.camera().near(), shaderProvider.viewport.camera().far());

		}

		super.render(renderable, combinedAttributes);
	}

	public boolean canRender(Renderable renderable) {

		String matName = renderable.material.id;

		boolean hasCustomShader = Bdx.matShaders.containsKey(matName);	// Is there a custom shader for the rendered material?

		if (hasCustomShader) {

			if (Bdx.matShaders.get(matName).programData == program)					// Is this shader for that rendered material?
				return true;											// If so, it can be used to render
			else
				return false;

		}
		else {															// This material doesn't have a custom shader.
			if (materialName != null)									// So never use a custom shader; always use an un-customized one.
				return false;
			else
				return super.canRender(renderable);
		}

	}

}

public class BDXShaderProvider extends DefaultShaderProvider {

	Viewport viewport;

	public BDXShaderProvider(){

		super(new DefaultShader.Config(Gdx.files.internal("bdx/shaders/3d/default.vert").readString(),
				Gdx.files.internal("bdx/shaders/3d/default.frag").readString()));

		config.numPointLights = 8;
		config.numSpotLights = 8;
		config.numDirectionalLights = 2;
	}

	public Shader createShader(Renderable renderable) {

		BDXDefaultShader shader;

		if (Bdx.matShaders.containsKey(renderable.material.id)) {
			MaterialShader sp = Bdx.matShaders.get(renderable.material.id);
			shader = new BDXDefaultShader(renderable, config, sp);
			shader.materialName = renderable.material.id;
		}
		else {
			if (Bdx.Display.advancedLighting())
				shader = new BDXDefaultShader(renderable, config);
			else {
				DefaultShader.Config lowConfig = new DefaultShader.Config(Gdx.files.internal("bdx/shaders/3d/vertexLighting.vert").readString(),
						Gdx.files.internal("bdx/shaders/3d/vertexLighting.frag").readString());
				lowConfig.numPointLights = config.numPointLights;
				lowConfig.numSpotLights = config.numSpotLights;
				lowConfig.numDirectionalLights = config.numDirectionalLights;
				shader = new BDXDefaultShader(renderable, lowConfig);
			}
		}

		shader.shaderProvider = this;

		return shader;
	}

	public void update(Viewport viewport){
		this.viewport = viewport;
	}

	public void deleteShaders(){
		for (Shader s : shaders){
			BDXDefaultShader shader = (BDXDefaultShader) s;
			if (shader.materialName != null)
				Bdx.matShaders.remove(shader.materialName);		// Remove the ShaderProgram from the custom Shaders HashMap because
			s.dispose();										// Shader.dispose() destroys the ShaderProgram, too.
		}
		shaders.clear();
	}

}
