package com.nilunder.bdx.components;

import java.util.ArrayList;
import java.util.HashMap;

import javax.vecmath.Vector2f;
import javax.vecmath.Matrix3f;

import com.badlogic.gdx.graphics.GLTexture;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;

import com.nilunder.bdx.*;
import com.nilunder.bdx.utils.Timer;


public class SpriteAnim extends Component<GameObject> {

	public static class Animation extends ArrayList<Vector2f>{
		public String name;
		public float fps;
		public boolean looping;

		public int playHead;
		public int playDir;

		public Animation(String name, float fps, boolean looping){
			this.name = name;
			this.fps = fps;
			this.looping = looping;
			playHead = 0;
			playDir = 1;
		}

		public Vector2f nextFrame(){
			if (onLastFrame()){
				if (looping)
					reset();
				else
					playHead -= playDir;
			}

			Vector2f frame = get(playHead); 

			playHead += playDir;

			return frame;
		}

		public boolean onLastFrame(){
			return playHead == size() || playHead == -1;
		}

		public void reset(){
			if (playDir > 0)
				playHead = 0;
			else
				playHead = size() - 1;
		}

	}
	
	public float speed;
	public HashMap<String, Animation> animations;
	public Animation active;

	private int prevFrame;
	private Timer ticker;
	private Matrix3f uvScale;
	private boolean rowBased;
	private Vector2f baseFrame;
	private Vector2f frameDim;
	
	public SpriteAnim(GameObject g, int frameWidth, int frameHeight, boolean rowBased, boolean uniqueModel){
		super(g);
		if (uniqueModel)
			g.mesh(g.mesh().copy());
		this.rowBased = rowBased;
		animations = new HashMap<String, Animation>();
		ticker = new Timer();
		uvScale = Matrix3f.identity();
		speed = 1;
		state = play;

		// initially displayed frame
		HashMap<Model,Vector2f> modelToFrame = g.scene.modelToFrame;

		baseFrame = modelToFrame.get(g.modelInstance.model);
		if (baseFrame == null){
			baseFrame = uvFrame();
			modelToFrame.put(g.modelInstance.model, baseFrame);
		}

		// frameDim
		TextureAttribute ta = (TextureAttribute)g.modelInstance.materials.first().get(TextureAttribute.Diffuse);
		GLTexture t = ta.textureDescription.texture;
		float u = 1f / t.getWidth();
		float v = 1f / t.getHeight();

		frameDim = new Vector2f(u * frameWidth, v * frameHeight);
	}

	public SpriteAnim(GameObject g, int frameWidth, int frameHeight){
		this(g, frameWidth, frameHeight, true, true);
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

	public ArrayList<String> animationNames(){
		return new ArrayList<String>(animations.keySet());
	}

	public float uvScaleX(){
		return uvScale.getM00();
	}
	
	public float uvScaleY(){
		return uvScale.getM11();
	}
	
	public void uvScaleX(float s){
		uvScale(s, uvScaleY());
	}
	
	public void uvScaleY(float s){
		uvScale(uvScaleX(), s);
	}
	
	private void uvScale(float x, float y){
		if (uvScaleX() == x && uvScaleY() == y)
			return;
			
		uvScale.invert();
		uvScale.mul(Matrix3f.scale(x, y));
		
		Vector2f df = uvFrame();
		Matrix3f t = Matrix3f.position(df.x, df.y);
		Matrix3f tInv = t.inverted();
		t.mul(uvScale);
		t.mul(tInv);
		
		g.mesh().transformUV(t);
	}
	
	public void play(String name){
		Animation next = animations.get(name);

		if (active != next){
			active = next;
			active.playDir = speed * active.fps < 0 ? -1 : 1;
			active.reset();
			ticker.done(true); // immediate play
		}

		if (!active.looping && active.onLastFrame()){
			active.playDir = speed * active.fps < 0 ? -1 : 1;
			active.reset();
			ticker.done(true);
		}

	}

	public void showNextFrame(){
		active.playDir = speed * active.fps < 0 ? -1 : 1;
		uvFrame(active.nextFrame());
	}

	public void frame(int frame){
		active.playHead = frame; // Set the frame, and
		ticker.done(true); // Update the sprite immediately
	}

	public int frame(){
		return active.playHead - active.playDir;
	}

	public boolean frameChanged(){
		return prevFrame != frame();
	}

	private State play = new State(){
		private float nz(float n){
			return n <= 0 ? 0.000001f : n;
		}

		public void main(){
			if (active == null)
				return;

			prevFrame = frame();

			ticker.interval = 1f / nz(Math.abs(active.fps) * Math.abs(speed));

			if (ticker.tick()){
				showNextFrame();
			}

		}
	};

	private void uvFrame(Vector2f frame){
		Vector2f df = uvFrame();
		Matrix3f t = Matrix3f.position(frame.x - df.x, frame.y - df.y);
		g.mesh().transformUV(t);
	}

	private Vector2f uvFrame(){
		Vector2f frame = new Vector2f();
		
		float[] verts = g.mesh().vertices();
		int numIndices = g.mesh().numIndices();
		for (int i = 0; i < numIndices; i++){
			int offset = i * Bdx.VERT_STRIDE;
			frame.x += verts[offset + 6];
			frame.y += verts[offset + 7];
		}
		frame.x /= numIndices;
		frame.y /= numIndices;
		
		return frame;
	}

}
