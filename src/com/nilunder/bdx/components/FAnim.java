package com.nilunder.bdx.components;

import java.util.*;
import javax.vecmath.*;

import com.badlogic.gdx.utils.JsonValue;
import com.bulletphysics.linearmath.MatrixUtil;

import com.nilunder.bdx.*;

public class FAnim extends Component<GameObject>{

	private static class KeyFrame{
		public Vector2f handle_left;
		public Vector2f position;
		public Vector2f handle_right;

		public KeyFrame(JsonValue frame){
			JsonValue p = frame.child;
			handle_left = new Vector2f(p.asFloatArray());
			position = new Vector2f(p.next.asFloatArray());
			handle_right = new Vector2f(p.next.next.asFloatArray());
		}
	}

	private static class Channel extends ArrayList<KeyFrame>{

		public Channel(JsonValue channel){
			for (JsonValue frame : channel){
				this.add(new KeyFrame(frame));
			}
		}
	}

	private static class Action extends HashMap<Integer, Channel>{

		public Action(JsonValue action){
			for (JsonValue channel : action){
				put(Integer.parseInt(channel.name), new Channel(channel));
			}
		}
	}

	private static HashMap<String, Action> actions;

	public static void loadActions(JsonValue actions){
		FAnim.actions = new HashMap<String, Action>();
		for (JsonValue act : actions){
			FAnim.actions.put(act.name, new Action(act));
		}
	}

	public int loop;
	public boolean bounce;

	private Action action;
	private float frame;
	private float firstFrame;
	private float lastFrame;
	private float increment;
	private float[] offsetLocRotScale;
	private boolean additive;

	public FAnim(GameObject g){
		super(g);
		fps(24);
		additive(true);
	}

	public void fps(float fps){
		increment = fps / 60.f;
	}

	public void action(String name){
		bounce = false;

		action = actions.get(name);
		firstFrame = firstFrame();
		lastFrame = lastFrame();
		frame = firstFrame;
		loop = 0;
	}

	private State play = new State(){
		public int wall;

		public void main(){
			showFrame(frame);

			if (wall != 0){
				if (bounce)
					direction(-wall);

				if (loop == 0){
					stop();
				}else{
					--loop;
					if (!bounce)
						frame = wall == 1 ? firstFrame : lastFrame;
				}

				wall = 0;
			}

			wall = advance();
		}

	};

	private void direction(int dir){
		increment = Math.abs(increment) * dir;
	}

	private int advance(){
		frame += increment;
		int wall = 0;

		if (frame > lastFrame){
			frame = lastFrame;
			wall = 1;
		}else if (frame < firstFrame){
			frame = firstFrame;
			wall = -1;
		}

		return wall;
	}

	public FAnim play(){
		state = play;
		offsetLocRotScale = offsetLocRotScale();
		return this;
	}

	public FAnim loop(){
		return loop(Integer.MAX_VALUE);
	}

	public FAnim loop(int n){
		loop = n;
		return this;
	}

	public FAnim bounce(){
		bounce = true;
		return this;
	}

	public void stop(){
		frame = firstFrame;
		increment = Math.abs(increment);
		state = null;
	}

	public void pause(){
		state = null;
	}

	public void frame(float frame){
		this.frame = frame;
		showFrame(frame);
	}

	public float frame(){
		return frame;
	}

	public boolean isPlaying() {
		return state != null;
	}

	public void additive(boolean add){
		additive = add;
	}

	public boolean additive() {
		return additive;
	}

	private float firstFrame(){
		Channel ch = action.entrySet().iterator().next().getValue();
		return ch.get(0).position.x;
	}

	private float lastFrame(){
		Channel ch = action.entrySet().iterator().next().getValue();
		return ch.get(ch.size() - 1).position.x;
	}

	private void showFrame(float frame){
		float[] lrs = locRotScale();

		for (Map.Entry<Integer,Channel> e : action.entrySet()){
			int i = e.getKey();
			if (additive)
				lrs[i] = channelValue(e.getValue()) + offsetLocRotScale[i];
			else
				lrs[i] = channelValue(e.getValue());
		}

		// Location
		g.position(lrs[0], lrs[1], lrs[2]);

		// Rotation
		Matrix3f ori = new Matrix3f();
		MatrixUtil.setEulerZYX(ori, lrs[3], lrs[4], lrs[5]);
		g.orientation(ori);

		// Scale
		g.scale(lrs[6], lrs[7], lrs[8]);

	}

	private float channelValue(Channel channel){
		Iterator<KeyFrame> ahead = channel.iterator();
		KeyFrame fkf = ahead.next();

		if (frame < fkf.position.x)
			return fkf.position.y;

		for (KeyFrame kf : channel){
			KeyFrame kf_;

			if (ahead.hasNext())
				kf_ = ahead.next();
			else
				return kf.position.y;

			if (frame >= kf.position.x && frame < kf_.position.x){
				Vector2f range = kf_.position.minus(kf.position);
				float t = (frame - kf.position.x) / range.x;
				return bezier(kf.position, kf.handle_right, t, kf_.handle_left, kf_.position);
			}
		}

		return firstFrame;
	}

	private float bezier(Vector2f p0, Vector2f p1, float t, Vector2f p2, Vector2f p3){
		float u = 1.f - t;
		float tt = t*t;
		float uu = u*u;
		float uuu = uu * u;
		float ttt = tt * t;

		Vector2f p = p0.mul(uuu); //first term
		p.add(p1.mul(3.f * uu * t)); //second term
		p.add(p2.mul(3.f * u * tt)); //third term
		p.add(p3.mul(ttt)); //fourth term

		return p.y;
	}

	private float[] locRotScale(){
		Vector3f p = g.position();
		Vector3f r = g.orientation().euler();
		Vector3f s = g.scale();

		float[] lrs = new float[]{
			p.x, p.y, p.z,
			r.x, r.y, r.z,
			s.x, s.y, s.z
		};

		return lrs;
	}

	private float[] offsetLocRotScale(){
		float[] lrs = locRotScale();
		float[] offset = new float[lrs.length];
		for (Map.Entry<Integer,Channel> e : action.entrySet()){
			int i = e.getKey();
			offset[i] = lrs[i] - channelValue(e.getValue());
		}
		return offset;
	}
}
