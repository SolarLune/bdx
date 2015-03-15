package com.nilunder.bdx.components;

import com.nilunder.bdx.Component;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.State;

import javax.vecmath.Vector3f;

public class Track extends Component {
	public GameObject target;
	public Vector3f offset;
	public String axis;
	public String upAxis;
	
	private Vector3f upVec;
	
	public Track(GameObject g, GameObject target){
		super(g);
		this.target = target;
		offset = new Vector3f();
		axis = "Y";
		upAxis = "Z";
		upVec = new Vector3f(0, 0, 1);
		state = track;
	}
	
	private State track = new State(){
		public void main(){
			if (target == null || !target.valid()){
				target = null;
				return;
			}
			Vector3f vec = target.position().plus(offset).minus(g.position());
			g.alignAxisToVec(upAxis, upVec);
			g.alignAxisToVec(axis, vec);
		}
	};
}
