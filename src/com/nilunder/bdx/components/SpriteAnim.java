package com.nilunder.bdx.components;

import java.util.*;

import javax.vecmath.*;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.math.*;

import com.bulletphysics.linearmath.MatrixUtil;

import com.nilunder.bdx.*;
import com.nilunder.bdx.utils.Timer;


public class SpriteAnim extends Component {

	private static class Animation extends ArrayList<Vector2f>{
		public String name;
		public float fps;
		public boolean looping;

		private int playHead;

		public Animation(String name, float fps, boolean looping){
			this.name = name;
			this.fps = fps;
			this.looping = looping;
			playHead = 0;
		}

		public Vector2f nextFrame(){
			if (onLastFrame()){
				if (looping)
					reset();
				else
					--playHead;
			}

			return get(playHead++);

		}

		public boolean onLastFrame(){
			return playHead == size();
		}

		public void reset(){
			playHead = 0;
		}

	}
	
	public float speed;

	private HashMap<String, Animation> animations;
	private Animation active;
	private Timer tick;
	private Matrix3 uvScale;
	private boolean rowBased;
	private Vector2f baseFrame;
	private Vector2f displayedFrame;
	private Vector2f frameDim;
	
	public SpriteAnim(GameObject g, int frameWidth, int frameHeight, boolean rowBased){
		super(g);
		this.rowBased = rowBased;
		animations = new HashMap<String, Animation>();
		tick = new Timer();
		uvScale = new Matrix3();
		uvScale.idt();
		speed = 1;
		state = play;

		baseFrame = frame();
		displayedFrame = baseFrame;

		// frameDim
		TextureAttribute ta = (TextureAttribute)g.modelInstance.materials.get(0).get(TextureAttribute.Diffuse);
		GLTexture t = ta.textureDescription.texture;
		float u = 1f / t.getWidth();
		float v = 1f / t.getHeight();

		frameDim = new Vector2f(u * frameWidth, v * frameHeight);
	}

	public SpriteAnim(GameObject g, int frameWidth, int frameHeight){
		this(g, frameWidth, frameHeight, true);
	}

	public void add(String name, int index, int[] frames){
		add(name, index, frames, 12, true);
	}

	public void add(String name, int sequence, int[] frames, float fps, boolean looping){
		Animation anim = new Animation(name, fps, looping);

		for (int i : frames){
			Vector2f f = new Vector2f(baseFrame);
			if (rowBased){
				f.x += i * frameDim.x;
				f.y += sequence * frameDim.y;
			}else{
				f.x += sequence * frameDim.x;
				f.y += i * frameDim.y;
			}
			anim.add(f);
		}

		animations.put(name, anim);
	}

	public boolean onLastFrame(){
		return active.onLastFrame();
	}

	public void uvScaleX(float s){
		uvScale(s, uvScaleY());
	}

	public void uvScaleY(float s){
		uvScale(uvScaleX(), s);
	}

	public float uvScaleX(){
		return uvScale.val[Matrix3.M00];
	}
	
	public float uvScaleY(){
		return uvScale.val[Matrix3.M11];
	}
	
	public void play(String name){
		Animation next = animations.get(name);

		if (active != next){
			active = next;
			tick.timeLast = 0; // immediate play
		}

		if (!active.looping && onLastFrame()){
			active.reset();
			tick.timeLast = 0;
		}

	}

	public String current(){
		return active == null ? "BDX_NONE" : active.name;
	}
	
	public void showNextVector2f(){
		if (active == null)
			return;

		frame(active.nextFrame());
	}
	
	private State play = new State(){
		private float nz(float n){
			return n <= 0 ? Float.MIN_VALUE : n;
		}

		public void main(){
			if (active == null)
				return;

			active.fps = Math.abs(active.fps);
			speed = Math.abs(speed);

			tick.delta(1f / nz(active.fps * speed));

			if (tick.time()){
				showNextVector2f();
			}
		}
	};

	private void frame(Vector2f frame){
		Matrix3 trans = new Matrix3();
		Vector2f df = displayedFrame;
		trans.setToTranslation(frame.x - df.x, frame.y - df.y);
		
		Mesh mesh = g.modelInstance.model.meshes.first();
		mesh.transformUV(trans);

		displayedFrame = frame;
	}

	private Vector2f frame(){
		Mesh mesh = g.modelInstance.model.meshes.first();
		int n = mesh.getNumVertices();
		float[] verts = new float[n*5];
		mesh.getVertices(0, verts.length, verts);

		Vector2f frame = new Vector2f(0, 0);

		int uvStart = 3;
		for (int v = 0; v < n; ++v){
			int i = v * 5;
			frame.x += verts[i + uvStart];
			frame.y += verts[i + uvStart + 1];
		}

		frame.x /= n;
		frame.y /= n;
		
		return frame;
	}

	private void scaleUV(Matrix3 scale){
		Matrix3 trans = new Matrix3(); trans.idt();
		Vector2f df = displayedFrame;
		trans.setToTranslation(df.x, df.y);

		Matrix3 toOrigin = new Matrix3(trans);
		toOrigin.inv();

		trans.mul(scale);
		trans.mul(toOrigin);

		Mesh mesh = g.modelInstance.model.meshes.first();
		mesh.transformUV(trans);
	}

	private void uvScale(float x, float y){
		if (uvScaleX() == x && uvScaleY() == y)
			return;
		
		// back to unit scale
		uvScale.inv();
		scaleUV(uvScale);

		// set new scale
		uvScale.idt();
		uvScale.scale(x, y);
		scaleUV(uvScale);
	}
}
