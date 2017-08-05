package com.nilunder.bdx.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;

public class RenderBuffer extends FrameBuffer{
		
	private SpriteBatch batch;
	public TextureRegion region;
		
	public RenderBuffer(SpriteBatch batch, int bufferWidth, int bufferHeight) {
		super(Pixmap.Format.RGBA8888, bufferWidth, bufferHeight, true);
		this.batch = batch;
		region = new TextureRegion(this.getColorBufferTexture());
		region.flip(false, true);
	}	
	
	public RenderBuffer(SpriteBatch batch) {
		this(batch, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());		
	}
	
	public void drawTo(RenderBuffer dest, ScreenShader filter, float x, float y, float w, float h){

		if (dest != null) {
			if (ScreenShader.nearestFiltering)
				getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
			else
				getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

			dest.begin();
		} else										// Rendering to the screen, so nearest is good to keep things sharp
				getColorBufferTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

		if (filter != null)
			batch.setShader(filter.program);		// Set shader BEFORE calling begin() (avoids shader switching)
		else
			batch.setShader(null);
		batch.begin();
		if (filter != null && filter.program != null) {
			for (UniformSet uniformSet : filter.uniformSets)
				uniformSet.set(filter.program);
		}
		batch.enableBlending();
		batch.setBlendFunction(GL20.GL_ONE, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.draw(region, x, y, w, h);
		batch.end();
		
		if (dest != null)
			dest.end();
	}

	public void drawTo(RenderBuffer dest){
		drawTo(dest, null, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	public void clear(){
		
		begin();
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		end();
		
	}

}
