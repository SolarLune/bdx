package com.nilunder.bdx.gl;

import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.IntAttribute;
import com.badlogic.gdx.utils.Array;
import com.nilunder.bdx.Scene;
import com.nilunder.bdx.utils.ArrayListNamed;
import com.nilunder.bdx.utils.Color;
import com.nilunder.bdx.utils.Named;

public class Material extends com.badlogic.gdx.graphics.g3d.Material implements Named {

	public Material() {
		super();
	}

	public Material(String id) {
		super(id);
	}

	public Material(Attribute... attributes) {
		super(attributes);
	}

	public Material(String id, Attribute... attributes) {
		super(id, attributes);
	}

	public Material(Array<Attribute> attributes) {
		super(attributes);
	}

	public Material(String id, Array<Attribute> attributes) {
		super(id, attributes);
	}

	public Material(Material copyFrom) {
		super(copyFrom);
	}

	public Material(com.badlogic.gdx.graphics.g3d.Material copyFrom){
		super(copyFrom);
	}

	public Material(String id, Material copyFrom) {
		super(id, copyFrom);
	}

	public Color color(){
		ColorAttribute ca = (ColorAttribute) get(ColorAttribute.Diffuse);
		return new Color(ca.color);
	}

	public void color(Color color){
		ColorAttribute ca = (ColorAttribute) get(ColorAttribute.Diffuse);
		ca.color.set(color);
		if (get(BlendingAttribute.Type) != null) {
			BlendingAttribute ba = (BlendingAttribute) get(BlendingAttribute.Type);
			ba.opacity = color.a;
		}
	}

	public Color tint(){
		ColorAttribute ta = (ColorAttribute) get(Scene.BDXColorAttribute.Tint);
		return new Color(ta.color);
	}

	public void tint(Color color){
		ColorAttribute ta = (ColorAttribute) get(Scene.BDXColorAttribute.Tint);
		ta.color.set(color);
	}

	public int[] blendMode(){
		BlendingAttribute ba = (BlendingAttribute) get(BlendingAttribute.Type);
		return new int[]{ba.sourceFunction, ba.destFunction};
	}

	public void blendMode(int src, int dest){
		BlendingAttribute ba = (BlendingAttribute) get(BlendingAttribute.Type);
		ba.sourceFunction = src;
		ba.destFunction = dest;
	}

	public boolean shadeless(){
		IntAttribute sa = (IntAttribute) get(Scene.BDXIntAttribute.Shadeless);
		return sa.value == 1;
	}

	public void shadeless(boolean shadeless){
		IntAttribute sa = (IntAttribute) get(Scene.BDXIntAttribute.Shadeless);
		sa.value = shadeless ? 1 : 0;
	}

	public String name(){
		return id;
	}

	public String toString(){
		return id + " <" + getClass().getName() + "> @" + Integer.toHexString(hashCode());
	}

}
