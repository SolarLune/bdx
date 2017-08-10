package com.nilunder.bdx.gl;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.nilunder.bdx.Scene;

public abstract class UniformSet {

    public Scene scene;

    public abstract void set(ShaderProgram program);

}