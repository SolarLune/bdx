package com.nilunder.bdx.utils;

import java.util.ArrayList;

import javax.vecmath.Vector3f;

public class Random{

	private static java.util.Random R;

	public static <T> T choice(ArrayList<T> list){
		if (R == null){
			R = new java.util.Random();
		}
		return list.get(R.nextInt(list.size()));
	}

	public static float random(){
		if (R == null){
			R = new java.util.Random();
		}
		return R.nextFloat();
	}

	public static Vector3f direction(){
		ArrayList<Integer> ints = new ArrayList<Integer>();
		ints.add(1);
		ints.add(-1);
		return new Vector3f(
				Random.random() * Random.choice(ints), 
				Random.random() * Random.choice(ints),
				Random.random() * Random.choice(ints));
	}
}
