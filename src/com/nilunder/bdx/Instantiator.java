package com.nilunder.bdx;

import com.badlogic.gdx.utils.JsonValue;

public class Instantiator {

	public GameObject newObject(JsonValue gobj){
		String type = gobj.get("type").asString();
		if (type.equals("CAMERA")){
			return new Camera();
		}
		if (type.equals("FONT")){
			return new Text();
		}
		if (type.equals("LAMP")){
			return new Light();
		}
		return new GameObject();
	}
	
}
