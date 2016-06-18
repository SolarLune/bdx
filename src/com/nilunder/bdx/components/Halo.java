package com.nilunder.bdx.components;

import com.nilunder.bdx.Component;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.State;
import com.nilunder.bdx.gl.Viewport;

public class Halo extends Component<GameObject> {
	
	public Viewport viewport; 

	public Halo(GameObject g, Viewport viewport){
		super(g);
		this.viewport = viewport;
		state = track;
		state.main();
	}
	
	private State track = new State(){
		public void main(){
			g.orientation(viewport.orientation());
		}
	};
}
