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
}
