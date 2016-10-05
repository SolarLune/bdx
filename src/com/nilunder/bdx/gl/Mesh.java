package com.nilunder.bdx.gl;

import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.model.NodePart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonReader;
import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.utils.ArrayListNamed;
import com.nilunder.bdx.utils.Color;
import com.nilunder.bdx.utils.Named;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;

public class Mesh implements Named {

	public Model model;
	public String name;
	public Scene scene;
	public ArrayListMaterials materials;
	public ArrayList<ModelInstance> instances;
	public boolean defaultMesh = false;

	public class ArrayListMaterials extends ArrayListNamed<Material> {

		public Material set(int index, Material material) {
			Material mat = material;
			if (Bdx.defaultMaterialCopy)
				mat = new Material(mat);
			model.nodes.get(0).parts.get(index).material = mat;
			for (ModelInstance m : instances)
				m.nodes.get(0).parts.get(index).material = mat;
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

	public Mesh(Model model, Scene scene, String name){
		this.model = model;
		this.name = name;
		this.scene = scene;
		materials = new ArrayListMaterials();
		for (NodePart part : model.nodes.get(0).parts)
			materials.add((Material) part.material);
		instances = new ArrayList<ModelInstance>();
	}

	public Mesh(Model model, Scene scene){
		this(model, scene, model.meshParts.first().id);
	}

	protected void finalize() throws Throwable {
		model.dispose();
	}

    public int getVertexCount(int materialSlot){
        return model.nodes.first().parts.get(materialSlot).meshPart.size;
    }

    public int getVertexCount(){
        int count = 0;
        for (int i = 0; i < model.nodes.get(0).parts.size; i++)
            count += getVertexCount(i);
        return count;
    }

    public void vertPos(int materialSlot, int index, float x, float y, float z){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[getVertexCount() * Bdx.VERT_STRIDE];
        mesh.getVertices(verts);
        int i = (model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE);
        verts[i] = x;
        verts[i+1] = y;
        verts[i+2] = z;
        mesh.setVertices(verts);
    }

    public void vertPos(int materialSlot, int index, Vector3f pos){
        vertPos(materialSlot, index, pos.x, pos.y, pos.z);
    }

    public Vector3f vertPos(int materialSlot, int index){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[3];
        mesh.getVertices((model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE), 3, verts);
        return new Vector3f(verts[0], verts[1], verts[2]);
    }

    public void vertNor(int materialSlot, int index, float x, float y, float z){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[getVertexCount() * Bdx.VERT_STRIDE];
        mesh.getVertices(verts);
        int i = (model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE);
        verts[i+3] = x;
        verts[i+4] = y;
        verts[i+5] = z;
        mesh.setVertices(verts);
    }

    public void vertNor(int materialSlot, int index, Vector3f pos){
        vertNor(materialSlot, index, pos.x, pos.y, pos.z);
    }

    public Vector3f vertNor(int materialSlot, int index){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[3];
        mesh.getVertices((model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE) + 3, 3, verts);
        return new Vector3f(verts[0], verts[1], verts[2]);
    }

    public void vertUV(int materialSlot, int index, float u, float v){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[getVertexCount() * Bdx.VERT_STRIDE];
        mesh.getVertices(verts);
        int i = (model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE);
        verts[i+6] = u;
        verts[i+7] = v;
        mesh.setVertices(verts);
    }

    public void vertUV(int materialSlot, int index, Vector2f uv){
        vertUV(materialSlot, index, uv.x, uv.y);
    }

    public Vector2f vertUV(int materialSlot, int index){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[3];
        mesh.getVertices((model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE) + 6, 2, verts);
        return new Vector2f(verts[0], verts[1]);
    }

    public void vertTransform(int materialSlot, Matrix4f matrix){
        Matrix4f m = matrix;
        float[] vals = {m.m00, m.m10, m.m20, m.m30,
                        m.m01, m.m11, m.m21, m.m31,
                        m.m02, m.m12, m.m22, m.m32,
                        m.m03, m.m13, m.m23, m.m33};
        model.meshes.first().transform(new Matrix4(vals), model.meshParts.get(materialSlot).offset, model.meshParts.get(materialSlot).size);
    }

    public void vertTransformUV(int materialSlot, Matrix3f matrix){
        Matrix3f m = matrix;
        float[] vals = {m.m00, m.m10, m.m20,
                m.m01, m.m11, m.m21,
                m.m02, m.m12, m.m22};
        float[] verts = new float[getVertexCount() * Bdx.VERT_STRIDE];
        model.meshes.first().getVertices(verts);
        MeshPart mp = model.meshParts.get(materialSlot);
        com.badlogic.gdx.graphics.Mesh.transformUV(new Matrix3(vals), verts, Bdx.VERT_STRIDE, 6, mp.offset, mp.size);
        model.meshes.first().setVertices(verts);
    }

	public String serialized() {

		HashMap<String, Float[]> out = new HashMap<String, Float[]>();
		Float[] d;

		for (int i = 0; i < materials.size(); i++) {
			d = new Float[getVertexCount(i) * Bdx.VERT_STRIDE];
			for (int v = 0; v < getVertexCount(i); v++) {
				int vi = v * 8;
				Vector3f p = vertPos(i, v);
				d[vi] = p.x;
				d[vi + 1] = p.y;
				d[vi + 2] = p.z;
				p = vertNor(i, v);
				d[vi + 3] = p.x;
				d[vi + 4] = p.y;
				d[vi + 5] = p.z;
				Vector2f u = vertUV(i, v);
				d[vi + 6] = u.x;
				d[vi + 7] = u.y;
			}
			out.put(materials.get(i).name(), d);
		}

		return new Json().toJson(out);
	}

	public String name(){
		return name;
	}

	public Mesh copy(String newName){
		
		Model uniqueModel;
		Mesh newMesh;
				
		if (!defaultMesh)
			uniqueModel = scene.createModel(new JsonReader().parse(serialized()));
		else
			uniqueModel = new ModelBuilder().createBox(1.0f, 1.0f, 1.0f, scene.defaultMaterial, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates);
		
		newMesh = new Mesh(uniqueModel, scene, newName);
		newMesh.materials.clear();
		newMesh.defaultMesh = defaultMesh;

		for (NodePart part : uniqueModel.nodes.get(0).parts) {
			Material newMat = new Material(part.material);
			newMesh.materials.add(newMat);
			part.material = newMat;
		}

		return newMesh;
	}

	public Mesh copy(){
		return copy(name);
	}

	public ModelInstance getInstance(){
		ModelInstance m = new ModelInstance(model);
		for (int i = 0; i < m.nodes.get(0).parts.size; i++)
			m.nodes.get(0).parts.get(i).material = materials.get(i);
		instances.add(m);
		return m;
	}

	public String toString(){
		return name + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());
	}

	// Also UV, Normal, and Transforms (and possibly "do this to all" versions that don't use transforms?)

}
