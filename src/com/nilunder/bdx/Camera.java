package com.nilunder.bdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector3;

import javax.vecmath.*;

public class Camera extends GameObject{

	public Matrix4f projection(){
		Matrix4f m = new Matrix4f();
		m.set(scene.cam.projection.getValues());
		return m;
	}

	public Vector2f screenPosition(Vector3f worldPosition){

		Vector3 out = scene.cam.project(new Vector3(worldPosition.x, worldPosition.y, worldPosition.z));

		return new Vector2f(out.x / Gdx.app.getGraphics().getWidth(), out.y / Gdx.app.getGraphics().getHeight());

	}

}
