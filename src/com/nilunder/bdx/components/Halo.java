package com.nilunder.bdx.components;

import com.nilunder.bdx.Component;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.State;

public class Halo extends Component<GameObject> {

	public Halo(GameObject g){
		super(g);
		state = track;
		state.main();
	}
	
	private State track = new State(){
		public void main(){
			g.orientation(g.scene.camera.orientation());
		}
	};
}
