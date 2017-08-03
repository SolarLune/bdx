package com.nilunder.bdx.utils;

import java.util.ArrayList;

public class ArrayListNamed<T extends Named> extends ArrayList<T>{

	public T get(String name){
		for (T x: this){
			if (x.name().equals(name)){
				return x;
			}
		}
		return null;
	}
	
	public int indexOf(String name){
		return indexOf(get(name));
	}
	
	public T remove(String name){
		T x = get(name);
		remove(x);
		return x;
	}

	public void moveUp(T object) {
		moveTo(object, Math.min(indexOf(object) + 1, size() - 1));
	}

	public void moveUp(String name) {
		moveUp(get(name));
	}

	public void moveDown(T object) {
		moveTo(object, Math.max(0, indexOf(object) - 1));
	}

	public void moveDown(String name) {
		moveDown(get(name));
	}

	public void moveTo(T object, int index) {
		int oldIndex = indexOf(object);
		remove(oldIndex);
		add(index, object);
	}

	public void moveTo(String name, int index) {
		moveTo(get(name), index);
	}
	
}
