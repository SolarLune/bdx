package com.nilunder.bdx.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.bulletphysics.linearmath.MatrixUtil;
import com.nilunder.bdx.Component;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.State;
import com.nilunder.bdx.utils.Timer;


public class TexAnim extends Component {

	private class Frame extends ArrayList<Vector3f>{
		
		public Frame(JsonValue frame){
			for (JsonValue arr : frame){
				float[] a = arr.asFloatArray(); 
				add(new Vector3f(a[0], a[1], 1));
			}
		}
		
		public Frame(Frame frame){
			for (Vector3f v : frame){
				add(new Vector3f(v));
			}
		}
	}

	private class Sequence extends ArrayList<Frame>{
		Iterator<Frame> iter;
		
		public Sequence(JsonValue sequence){
			for (JsonValue frame : sequence){
				add(new Frame(frame));
			}
			iter = iterator();
		}
		
		public Frame nextFrame(){
			Frame frame = iter.next();
			if (!iter.hasNext())
				iter = iterator();
			return frame;
		}
	}
	
	public float fps;
	
	private HashMap<String, Sequence> sequences;
	private Sequence activeSequence;
	private Timer tick;
	private Matrix3f uvTransform;
	
	public TexAnim(GameObject g, FileHandle sequences){
		super(g);
		importSequences(sequences);
		sequence(this.sequences.keySet().iterator().next());
		tick = new Timer();
		uvTransform = new Matrix3f();
		uvTransform.setIdentity();
		fps(12);
		state = play;
	}
	
	public void flipX(){
		MatrixUtil.scale(uvTransform, uvTransform, new Vector3f(-1, 1, 1));
	}
	
	public void flipY(){
		MatrixUtil.scale(uvTransform, uvTransform, new Vector3f(1, -1, 1));
	}
	
	public boolean isFlippedX(){
		return uvTransform.m00 < 0;
	}
	
	public boolean isFlippedY(){
		return uvTransform.m11 < 0;
	}
	
	public void fps(float fps){
		this.fps = fps;
		tick.delta(1f/Math.abs(fps));
	}
	
	public void sequence(String name){
		activeSequence = sequences.get(name);
	}
	
	public void showNextFrame(){
		frame(transformed(activeSequence.nextFrame()));
	}
	
	private State play = new State(){
		public void main(){
			if (tick.time()){
				showNextFrame();
			}
		}
	};
	
	private void importSequences(FileHandle sequences){
		this.sequences = new HashMap<String, Sequence>();
		JsonValue root = new JsonReader().parse(sequences);
		for (JsonValue seq : root){
			this.sequences.put(seq.name, new Sequence(seq));
		}
	}
	
	private Frame transformed(Frame frame){
		Frame f = new Frame(frame);
		
		Vector3f sum = new Vector3f();
		for (Vector3f v : f){
			sum.add(v);
		}
	
		sum.scale(0.25f);
		sum.z = 1;
		
		Matrix3f fromOrigin = new Matrix3f();
		fromOrigin.setIdentity();
		fromOrigin.setColumn(2, sum);
		
		Matrix3f toOrigin = new Matrix3f(fromOrigin);
		toOrigin.invert();
		
		Matrix3f t = new Matrix3f(fromOrigin);
		t.mul(uvTransform);
		t.mul(toOrigin);
		
		for (Vector3f v : f){
			t.transform(v);
		}

		return f;
	}

	private void frame(Frame frame){
		Mesh mesh = g.modelInstance.model.meshes.first();
		
		int start = 0;
		int count = mesh.getNumVertices();
		
		VertexAttribute posAttr = mesh.getVertexAttribute(Usage.TextureCoordinates);
		int offset = posAttr.offset / 4;
		int vertexSize = mesh.getVertexSize() / 4;
		int numVertices = mesh.getNumVertices();

		float[] vertices = new float[numVertices * vertexSize];
		int[] tcIndices = new int[]{0, 1, 2, 2, 3, 0};

		mesh.getVertices(0, vertices.length, vertices);

		int idx = offset + (start * vertexSize);
		for (int i = 0; i < count; i++) {
			Vector3f uv = frame.get(tcIndices[i]);
			vertices[idx] = uv.x;
			vertices[idx + 1] = uv.y;
			idx += vertexSize;
		}
		
		mesh.setVertices(vertices, 0, vertices.length);

	}

}
