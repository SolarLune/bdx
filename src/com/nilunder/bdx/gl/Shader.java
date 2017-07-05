package com.nilunder.bdx.gl;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.Disposable;
import com.nilunder.bdx.Bdx;

import java.util.ArrayList;

public class Shader implements Disposable {

    public static boolean hotloading = false;
    public boolean active = true;
    public String vertexShader = "";
    public String fragmentShader = "";
    public String lastWorkingVertexShader = "";
    public String lastWorkingFragmentShader = "";
    public FileHandle vertexShaderHandle = null;
    public FileHandle fragmentShaderHandle = null;
    public String prefix = "";
    public ArrayList<UniformSet> uniformSets;
    public ShaderProgram program;
    private boolean updateProgram = false;

    private long lastLoadAttempt = 0;

    public Shader(FileHandle vertexShader, FileHandle fragmentShader) {
        this(vertexShader.readString(), fragmentShader.readString());
        vertexShaderHandle = vertexShader;
        fragmentShaderHandle = fragmentShader;
    }

    public Shader(String vertexShader, String fragmentShader) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        init();
    }

    public void init() {
        uniformSets = new ArrayList<UniformSet>();
        lastLoadAttempt = System.currentTimeMillis();
    }

    public boolean hotloadable() {

        if ((hotloading) && (fragmentShaderHandle.lastModified() > lastLoadAttempt || vertexShaderHandle.lastModified() > lastLoadAttempt)) {
            updateProgram = true;
            lastLoadAttempt = System.currentTimeMillis();
        }

        return updateProgram;

    }

    public ShaderProgram compile() {

        if (hotloadable())
            setShader(vertexShaderHandle.readString(), fragmentShaderHandle.readString());

        if (program == null || updateProgram) {

            program = new ShaderProgram(prefix + vertexShader, prefix + fragmentShader);

            if (program.isCompiled()) {
                if (updateProgram)
                    Gdx.app.log("BDX Shader", "Successfully re-compiled at " + Bdx.time);
                lastWorkingVertexShader = vertexShader;
                lastWorkingFragmentShader = fragmentShader;
            } else {
                String l = "";
                if (fragmentShaderHandle != null)
                    l += "Fragment shader location: " + fragmentShaderHandle.path() + "\n";
                else
                    l += "Fragment shader: " + fragmentShader + "\n";
                if (vertexShaderHandle != null)
                    l += "Vertex shader location: " + vertexShaderHandle.path();
                else
                    l += "Vertex shader: " + vertexShader;
                    Gdx.app.error("BDX Shader", "ERROR: Compilation error in ScreenShader at " + Bdx.time + ": \n" + "--------\n" + program.getLog() + "--------\n" + l);
                program.dispose();
                program = new ShaderProgram(prefix + lastWorkingVertexShader, prefix + lastWorkingFragmentShader);
            }

        }

        updateProgram = false;

        return program;

    }

    public void setShader(String vertexShader, String fragmentShader) {

        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
        lastLoadAttempt = System.currentTimeMillis();
        updateProgram = true;

    }

    public boolean compiled() {
        return program != null && program.isCompiled();
    }

    public void dispose() {

        if (program != null) {
            program.dispose();
            program = null;
        }

    }

    public Shader setPrefix(String prefix){
        this.prefix = prefix;
        return this;
    }

    public static Shader load(String vertexPath, String fragmentPath) {
        return new Shader(Gdx.files.internal("bdx/shaders/3d/" + vertexPath), Gdx.files.internal("bdx/shaders/3d/" + fragmentPath));
    }

    public static Shader load(String fragmentPath) {
        return load("default.vert", fragmentPath);
    }

}
