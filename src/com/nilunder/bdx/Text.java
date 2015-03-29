package com.nilunder.bdx;

import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.graphics.*;

public class Text extends GameObject{
	public String text;
	public JsonValue font;

	public void set(String txt){
		// Reform quads according to Angel Code font format
		Mesh mesh = modelInstance.model.meshes.first();
		int vertexSize = mesh.getVertexSize() / 4;
		int numVertices = mesh.getNumVertices();
		float[] verts = new float[numVertices * vertexSize];
		int vi = 0;

		int capacity = (numVertices / 3) / 2; // number of quads

		text = txt.substring(0, Math.min(txt.length(), capacity));

		JsonValue cm = font.get("common");
		float su = 1.f / cm.get("scaleW").asInt();
		float sv = 1.f / cm.get("scaleH").asInt();

		JsonValue char_data = font.get("char");

		JsonValue at_c = char_data.get(Integer.toString('O'));
		boolean builtin = font.get("info").get("face").asString().equals("Bfont");
		float scale = 0.0225f * (builtin ? 1.4f : 1f);
		float unit_height = at_c.get("height").asInt() * scale;

		int pos = 0;
		float z = 0;

		for (int i = 0; i < capacity; ++i){
			char chr = ' ';
			if (i < text.length())
				chr = text.charAt(i);

			JsonValue c = char_data.get(Integer.toString(chr));
			int x = pos + c.get("xoffset").asInt();
			int y = 0 - c.get("yoffset").asInt();
			int w = c.get("width").asInt();
			int h =  c.get("height").asInt();
			pos += c.get("xadvance").asInt();

			float u = c.get("x").asInt();
			float v = c.get("y").asInt();

			float[][] quad = {
				{x  , y-h, z, 0, 0, 1, u  , v+h},
				{x+w, y-h, z, 0, 0, 1, u+w, v+h},
				{x+w, y  , z, 0, 0, 1, u+w, v  },
				{x+w, y  , z, 0, 0, 1, u+w, v  },
				{x  , y  , z, 0, 0, 1, u,   v  },
				{x  , y-h, z, 0, 0, 1, u  , v+h}
			};

			z += 0.0001;

			for (float[] vert: quad){
				vert[0] *= scale;
				vert[1] *= scale;
				vert[0] -= 0.05 + (builtin ? 0.03 : 0);
				vert[1] += unit_height * (0.76 - (builtin ? 0.05 : 0));
				vert[6] *= su;
				vert[7] *= sv;
				for (float f: vert){
					verts[vi++] = f;
				}
			}
		}

		mesh.setVertices(verts, 0, verts.length);

	}

}
