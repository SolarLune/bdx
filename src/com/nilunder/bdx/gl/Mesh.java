package com.nilunder.bdx.gl;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.utils.ArrayListNamed;
import com.nilunder.bdx.utils.Color;
import com.nilunder.bdx.utils.Named;
import com.nilunder.bdx.utils.JoinData;

public class Mesh implements Named, Disposable {

	public Model model;
	public String name;
	public Scene scene;
	public Vector3f median;
	public Vector3f dimensions;
	public ArrayListMaterials materials;
	public ArrayList<ModelInstance> instances;
	public boolean autoDispose;

	public class ArrayListMaterials extends ArrayListNamed<Material> {

		public Material set(int index, Material material) {
			model.nodes.first().parts.get(index).material = material;
			for (ModelInstance m : instances)
				m.nodes.first().parts.get(index).material = material;
			return super.set(index, material);
		}

		public Material set(int index, String matName) {
			return set(index, scene.materials.get(matName));
		}

		public void set(Material material){
			for (int i = 0; i < size(); i++) {
				set(i, material);
			}
		}

		public void set(String matName){
			set(scene.materials.get(matName));
		}

		public void color(Color color){
			for (Material mat : this)
				mat.color(color);
		}

		public void tint(Color color){
			for (Material mat : this)
				mat.tint(color);
		}

		public void blendMode(int src, int dest){
			for (Material mat : this)
				mat.blendMode(src, dest);
		}

		public void shadeless(boolean shadelessness){
			for (Material mat : this)
				mat.shadeless(shadelessness);
		}

	}

	private static Model createModel(JsonValue model, Scene scene, Integer vertexArrayLength){
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		short idx = 0;
		for (JsonValue m : model.get("materials")){
			Material mat = scene.materials.get(m.name);
			if (mat.name().equals(scene.defaultMaterial.name())){
				mat = new Material(mat);
			}
			MeshPartBuilder mpb = builder.part(mat.name(), GL20.GL_TRIANGLES,
					Usage.Position | Usage.Normal | Usage.TextureCoordinates, mat);
			float[] verts = m.asFloatArray();
			int vertexCount;
			if (vertexArrayLength != null && vertexArrayLength != verts.length){
				verts = Arrays.copyOf(verts, vertexArrayLength);
				vertexCount = vertexArrayLength / Bdx.VERT_STRIDE;
			}else{
				vertexCount = verts.length / Bdx.VERT_STRIDE;
			}
			mpb.vertex(verts);
			try{
				for (short i = 0; i < vertexCount; ++i){
					mpb.index(idx);
					idx += 1;
				}
			}catch (Error e){
				throw new RuntimeException("MODEL ERROR: Model parts with more than 4095 vertices are not supported. Part " + mat.name() + " of " + model.name + " has " + Integer.toString(vertexCount) + " vertices.");
			}
		}
		return builder.end();
	}
	
	private static Model createModel(JsonValue model, Scene scene){
		return createModel(model, scene, null);
	}
	
	private static Model createModelJoined(JoinData data){
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		short idx = 0;
		for (Map.Entry<Material, JoinData.Part> e : data.parts.entrySet()){
			Material mat = e.getKey();
			JoinData.Part part = e.getValue();
			int vertexArrayLengthJoined = part.vertexArrayLength();
			float[] verticesJoined = new float[vertexArrayLengthJoined];
			
			int j = 0;
			for (float[] vertices : part.values()){
				int vertexArrayLength = vertices.length;
				for (int i = 0; i < vertexArrayLength; i++){
					verticesJoined[i + j] = vertices[i];
				}
				j += vertexArrayLength;
			}
			
			MeshPartBuilder mpb = builder.part(mat.name(), GL20.GL_TRIANGLES, Usage.Position | Usage.Normal | Usage.TextureCoordinates, mat);
			mpb.vertex(verticesJoined);
			int vertexCount = vertexArrayLengthJoined / Bdx.VERT_STRIDE;
			try{
				for (short i = 0; i < vertexCount; i++){
					mpb.index(idx);
					idx++;
				}
			}catch (Error error){
				throw new RuntimeException("MODEL ERROR: Model parts with more than 4095 vertices are not supported. Part " + mat.name() + " of the joined model has " + Integer.toString(vertexCount) + " vertices.");
			}
		}
		
		return builder.end();
	}
	
	public void updateMedianDimensions(){
		BoundingBox bbox = model.meshes.first().calculateBoundingBox();
		Vector3 tmp = bbox.getCenter(new Vector3());
		median = new Vector3f(tmp.x, tmp.y, tmp.z);
		bbox.getDimensions(tmp);
		dimensions = new Vector3f(tmp.x, tmp.y, tmp.z);
	}
	
	private Mesh(Model model, Scene scene, String name, boolean updateMedianDimensions){
		this.model = model;
		this.name = name;
		this.scene = scene;
		materials = new ArrayListMaterials();
		for (NodePart part : model.nodes.first().parts)
			materials.add((Material) part.material);
		instances = new ArrayList<ModelInstance>();
		if (updateMedianDimensions){
			updateMedianDimensions();
		}
		autoDispose = true;
	}
	
	public Mesh(Model model, Scene scene, String name){
		this(model, scene, name, true);
	}
	
	public Mesh(JsonValue model, Scene scene, String name, Integer vertexArrayLength){
		this(createModel(model, scene, vertexArrayLength), scene, name, false);
		median = new Vector3f(model.get("median").asFloatArray());
		dimensions = new Vector3f(model.get("dimensions").asFloatArray());
	}
	
	public Mesh(JsonValue model, Scene scene, String name){
		this(model, scene, name, null);
	}
	
	public Mesh(JsonValue model, Scene scene){
		this(model, scene, model.name);
	}
	
	public Mesh(String serialized, Scene scene, String name, Integer vertexArrayLength){
		this(new JsonReader().parse(serialized), scene, name, vertexArrayLength);
	}
	
	public Mesh(String serialized, Scene scene, String name){
		this(serialized, scene, name, null);
	}
	
	public Mesh(JoinData data, Scene scene, String name){
		this(createModelJoined(data), scene, name);
	}
	
	public int offset(int ms){
		return model.meshParts.get(ms).offset;
	}
	
	public int vertexCount(){
		return model.meshes.first().getNumIndices();
	}
	
	public int vertexCount(int ms){
		return model.meshParts.get(ms).size;
	}
	
	public int vertexArrayLength(){
		return vertexCount() * Bdx.VERT_STRIDE;
	}
	
	public int vertexArrayLength(int ms){
		return vertexCount(ms) * Bdx.VERT_STRIDE;
	}
	
	public float[] vertices(){
		return model.meshes.first().getVertices(new float[vertexArrayLength()]);
	}
	
	private float[] vertices(int offset, int vertexArrayLength){
		return model.meshes.first().getVertices(offset, vertexArrayLength, new float[vertexArrayLength]);
	}
	
	public float[] vertices(int ms){
		MeshPart mp = model.meshParts.get(ms);
		int offset = mp.offset * Bdx.VERT_STRIDE;
		int vertexArrayLength = mp.size * Bdx.VERT_STRIDE;
		return mp.mesh.getVertices(offset, vertexArrayLength, new float[vertexArrayLength]);
	}
	
	public void vertices(float[] va){
		model.meshes.first().setVertices(va);
	}
	
	public void vertices(int ms, float[] va){
		MeshPart mp = model.meshParts.get(ms);
		int offset = mp.offset * Bdx.VERT_STRIDE;
		int vertexArrayLength = mp.size * Bdx.VERT_STRIDE;
		mp.mesh.setVertices(va, offset, vertexArrayLength);
	}
	
	private float[] verticesTransformed(float[] va, Matrix4f trans){
		int vertexArrayLength = va.length;
		int vertexCount = va.length / Bdx.VERT_STRIDE;
		float[] tva = new float[vertexArrayLength];
		
		Matrix4f t = Matrix4f.identity();
		Vector3f v = new Vector3f();
		Matrix4f transNoPos = new Matrix4f(trans);
		transNoPos.position(v);
		Matrix4f vertTrans = new Matrix4f();
		Matrix4f normTrans = new Matrix4f();
		int j = 0;
		for (int i = 0; i < vertexCount; i++){
			vertTrans.set(trans);
			v.set(va[j], va[j+1], va[j+2]);
			t.position(v);
			vertTrans.mul(t);
			
			normTrans.set(transNoPos);
			v.set(va[j+3], va[j+4], va[j+5]);
			t.position(v);
			normTrans.mul(t);
			
			tva[j] = vertTrans.m03;
			tva[j+1] = vertTrans.m13;
			tva[j+2] = vertTrans.m23;
			tva[j+3] = normTrans.m03;
			tva[j+4] = normTrans.m13;
			tva[j+5] = normTrans.m23;
			tva[j+6] = va[j+6];
			tva[j+7] = va[j+7];
			j += Bdx.VERT_STRIDE;
		}
		
		return tva;
	}
	
	public float[] verticesTransformed(int ms, Matrix4f t){
		return verticesTransformed(vertices(ms), t);
	}
	
	public float[] verticesTransformed(Matrix4f t){
		return verticesTransformed(vertices(), t);
	}
	
	// [Normals are not calculated with Matrix4.transform() methods](https://github.com/libgdx/libgdx/blob/master/gdx/src/com/badlogic/gdx/graphics/Mesh.java#L903-L948)
	
	public void transform(Matrix4f t){
		vertices(verticesTransformed(t));
	}
	
	public void transform(int ms, Matrix4f t){
		vertices(ms, verticesTransformed(ms, t));
	}
	
	public void transformUV(Matrix3f t){
		Matrix3 trans = new Matrix3(t.transposed().floatArray());
		model.meshes.first().transformUV(trans);
	}
	
	public void transformUV(int ms, Matrix3f t){
		MeshPart mp = model.meshParts.get(ms);
		Matrix3 trans = new Matrix3(t.transposed().floatArray());
		float[] verts = vertices();
		com.badlogic.gdx.graphics.Mesh.transformUV(trans, verts, Bdx.VERT_STRIDE, 6, mp.offset, mp.size);
		vertices(verts);
	}
	
	public void vertPos(int ms, int index, float x, float y, float z){
		int i = (offset(ms) + index) * Bdx.VERT_STRIDE;
		float[] verts = vertices();
		verts[i] = x;
		verts[i+1] = y;
		verts[i+2] = z;
		vertices(verts);
	}
	
	public void vertPos(int ms, int index, Vector3f pos){
		vertPos(ms, index, pos.x, pos.y, pos.z);
	}
	
	public Vector3f vertPos(int ms, int index){
		int i = (offset(ms) + index) * Bdx.VERT_STRIDE;
		return new Vector3f(vertices(i, 3));
	}
	
	public void vertNor(int ms, int index, float x, float y, float z){
		int i = (offset(ms) + index) * Bdx.VERT_STRIDE;
		float[] verts = vertices();
		verts[i+3] = x;
		verts[i+4] = y;
		verts[i+5] = z;
		vertices(verts);
	}
	
	public void vertNor(int ms, int index, Vector3f pos){
		vertNor(ms, index, pos.x, pos.y, pos.z);
	}
	
	public Vector3f vertNor(int ms, int index){
		int i = (offset(ms) + index) * Bdx.VERT_STRIDE;
		return new Vector3f(vertices(i + 3, 3));
	}
	
	public void vertUV(int ms, int index, float u, float v){
		int i = (offset(ms) + index) * Bdx.VERT_STRIDE;
		float[] verts = vertices();
		verts[i+6] = u;
		verts[i+7] = v;
		vertices(verts);
	}
	
	public void vertUV(int ms, int index, Vector2f uv){
		vertUV(ms, index, uv.x, uv.y);
	}
	
	public Vector2f vertUV(int ms, int index){
		int i = (offset(ms) + index) * Bdx.VERT_STRIDE;
		return new Vector2f(vertices(i + 6, 2));
	}
	
	private class Serialized{
		private HashMap<String, float[]> materials;
		private float[] median;
		private float[] dimensions;
		
		protected Serialized(){
			Mesh outer = Mesh.this;
			materials = new HashMap<String, float[]>();
			for (int i = 0; i < outer.materials.size(); i++){
				materials.put(outer.materials.get(i).name(), vertices(i));
			}
			median = new float[3];
			dimensions = new float[3];
			outer.median.get(median);
			outer.dimensions.get(dimensions);
		}
	}
	
	public String serialized(){
		return new Json().toJson(new Serialized());
	}

	public String name(){
		return name;
	}

	public Mesh copy(String newName){
		Mesh newMesh = new Mesh(new JsonReader().parse(serialized()), scene, newName);
		newMesh.materials.clear();

		for (NodePart part : newMesh.model.nodes.first().parts) {
			Material newMat = new Material( (Material) part.material );			// Don't forget to cast to Material for it to be a true copy (see shader copying)
			newMesh.materials.add(newMat);
			part.material = newMat;
		}

		scene.meshCopies.add(newMesh);

		return newMesh;
	}

	public Mesh copy(){
		return copy(name);
	}

	public ModelInstance getNewModelInstance(){
		ModelInstance mi = new ModelInstance(model);
		for (int i = 0; i < mi.nodes.first().parts.size; i++)
			mi.nodes.first().parts.get(i).material = materials.get(i);
		instances.add(mi);
		return mi;
	}

	public String toString(){
		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());
	}

	// Also UV, Normal, and Transforms (and possibly "do this to all" versions that don't use transforms?)

	public void dispose() {
		if (scene != null)
			scene.meshCopies.remove(this);
		if (model != null)
			model.dispose();
		model = null;
		scene = null;
	}

	public boolean valid() {
		return model != null;
	}

}
