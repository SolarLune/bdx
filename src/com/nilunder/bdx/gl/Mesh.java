package com.nilunder.bdx.gl;

import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.utils.ArrayListNamed;
import com.nilunder.bdx.utils.Color;
import com.nilunder.bdx.utils.Named;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;

public class Mesh implements Named {

	public Model library;
	public String name;
	public Scene scene;
	public ArrayListMaterials materials;
	public ArrayList<ModelInstance> instances;
	public Node modelRoot = null;
	public Node armature = null;

	public class ArrayListMaterials extends ArrayListNamed<Material> {

		public Material set(int index, Material material) {
			for (Node n : library.nodes) {
				if (n.parts.size > index)
					n.parts.get(index).material = material;
			}
			for (ModelInstance m : instances) {
				for (Node n : m.nodes) {
					if (n.parts.size > index)
						n.parts.get(index).material = material;
				}
			}
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

	public Mesh(Model library, Scene scene, String name, Node modelRoot){

		this.library = library;
		this.name = name;
		this.scene = scene;
		if (modelRoot != null)
			this.modelRoot = modelRoot;
		else
			this.modelRoot = library.nodes.first();

		if (scene.json.get("meshes_to_armatures").get(name) != null)
			armature = library.getNode(scene.json.get("meshes_to_armatures").get(name).asString());

		materials = new ArrayListMaterials();

		for (NodePart part : this.modelRoot.parts) {

			if (part.material.id.equals("__BDX_DEFAULT"))
				part.material = scene.defaultMaterial;
			else
				part.material = scene.materials.get(part.material.id);

			materials.add((Material) part.material);

		}

		instances = new ArrayList<ModelInstance>();
	}

	public Mesh(Model library, Scene scene){
		this(library, scene, library.meshParts.first().id, null);
	}

	protected void finalize() throws Throwable {
		library.dispose();
	}

	public void debug(){

		for (int i = 0; i < materials.size(); i++) {
			System.out.println("-----");
			System.out.println("material: " + materials.get(i).name());
			System.out.println("-----");
			System.out.println("index count: " + indexNum(i));
			System.out.println("polygon count: " + polyNum(i));
			System.out.println("vertex count: " + vertNum(i));
			String s = "";
			for (short ind : indices(i))
				s += ind + " ";
			System.out.println("Indices: " + s);
		}

		System.out.println("-----");
		System.out.println("Totals");
		System.out.println("-----");
		System.out.println("index count: " + indexNum());
		System.out.println("polygon count: " + polyNum());
		System.out.println("vertex count: " + vertNum());
		System.out.println("vert stride: " + vertStride());

		String s = "";
		for (short ind : indices())
			s += ind + " ";
		System.out.println("indices: " + s);
		s = "";
		float[] verts = verts();
		for (int v = 0; v < verts.length; v++) {
			if (v % vertStride() == 0)
				s += "\n";
			s += verts[v] + " ";
		}
		System.out.println("verts: " + s);

	}

	public int offset(int materialSlot) {
		return modelRoot.parts.get(materialSlot).meshPart.offset;
	}

    public int indexNum(int materialSlot) {
		return modelRoot.parts.get(materialSlot).meshPart.size;
    }

    public int indexNum(){
        int count = 0;
		for (int i = 0; i < modelRoot.parts.size; i++)
			count += indexNum(i);
        return count;
    }

    public int polyNum(int materialSlot) {
		return indexNum(materialSlot) / 3;
	}

	public int polyNum() {
    	return indexNum() / 3;
	}

	public int vertNum(int materialSlot) {
    	ArrayList<Short> count = new ArrayList<Short>();
		for (short i : indices(materialSlot)) {
			if (!count.contains(i))
				count.add(i);
		}
		return count.size();
	}

	public int vertNum() {
		ArrayList<Short> count = new ArrayList<Short>();
		for (short i : indices()) {
			if (!count.contains(i))
				count.add(i);
		}
		return count.size();
	}

    public short[] indices(int materialSlot) {

    	MeshPart part = modelRoot.parts.get(materialSlot).meshPart;
    	short[] indexList = new short[part.size];
    	part.mesh.getIndices(part.offset, part.size, indexList, 0);
    	return indexList;

	}

	public short[] indices() {
    	short[] indexList = new short[indexNum()];
		modelRoot.parts.first().meshPart.mesh.getIndices(modelRoot.parts.first().meshPart.offset, indexNum(), indexList, 0);
		return indexList;
	}

	public float[] verts() {
		MeshPart part = modelRoot.parts.first().meshPart;
		float[] vertList = new float[part.mesh.getNumVertices() * vertStride()];
		part.mesh.getVertices(vertList);
    	return vertList;
	}

	public void verts(float[] vertices) {
		MeshPart part = modelRoot.parts.first().meshPart;
		part.mesh.setVertices(vertices, 0, vertices.length);
	}

    public int vertStride() {
    	int c = 0;
    	for (VertexAttribute va : modelRoot.parts.first().meshPart.mesh.getVertexAttributes())
    		c += va.numComponents;
    	return c;
	}

    public void vertPos(int materialSlot, int index, float x, float y, float z){
		float[] verts = verts();
		short[] s = indices(materialSlot);
		int st = vertStride();
        verts[s[index] * st] = x;
        verts[s[index] * st + 1] = y;
        verts[s[index] * st + 2] = z;
        verts(verts);
	}

    public void vertPos(int materialSlot, int index, Vector3f pos){
        vertPos(materialSlot, index, pos.x, pos.y, pos.z);
    }

    public Vector3f vertPos(int materialSlot, int index){
        float[] verts = verts();
        short[] s = indices(materialSlot);
        Vector3f pos = new Vector3f();
		int st = vertStride();
        pos.set(verts[s[index] * st],
				verts[s[index] * st + 1],
				verts[s[index] * st + 2]);
        return pos;
    }

    public void vertNor(int materialSlot, int index, float x, float y, float z){
		float[] verts = verts();
		short[] s = indices(materialSlot);
		int st = vertStride();
		verts[s[index] * st + 3] = x;
		verts[s[index] * st + 4] = y;
		verts[s[index] * st + 5] = z;
		verts(verts);
    }

    public void vertNor(int materialSlot, int index, Vector3f pos){
        vertNor(materialSlot, index, pos.x, pos.y, pos.z);
    }

    public Vector3f vertNor(int materialSlot, int index){
		float[] verts = verts();
		short[] s = indices(materialSlot);
		Vector3f nor = new Vector3f();
		int st = vertStride();
		nor.set(verts[s[index] * st + 3],
				verts[s[index] * st + 4],
				verts[s[index] * st + 5]);
		return nor;
    }

    public void vertUV(int materialSlot, int index, float u, float v){

		MeshPart meshPart = modelRoot.parts.get(materialSlot).meshPart;
		if (meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates) != null) {		// Might not have UV
			float[] verts = verts();
			short[] s = indices(materialSlot);
			int st = vertStride();
			verts[s[index] * st + 5] = u;
			verts[s[index] * st + 6] = v;
			verts(verts);
		}

    }

    public void vertUV(int materialSlot, int index, Vector2f uv){
        vertUV(materialSlot, index, uv.x, uv.y);
    }

    public Vector2f vertUV(int materialSlot, int index){
		if (modelRoot.parts.get(materialSlot).meshPart.mesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates) != null) {        // Might not have UV
			float[] verts = verts();
			short[] s = indices(materialSlot);
			Vector2f uv = new Vector2f();
			int st = vertStride();
			uv.set(verts[s[index] * st + 5],
					verts[s[index] * st + 6]);
			return uv;
		}
		else
			return new Vector2f();
    }

    public void vertTransform(int materialSlot, Matrix4f matrix){
        float[] vals = {matrix.m00, matrix.m10, matrix.m20, matrix.m30,
						matrix.m01, matrix.m11, matrix.m21, matrix.m31,
						matrix.m02, matrix.m12, matrix.m22, matrix.m32,
						matrix.m03, matrix.m13, matrix.m23, matrix.m33};
		modelRoot.parts.get(materialSlot).meshPart.mesh.transform(new Matrix4(vals));
    }

    public void vertTransformUV(int materialSlot, Matrix3f matrix){
        float[] vals = {matrix.m00, matrix.m10, matrix.m20,
                		matrix.m01, matrix.m11, matrix.m21,
                		matrix.m02, matrix.m12, matrix.m22};
        modelRoot.parts.get(materialSlot).meshPart.mesh.transformUV(new Matrix3(vals));
    }

	public String name(){
		return name;
	}

	public Mesh copy(){
		Node newRoot = modelRoot.copy();
		newRoot.id += "_Copy";
		library.nodes.add(newRoot);		// Gotta make a new copy of the mesh and add it to the library so getInstance can find it
		Mesh m = new Mesh(library, scene, name, newRoot);
		for (NodePart nodePart : m.modelRoot.parts)
			nodePart.meshPart.mesh = nodePart.meshPart.mesh.copy(false);
		return m;
	}

	public ModelInstance getInstance(){

		ModelInstance m;

		Array<String> rootNodes = new Array<String>();

		modelRoot.translation.set(0, 0, 0);

		if (armature != null)
			rootNodes.add(armature.id);
		if (modelRoot != null)
			rootNodes.add(modelRoot.id);

		m = new ModelInstance(library, rootNodes);

		for (Node n : m.nodes) {

			n.globalTransform.set(new Matrix4());

			if (n.parts.size > 0) {
				for (int i = 0; i < n.parts.size; i++)
					n.parts.get(i).material = materials.get(i);
			}

		}

		if (armature != null) {
			armature.translation.set(0, 0, 0);
			armature.rotation.set(new Vector3(1, 0, 0), (float) Math.toRadians(90));
			armature.scale.set(1, 1, 1);
		}

		instances.add(m);

		return m;

	}

	public String toString(){
		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());
	}

	// Also UV, Normal, and Transforms (and possibly "do this to all" versions that don't use transforms?)

}
