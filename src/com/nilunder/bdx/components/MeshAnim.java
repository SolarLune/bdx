package com.nilunder.bdx.components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import com.nilunder.bdx.Component;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.State;
import com.nilunder.bdx.utils.Timer;

public class MeshAnim extends Component<GameObject> {

	public static class Animation extends ArrayList<String>{
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

		public String nextFrame(){
			if (onLastFrame()){
				if (looping)
					reset();
				else
					playHead -= playDir;
			}

			String frame = get(playHead);

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
	public String currentMesh;

	private int prevFrame;
	private Timer ticker;

	public MeshAnim(GameObject g){
		super(g);
		animations = new HashMap<String, Animation>();
		ticker = new Timer();
		speed = 1;
		state = play;
		currentMesh = g.mesh().name();
	}

	public void add(String name, String[] frames){
		add(name, frames, 12, true);
	}

	public void add(String name, String[] frames, float fps, boolean looping){
		Animation anim = new Animation(name, fps, looping);

		anim.addAll(Arrays.asList(frames));

		animations.put(name, anim);
	}

	public ArrayList<String> animationNames(){
		return new ArrayList<String>(animations.keySet());
	}

	public void play(String name){
		Animation next = animations.get(name);

		if (active != next){
			active = next;
			ticker.done(true); // immediate play
		}

		if (!active.looping && active.onLastFrame()){
			active.reset();
			ticker.done(true);
		}

	}

	public void showNextFrame(){
		active.playDir = speed * active.fps < 0 ? -1 : 1;

		String frame = active.nextFrame();

		if (!currentMesh.equals(frame)) {
			currentMesh = frame;
			g.mesh(currentMesh);
		}
	}

	public void frame(int frame){
		active.playHead = frame; // Set the frame, and
		ticker.done(true); // Update the mesh immediately
	}

	public int frame(){
		return active.playHead - active.playDir;
	}

	public boolean frameChanged() {
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

}