package com.nilunder.bdx.gl;

import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.math.Matrix3;
import com.badlogic.gdx.math.Matrix4;
import com.nilunder.bdx.Bdx;

import javax.vecmath.Matrix3f;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector2f;
import javax.vecmath.Vector3f;

public class Mesh {

    public Model model;

    public Mesh(Model model){
        this.model = model;
    }

    public int getVertexCount(int materialSlot){
        return model.nodes.first().parts.get(materialSlot).meshPart.size;
    }

    public int getVertexCount(){
        int count = 0;
        for (int i = 0; i < model.nodes.get(0).parts.size; i++)
            count += getVertexCount(i);
        return count;
    }

    public void vertPos(int materialSlot, int index, float x, float y, float z){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[getVertexCount() * Bdx.VERT_STRIDE];
        mesh.getVertices(verts);
        int i = (model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE);
        verts[i] = x;
        verts[i+1] = y;
        verts[i+2] = z;
        mesh.setVertices(verts);
    }

    public void vertPos(int materialSlot, int index, Vector3f pos){
        vertPos(materialSlot, index, pos.x, pos.y, pos.z);
    }

    public Vector3f vertPos(int materialSlot, int index){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[3];
        mesh.getVertices((model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE), 3, verts);
        return new Vector3f(verts[0], verts[1], verts[2]);
    }

    public void vertNor(int materialSlot, int index, float x, float y, float z){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[getVertexCount() * Bdx.VERT_STRIDE];
        mesh.getVertices(verts);
        int i = (model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE);
        verts[i+3] = x;
        verts[i+4] = y;
        verts[i+5] = z;
        mesh.setVertices(verts);
    }

    public void vertNor(int materialSlot, int index, Vector3f pos){
        vertNor(materialSlot, index, pos.x, pos.y, pos.z);
    }

    public Vector3f vertNor(int materialSlot, int index){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[3];
        mesh.getVertices((model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE) + 3, 3, verts);
        return new Vector3f(verts[0], verts[1], verts[2]);
    }

    public void vertUV(int materialSlot, int index, float u, float v){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[getVertexCount() * Bdx.VERT_STRIDE];
        mesh.getVertices(verts);
        int i = (model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE);
        verts[i+6] = u;
        verts[i+7] = v;
        mesh.setVertices(verts);
    }

    public void vertUV(int materialSlot, int index, Vector2f uv){
        vertUV(materialSlot, index, uv.x, uv.y);
    }

    public Vector2f vertUV(int materialSlot, int index){
        com.badlogic.gdx.graphics.Mesh mesh = model.meshes.first();
        float[] verts = new float[3];
        mesh.getVertices((model.meshParts.get(materialSlot).offset * Bdx.VERT_STRIDE) + (index * Bdx.VERT_STRIDE) + 6, 2, verts);
        return new Vector2f(verts[0], verts[1]);
    }

    public void vertTransform(int materialSlot, Matrix4f matrix){
        Matrix4f m = matrix;
        float[] vals = {m.m00, m.m10, m.m20, m.m30,
                        m.m01, m.m11, m.m21, m.m31,
                        m.m02, m.m12, m.m22, m.m32,
                        m.m03, m.m13, m.m23, m.m33};
        model.meshes.first().transform(new Matrix4(vals), model.meshParts.get(materialSlot).offset, model.meshParts.get(materialSlot).size);
    }

    public void vertTransformUV(int materialSlot, Matrix3f matrix){
        Matrix3f m = matrix;
        float[] vals = {m.m00, m.m10, m.m20,
                m.m01, m.m11, m.m21,
                m.m02, m.m12, m.m22};
        float[] verts = new float[getVertexCount() * Bdx.VERT_STRIDE];
        model.meshes.first().getVertices(verts);
        MeshPart mp = model.meshParts.get(materialSlot);
        com.badlogic.gdx.graphics.Mesh.transformUV(new Matrix3(vals), verts, Bdx.VERT_STRIDE, 6, mp.offset, mp.size);
        model.meshes.first().setVertices(verts);
    }

    // Also UV, Normal, and Transforms (and possibly "do this to all" versions that don't use transforms?)

}
