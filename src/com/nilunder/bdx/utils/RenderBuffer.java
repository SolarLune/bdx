package com.nilunder.bdx.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.nilunder.bdx.ShaderProgram;

public class RenderBuffer extends FrameBuffer{
		
	private SpriteBatch batch;
	private TextureRegion region;
		
	public RenderBuffer(SpriteBatch batch, int bufferWidth, int bufferHeight) {
		super(Pixmap.Format.RGBA8888, bufferWidth, bufferHeight, true);
		this.batch = batch;
		region = new TextureRegion(this.getColorBufferTexture());
		region.flip(false, true);
	}	
	
	public RenderBuffer(SpriteBatch batch) {
		this(batch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());		
	}
	
	public void drawTo(RenderBuffer dest,ShaderProgram filter){

		if (ShaderProgram.nearestFiltering) {
			if (dest != null)
				dest.getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		}
		else {
			if (dest != null)
				dest.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		}
			
		if (dest != null)
			dest.begin();
				
		batch.begin();
		batch.enableBlending();
		batch.setShader(filter);
		batch.draw(region, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		batch.end();
		
		if (dest != null)
			dest.end();
	}

	public void drawTo(RenderBuffer dest){
		drawTo(dest, null);
	}
	
	public void clear(){
		
		begin();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		end();
		
	}

}
