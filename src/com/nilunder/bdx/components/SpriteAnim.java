package com.nilunder.bdx.components;

import java.util.*;

import javax.vecmath.*;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.utils.*;

import com.bulletphysics.linearmath.MatrixUtil;

import com.nilunder.bdx.*;
import com.nilunder.bdx.utils.Timer;


public class SpriteAnim extends Component {

	private static class Frame extends ArrayList<Vector3f>{
		
		public Frame(){
			for (int i = 0; i < 4; ++i){
				add(new Vector3f(0, 0, 1));
			}
		}

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

		public Frame moved(float dx, float dy){
			Frame copy = new Frame(this);
			for (Vector3f v : copy){
				v.x += dx;
				v.y += dy;
			}
			return copy;
		}
	}

	private static class Animation extends ArrayList<Frame>{
		public static float defaultFps = 12;

		public float fps;
		public boolean loop;
		public String name;

		private Iterator<Frame> iter;
		private Frame lastFrame;
		private Frame currentFrame;

		public Animation(String name, ArrayList<Frame> frames){
			addAll(frames);
			reset();
			init(name);
		}
		
		public Animation(String name, JsonValue frames){
			for (JsonValue frame : frames){
				add(new Frame(frame));
			}
			reset();
			init(name);
		}

		public Frame nextFrame(){
			if (!iter.hasNext()){
				if (loop)
					iter = iterator();
				else
					return lastFrame;
			}
			currentFrame = iter.next();
			return currentFrame;
		}

		public boolean onLastFrame(){
			return currentFrame == lastFrame;
		}

		public void reset(){
			iter = iterator();
		}

		private void init(String name){
			this.name = name;
			fps = defaultFps;
			loop = true;
			lastFrame = get(size() - 1);
		}
	}
	
	public float speed;

	private HashMap<String, Animation> animations;
	private Animation active;
	private Timer tick;
	private Matrix3f uvTransform;
	private Frame baseFrame;
	private boolean rowBased;
	private int frameWidth;
	private int frameHeight;
	
	public SpriteAnim(GameObject g){
		super(g);
		animations = new HashMap<String, Animation>();
		tick = new Timer();
		uvTransform = new Matrix3f();
		uvTransform.setIdentity();
		speed = 1;
		state = play;
	}

	public SpriteAnim(GameObject g, FileHandle sprycle){
		this(g);
		importAnimations(sprycle);
		play(animations.keySet().iterator().next());
	}

	public SpriteAnim(GameObject g, int frameWidth, int frameHeight){
		this(g, frameWidth, frameHeight, true);
	}

	public SpriteAnim(GameObject g, int frameWidth, int frameHeight, boolean rowBased){
		this(g);
		this.frameWidth = frameWidth;
		this.frameHeight = frameHeight;
		this.rowBased = rowBased;
		baseFrame = frame();
	}

	public void add(String name, int index, int[] frames){
		add(name, index, frames, Animation.defaultFps, true);
	}

	public void add(String name, int index, int[] frameIndices, float fps, boolean loop){
		TextureAttribute ta = (TextureAttribute)g.modelInstance.materials.get(0).get(TextureAttribute.Diffuse);
		GLTexture t = ta.textureDescription.texture;
		float u = 1f / t.getWidth();
		float v = 1f / t.getHeight();

		ArrayList<Frame> frames = new ArrayList<Frame>();
		for (int i : frameIndices){
			if (rowBased)
				frames.add(baseFrame.moved(u * i * frameWidth, v * index * frameHeight));
			else
				frames.add(baseFrame.moved(u * index * frameWidth, v * i * frameHeight));
		}

		Animation anim = new Animation(name, frames);
		anim.fps = fps;
		anim.loop = loop;
		animations.put(name, anim);
	}

	public boolean onLastFrame(){
		return active.onLastFrame();
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
	
	public void play(String name){
		active = animations.get(name);
		if (!active.loop && active.onLastFrame())
			active.reset();
	}

	public String current(){
		return active == null ? "BDX_NONE" : active.name;
	}
	
	public void showNextFrame(){
		if (active == null)
			return;

		frame(transformed(active.nextFrame()));
	}
	
	private State play = new State(){
		private float nz(float n){
			return n <= 0 ? Float.MIN_VALUE : n;
		}

		public void main(){
			if (active == null)
				return;

			active.fps = nz(active.fps);
			speed = nz(speed);

			tick.delta(1f/active.fps / speed);

			if (tick.time()){
				showNextFrame();
			}
		}
	};

	private void importAnimations(FileHandle sprycle){
		JsonValue root = new JsonReader().parse(sprycle);
		for (JsonValue anim : root){
			animations.put(anim.name, new Animation(anim.name, anim));
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
		_frame(frame, true);
	}

	private Frame frame(){
		Frame f = new Frame();
		_frame(f, false);
		return f;
	}

	private void _frame(Frame frame, boolean set){
		Mesh mesh = g.modelInstance.model.meshes.first();
		
		int start = 0;
		int count = mesh.getNumVertices();
		
		VertexAttribute posAttr = mesh.getVertexAttribute(VertexAttributes.Usage.TextureCoordinates);
		int offset = posAttr.offset / 4;
		int vertexSize = mesh.getVertexSize() / 4;
		int numVertices = mesh.getNumVertices();

		float[] vertices = new float[numVertices * vertexSize];
		int[] tcIndices = new int[]{0, 1, 2, 2, 3, 0};

		mesh.getVertices(0, vertices.length, vertices);

		int idx = offset + (start * vertexSize);
		for (int i = 0; i < count; i++) {
			Vector3f uv = frame.get(tcIndices[i]);
			if (set){
				vertices[idx] = uv.x;
				vertices[idx + 1] = uv.y;
			}else{
				uv.x = vertices[idx];
				uv.y = vertices[idx + 1];
			}
			idx += vertexSize;
		}

		if (set)
			mesh.setVertices(vertices, 0, vertices.length);
		
	}

}
