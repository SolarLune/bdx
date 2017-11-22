package com.nilunder.bdx.utils;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import javax.vecmath.Matrix4f;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.gl.Mesh;
import com.nilunder.bdx.gl.Material;

public class JoinData extends HashMap<Mesh, ArrayList<Matrix4f>>{
	
	public HashMap<Material, JoinData.Part> parts;
	
	private Matrix4f relativeTransform;
	
	public class Part extends LinkedHashMap<Matrix4f, float[]>{
		private int numVertices;
		
		public int numVertices(){
			return numVertices;
		}
		public int numIndices(){
			return numVertices / Bdx.VERT_STRIDE;
		}
		@Override
		public float[] put(Matrix4f transform, float[] vertices){
			float[] verticesOld = get(transform);
			if (verticesOld != null){
				numVertices -= verticesOld.length;
			}
			numVertices += vertices.length;
			return super.put(transform, vertices);
		}
		@Override
		public void putAll(Map<? extends Matrix4f, ? extends float[]> map){
			for (Map.Entry<? extends Matrix4f, ? extends float[]> e : map.entrySet()){
				put(e.getKey(), e.getValue());
			}
		}
		@Override
		public float[] remove(Object o){
			Matrix4f transform = (Matrix4f) o;
			float[] vertices = super.remove(transform);
			if (vertices != null){
				numVertices -= vertices.length;
			}
			return vertices;
		}
		@Override
		public void clear(){
			super.clear();
			numVertices = 0;
		}
	}
	
	public JoinData(){
		parts = new HashMap<Material, JoinData.Part>();
		relativeTransform = Matrix4f.identity();
	}
	
	public JoinData(HashMap<Mesh, ArrayList<Matrix4f>> map){
		this();
		putAll(map);
	}
	
	public JoinData(ArrayList<GameObject> objects, boolean endObjects, Matrix4f relativeTransform){
		this();
		
		this.relativeTransform.set(relativeTransform);
		
		HashMap<Mesh, ArrayList<Matrix4f>> map = new HashMap<Mesh, ArrayList<Matrix4f>>();
		
		Mesh m;
		ArrayList<Matrix4f> l;
		
		for (GameObject g : objects){
			m = g.mesh();
			l = map.get(m);
			if (l == null){
				l = new ArrayList<Matrix4f>();
				map.put(m, l);
			}
			l.add(g.transform());
			if (endObjects){
				g.endNoChildren();
			}
		}
		
		putAll(map);
	}
	
	public JoinData(ArrayList<GameObject> objects, boolean endObjects){
		this(objects, endObjects, Matrix4f.identity());
	}
	
	public JoinData(ArrayList<GameObject> objects){
		this(objects, true);
	}
	
	public JoinData(String s){
		this();
		deserialize(s);
	}
	
	@Override
	public ArrayList<Matrix4f> put(Mesh mesh, ArrayList<Matrix4f> transforms){
		for (Matrix4f transform : transforms){
			add(mesh, transform);
		}
		return transforms;
	}
	
	@Override
	public void putAll(Map<? extends Mesh, ? extends ArrayList<Matrix4f>> map){
		for (Map.Entry<? extends Mesh, ? extends ArrayList<Matrix4f>> e : map.entrySet()){
			put(e.getKey(), e.getValue());
		}
	}
	
	@Override
	public void clear(){
		super.clear();
		parts.clear();
	}
	
	private void update(){
		HashMap<Mesh, ArrayList<Matrix4f>> map = new HashMap<Mesh, ArrayList<Matrix4f>>(this);
		clear();
		putAll(map);
	}
	
	public Matrix4f relativeTransform(){
		return new Matrix4f(relativeTransform);
	}
	
	public void relativeTransform(Matrix4f relativeTransform){
		this.relativeTransform.set(relativeTransform);
		update();
	}
	
	public void add(Mesh mesh, Matrix4f transform){
		ArrayList<Matrix4f> transforms = get(mesh);
		if (transforms == null){
			transforms = new ArrayList<Matrix4f>();
			super.put(mesh, transforms);
		}else if (transforms.contains(transform)){
			return;
		}
		transforms.add(transform);
		
		Matrix4f t = relativeTransform.inverted();
		t.mul(transform);
		for (int i = 0; i < mesh.materials.size(); i++){
			Material mat = mesh.materials.get(i);
			float[] vertices = mesh.verticesTransformed(i, t);
			JoinData.Part part = parts.get(mat);
			if (part == null){
				part = new JoinData.Part();
				parts.put(mat, part);
			}
			part.put(transform, vertices);
		}
	}
	
	public void remove(Mesh mesh, Matrix4f transform){
		ArrayList<Matrix4f> transforms = get(mesh);
		if (transforms != null && transforms.remove(transform)){
			if (transforms.isEmpty()){
				remove(mesh);
			}else{
				for (Material mat : mesh.materials){
					JoinData.Part part = parts.get(mat);
					float[] vertices = part.remove(transform);
					if (vertices != null){
						if (part.isEmpty()){
							parts.remove(mat);
						}
					}
				}
			}
		}
	}
	
	public void remove(Mesh mesh){
		if (super.remove(mesh) != null){
			update();
		}
	}
	
	public void remove(Matrix4f transform){
		for (Mesh mesh : keySet()){
			remove(mesh, transform);
		}
	}
	
	private static class Serialized{
		protected float[] transform = new float[16];
		protected HashMap<String, ArrayList<float[]>> models = new HashMap<String, ArrayList<float[]>>();
	}
	
    public String serialized(){
		Serialized s = new Serialized();
		relativeTransform.get(s.transform);
		for (Map.Entry<Mesh, ArrayList<Matrix4f>> e : entrySet()){
			String meshName = e.getKey().name();
			ArrayList<float[]> l = new ArrayList<float[]>();
			for (Matrix4f t : e.getValue()){
				l.add(t.floatArray());
			}
			s.models.put(meshName, l);
		}
		return new Json().toJson(s);
	}
	
	public void deserialize(String s){
		clear();
		JsonValue json = new JsonReader().parse(s);
		relativeTransform.set(json.get("transform").asFloatArray());
		JsonValue models = json.get("models");
		if (models == null){
			return;
		}
		for (JsonValue meshData : models){
			Mesh mesh = null;
			for (Scene scene : Bdx.scenes){
				if (scene.meshes.containsKey(meshData.name)){
					mesh = scene.meshes.get(meshData.name);
					break;
				}
			}
			try{
				ArrayList<Matrix4f> l = new ArrayList<Matrix4f>();
				for (JsonValue j : meshData){
					add(mesh, new Matrix4f(j.asFloatArray()));
				}
			}catch (Error e){
				throw new RuntimeException("ERROR: Mesh " + meshData.name + " does not exist.");
			}
		}
	}
	
}
