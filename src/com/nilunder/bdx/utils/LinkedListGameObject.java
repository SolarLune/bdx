package com.nilunder.bdx.utils;

import java.util.HashMap;

import com.nilunder.bdx.GameObject;

public class LinkedListGameObject extends LinkedListNamed<GameObject> {
	
	public GameObject getByProperty(String propName){
		for (GameObject g : this) {
			if (g.props.containsKey(propName)) {
				return g;
			}
		}
		return null;
	}
	
	public GameObject getByComponent(String compName){
		for (GameObject g : this) {
			if (g.components.get(compName) != null) {
				return g;
			}
		}
		return null;
	}
	
	public LinkedListGameObject getObjectsByProperty(String propName) {
		LinkedListGameObject l = new LinkedListGameObject();
		for (GameObject g : this) {
			if (g.props.containsKey(propName))
				l.add(g);
		}
		return l;
	}
	
	public LinkedListGameObject getObjectsByComponent(String compName) {
		LinkedListGameObject l = new LinkedListGameObject();
		for (GameObject g : this) {
			if (g.components.get(compName) != null)
				l.add(g);
		}
		return l;
	}
	
	public LinkedListGameObject group(String groupName){
		LinkedListGameObject l = new LinkedListGameObject();
		for (GameObject g : this){
			if (g.groups.contains(groupName)){
				l.add(g);
			}
		}
		return l;
	}
	
	public HashMap<String, LinkedListGameObject> groups(){
		HashMap<String, LinkedListGameObject> m = new HashMap<String, LinkedListGameObject>();
		for (GameObject g : this){
			for (String groupName : g.groups){
				if (!m.containsKey(groupName)){
					m.put(groupName, group(groupName));
				}
			}
		}
		return m;
	}
	
}
