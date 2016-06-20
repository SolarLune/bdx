package com.nilunder.bdx.utils;

import java.util.LinkedList;

public class LinkedListNamed<T extends Named> extends LinkedList<T>{

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
	
}
