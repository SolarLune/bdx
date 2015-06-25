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
		
	public RenderBuffer(SpriteBatch batch) {
		
		super(Pixmap.Format.RGBA8888, Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);
		getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
		this.batch = batch;
				
	}	
	
	public void drawTo(RenderBuffer dest,ShaderProgram filter){
		
		TextureRegion region = new TextureRegion(this.getColorBufferTexture());
		region.flip(false, true);
		
		if (dest != null)
			dest.begin();
				
		batch.begin();
		batch.setShader(filter);
		batch.draw(region, 0, 0);
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
