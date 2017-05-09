package com.nilunder.bdx.utils;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.GameObject;
import com.nilunder.bdx.Scene;

import java.util.ArrayList;

public class UpdateThread {

    static private int staticID;
    static public boolean announceLate = true;

    public Scene scene;
    public ArrayList<GameObject> objects;
    public int maxQueueSize = -1;
    public int id;
    private long runTime;
    public boolean running = true;

    public UpdateThread(Scene scene) {
        this.scene = scene;
        objects = new ArrayList<GameObject>();
        id = staticID;
        staticID++;
    }

    public String toString() {
        return "Update Thread #" + id + " " + hashCode();
    }

    public boolean runningLate() {
        return runTime() > Bdx.TICK_TIME;
    }

    public void frame() {}

    public float runTime() {
        return runTime / 1000f;
    }

    public Runnable update = new Runnable() {

        public void run() {

            if (objects.size() > 0) {

                runTime = System.currentTimeMillis();
                for (GameObject g : new ArrayList<GameObject>(objects))
                    scene.updateGameObject(g);
                runTime = System.currentTimeMillis() - runTime;

                if (announceLate && runningLate()) {
                    System.out.println("ATTENTION: UPDATE THREAD #" + id + " RAN LATE.");
                    System.out.println(runTime() + " (ELAPSED) VS. " + Bdx.TICK_TIME + " (GOAL)");
                    System.out.println("(" + Math.round((runTime() / Bdx.TICK_TIME) * 100) + "% OVERSHOT)");
                }

            }

        }

    };

}
