package com.nilunder.bdx;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.graphics.*;

import javax.vecmath.Vector3f;

public class Text extends GameObject{

	public enum Alignment {
		LEFT,
		CENTER,
		RIGHT
	}

	private String text = "";
	private Alignment alignment;
	private Alignment setAlignment;

	public JsonValue font;
	public int capacity;
	private float lineHeight = 1.0f;
	Vector3f originalScale = null;
	Vector3f min;
	Vector3f max;

	public void text(String txt) {
		// Reform quads according to Angel Code font format
		Mesh mesh = modelInstance.model.meshes.first();
		int vertexSize = mesh.getVertexSize() / 4;
		int numVertices = mesh.getNumVertices();
		float[] verts = new float[numVertices * vertexSize];
		int vi = 0;

		int capacity = (numVertices / 3) / 2; // number of quads

		String target = txt.substring(0, Math.min(txt.length(), capacity));

		if (text.equals(target) && alignment == setAlignment)
			return;

		text = target;
		setAlignment = alignment;

		JsonValue cm = font.get("common");
		float su = 1.f / cm.get("scaleW").asInt();
		float sv = 1.f / cm.get("scaleH").asInt();

		JsonValue char_data = font.get("char");

		JsonValue at_c = char_data.get(Integer.toString('O'));
		float scale = 0.0225f;

		int posX = 0;
		int posY = at_c.get("height").asInt() + at_c.get("yoffset").asInt();
		float z = 0;
		int totalWidth = 0;

		String[] lines = text.split("[\n]");

		int cap = 0;
		int past_vi = 0;

		for (String l : lines) {

			for (int i = 0; i < Math.min(l.length(), capacity - cap); ++i) {		// Write chars for the line or text object capacity, whichever's shorter

				char chr = ' ';
				if (i < l.length())
					chr = l.charAt(i);

				JsonValue c = char_data.get(Integer.toString(chr));
				if (c == null)
					c = char_data.get(Integer.toString(' '));

				int x = posX + c.get("xoffset").asInt();
				int y = posY - c.get("yoffset").asInt();
				int w = c.get("width").asInt();
				int h = c.get("height").asInt();
				posX += c.get("xadvance").asInt();

				if (i < l.length() && x + w > totalWidth)
					totalWidth = x + w;

				float u = c.get("x").asInt();
				float v = c.get("y").asInt();

				float[][] quad = {
						{x, y - h, z, 0, 0, 1, u, v + h},
						{x + w, y - h, z, 0, 0, 1, u + w, v + h},
						{x + w, y, z, 0, 0, 1, u + w, v},
						{x + w, y, z, 0, 0, 1, u + w, v},
						{x, y, z, 0, 0, 1, u, v},
						{x, y - h, z, 0, 0, 1, u, v + h}
				};

				z += 0.0001;

				for (float[] vert : quad) {
					vert[0] *= scale;
					vert[1] *= scale;
					vert[0] -= 0.05;
					vert[6] *= su;
					vert[7] *= sv;

					if (min == null)
						min = new Vector3f(vert[0], vert[1], 0);
					else
						min.set(Math.min(min.x, vert[0]), Math.min(min.y, vert[1]), 0);

					if (max == null)
						max = new Vector3f(vert[0], vert[1], 0);
					else
						max.set(Math.max(max.x, vert[0]), Math.max(max.y, vert[1]), 0);

					for (float f : vert)
						verts[vi++] = f;

				}

			}

			cap += l.length();
			posY -= (int) (cm.get("lineHeight").asInt() * this.lineHeight);		// Set up the Y for the next text line
			posX = 0;

			for (int i = past_vi; i < vi; i += Bdx.VERT_STRIDE){

				if (alignment == Alignment.CENTER)
					verts[i] -= (totalWidth / 2f) * scale;
				else if (alignment == Alignment.RIGHT)
					verts[i] -= totalWidth * scale;

			}

			past_vi = vi;
			totalWidth = 0;

		}

		mesh.setVertices(verts, 0, verts.length);
	}

	public void applyScalingDifference() {

		Vector3f scaleDiff = new Vector3f();

		scaleDiff.sub(min);
		scaleDiff.add(max);

		if (scaleDiff.x == 0)
			scaleDiff.x = 1;

		if (scaleDiff.y == 0)
			scaleDiff.y = 1;

		if (scaleDiff.z == 0)
			scaleDiff.z = 1;

		Vector3f ns = dimensionsNoScale.div(scaleDiff);

		scaleDiff.set(ns.x, ns.y, scaleDiff.z);

		originalScale = scale();

		scale(originalScale.mul(scaleDiff));

	}

	public void resetScalingDifference() {
		scale(originalScale);
		originalScale = null;
	}

	public String text(){
		return text;
	}

	public void alignment(Alignment alignment){
		this.alignment = alignment;
		text(text());
	}

	public Alignment alignment(){
		return alignment;
	}

	public void lineHeight(float lineHeight){
		this.lineHeight = lineHeight;
		text(text());
	}

	public float lineHeight(){
		return this.lineHeight;
	}

}
