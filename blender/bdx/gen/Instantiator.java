package com.comp.project.inst;

import com.badlogic.gdx.utils.JsonValue;
import com.nilunder.bdx.Instantiator;
import com.nilunder.bdx.*;
import com.nilunder.bdx.components.*;
import com.comp.project.*;


public class NAME extends Instantiator {

	public GameObject newObject(JsonValue gobj) {
		String name = gobj.name;

		if (gobj.get("class").asString().equals("NAME"))
			return new NAME();

		return super.newObject(gobj);
	}

	public void loadInComponents(GameObject g) {

	}
	
}
