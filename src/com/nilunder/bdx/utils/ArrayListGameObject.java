package com.nilunder.bdx.utils;

import com.nilunder.bdx.GameObject;

public class ArrayListGameObject extends ArrayListNamed<GameObject> {
	
	public GameObject getByProperty(String propName){
		for (GameObject t : this) {
			if (t.props.containsKey(propName)) {
				return t;
			}
		}
		return null;
		
	}
	
	public GameObject getByComponent(String compName){
		for (GameObject t : this) {
			if (t.components.get(compName) != null) {
				return t;
			}
		}
		return null;
	}
	
	public ArrayListGameObject getObjectsByProperty(String propName) {
		ArrayListGameObject ret = new ArrayListGameObject();
		for (GameObject t : this) {
			if (t.props.containsKey(propName))
				ret.add(t);
		}
		return ret;
	}
	
	public ArrayListGameObject getObjectsByComponent(String compName) {
		ArrayListGameObject ret = new ArrayListGameObject();
		for (GameObject t : this) {
			if (t.components.get(compName) != null)
				ret.add(t);
		}
		return ret;
	}
	
}
