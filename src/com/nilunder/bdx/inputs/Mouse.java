package com.nilunder.bdx.inputs;

import java.util.*;

import javax.vecmath.Vector2f;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

import com.nilunder.bdx.*;

public class Mouse extends Finger{

	public GdxProcessor.UpDownLog[] codeToLog;

	private HashMap<String,Integer> btnToCode;

	private boolean cursorVisible;
	public String cursorTexture;
	public int cursorOffsetX;
	public int cursorOffsetY;

	public Mouse(){
		codeToLog = new GdxProcessor.UpDownLog[5];
		for (int i = 0; i < 5; ++i)
			codeToLog[i] = new GdxProcessor.UpDownLog();

		btnToCode = new HashMap<>();
		btnToCode.put("left", 0);
		btnToCode.put("right", 1);
		btnToCode.put("middle", 2);
		btnToCode.put("back", 3);
		btnToCode.put("forward", 4);

		cursorVisible = true;
	}

	public boolean btnHit(String btn){
		GdxProcessor.UpDownLog b = codeToLog[btnToCode.get(btn)];
		return b.hit == GdxProcessor.currentTick;
	}

	public boolean btnDown(String btn){
		GdxProcessor.UpDownLog b = codeToLog[btnToCode.get(btn)];
		return b.hit > b.up;
	}

	public boolean btnUp(String btn){
		GdxProcessor.UpDownLog b = codeToLog[btnToCode.get(btn)];
		return b.up == GdxProcessor.currentTick;
	}

	public boolean clicked(GameObject g){
		return clicked(g, "left");
	} 

	public boolean clicked(GameObject g, String btn){
		if (btnHit(btn)){
			RayHit rh = ray();
			if (rh != null && rh.object == g){
				return true;
			}
		}

		return false;
	}

	public void position(int x, int y) {
		Gdx.input.setCursorPosition(x, y);	
	}

	public void position(Vector2f vec){
		position((int)vec.x, (int)vec.y);
	}

	public void positionNormalized(float x, float y){
		Vector2f c = Bdx.display.center();
		position((int)(x * c.x * 2), (int)((1 - y) * c.y * 2));
	}

	public void positionNormalized(Vector2f vec){
		positionNormalized(vec.x, vec.y);
	}

	public void cursorLock(boolean confine) {
		Gdx.input.setCursorCatched(confine);	
	}

	public boolean cursorLock() {
		return Gdx.input.isCursorCatched();
	}

	public void setCursorImage(String textureName, int offsetX, int offsetY) {
		cursorTexture = textureName;
		cursorOffsetX = offsetX;
		cursorOffsetY = offsetY;
		visible(visible());
	}

	public void setCursorImage(String textureName) {
		setCursorImage(textureName, 0, 0);
	}

	public void visible(boolean visible) {
		cursorVisible = visible;

		if (visible) {
			if (cursorTexture == null)
				Gdx.input.setCursorImage(null, 0, 0);
			else {

				Pixmap px = new Pixmap(Gdx.files.internal("bdx/textures/" + cursorTexture));
				Gdx.input.setCursorImage(px, cursorOffsetX, cursorOffsetY);
				px.dispose();

			}
		}
		else {

			Pixmap px = new Pixmap(16, 16, Pixmap.Format.RGBA8888);
			Gdx.input.setCursorImage(px, 0, 0);
			px.dispose();

		}
	}

	public boolean visible(){
		return cursorVisible;
	}

}
