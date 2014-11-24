package com.nilunder.bdx;

import javax.vecmath.*;

public class Camera extends GameObject{

	public Matrix4f projection(){
		Matrix4f m = new Matrix4f();
		m.set(scene.cam.projection.getValues());
		return m;
	}
}
