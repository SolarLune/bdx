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

class BDXDefaultShader extends DefaultShader {

	public final int u_shadeless = register("u_shadeless");
	public final int u_tintColor = register("u_tintColor");
	public final int u_emitColor = register("u_emitColor");
	public final int u_fogRange = register("u_fogRange");
	public final int u_camRange = register("u_camRange");

	BDXShaderProvider shaderProvider;
	Material applyingMaterial = null;

	public BDXDefaultShader(Renderable renderable, Config config) {
		super(renderable, config);
	}

	public BDXDefaultShader(Renderable renderable, DefaultShader.Config config, MaterialShader materialShader) {
		super(renderable,
				config,
				materialShader.setPrefix(createPrefix(renderable, config)).compile().programData);
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

		if (shaderProvider.scene != null) {

			ColorAttribute fog = (ColorAttribute) renderable.environment.get(ColorAttribute.Fog);
			if (fog == null)
				set(u_fogRange, 0f, 0f);
			else
				set(u_fogRange, shaderProvider.scene.fogRange().x, shaderProvider.scene.fogRange().y);

			set(u_camRange, shaderProvider.scene.camera.near(), shaderProvider.scene.camera.far());

		}

		if (applyingMaterial != null && applyingMaterial.shader != null) {
			for (UniformSet uniformSet : applyingMaterial.shader.uniformSets)
				uniformSet.set(program);
		}

		super.render(renderable, combinedAttributes);
	}

	public boolean canRender(Renderable renderable) {

		Material bdxMat = null;
		if (renderable.material instanceof Material)
			bdxMat = (Material) renderable.material;

		if (bdxMat != null) {
			if (bdxMat.shader != null && bdxMat.shader.active)				    // Is there a custom shader specified for the rendered material? If so,
				return bdxMat.shader.programData == program;					// Use this shader only if this is the shader for the material.
			else															    // Otherwise, the rendered material doesn't have a custom shader specified, so
				return applyingMaterial == null && super.canRender(renderable);	// Use this shader only if it's not a custom shader
		}
		else																	// We're rendering something that isn't even a BDX Material, so never mind
			return super.canRender(renderable);

	}

}

public class BDXShaderProvider extends DefaultShaderProvider {

	Scene scene;

	public BDXShaderProvider(){

		super(new DefaultShader.Config(Gdx.files.internal("bdx/shaders/3d/default.vert").readString(),
				Gdx.files.internal("bdx/shaders/3d/default.frag").readString()));

		config.numPointLights = 8;
		config.numSpotLights = 8;
		config.numDirectionalLights = 2;
	}

	public Shader createShader(Renderable renderable) {

		BDXDefaultShader bdxDefaultShader;
		Material bdxMat = null;
		if (renderable.material instanceof Material)
			bdxMat = (Material) renderable.material;

		if (bdxMat != null && bdxMat.shader != null && bdxMat.shader.active) {
			bdxDefaultShader = new BDXDefaultShader(renderable, config, bdxMat.shader);
            bdxDefaultShader.applyingMaterial = bdxMat;
		}
		else {
			if (Bdx.Display.advancedLighting())
				bdxDefaultShader = new BDXDefaultShader(renderable, config);
			else {
				DefaultShader.Config lowConfig = new DefaultShader.Config(Gdx.files.internal("bdx/shaders/3d/vertexLighting.vert").readString(),
						Gdx.files.internal("bdx/shaders/3d/vertexLighting.frag").readString());
				lowConfig.numPointLights = config.numPointLights;
				lowConfig.numSpotLights = config.numSpotLights;
				lowConfig.numDirectionalLights = config.numDirectionalLights;
				bdxDefaultShader = new BDXDefaultShader(renderable, lowConfig);
			}
		}

		bdxDefaultShader.shaderProvider = this;

		return bdxDefaultShader;
	}

	public void update(Scene scene){
		this.scene = scene;
	}

	public void deleteShaders(){
		for (Shader s : shaders){
			BDXDefaultShader bdxDefaultShader = (BDXDefaultShader) s;
			if (bdxDefaultShader.applyingMaterial != null && bdxDefaultShader.applyingMaterial.shader != null)
				bdxDefaultShader.applyingMaterial.shader.dispose();	// "Empty" (dispose()) the MaterialShader.
			else
				s.dispose();
		}
		shaders.clear();
	}

}
