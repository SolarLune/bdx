package com.nilunder.bdx.utils;

import com.nilunder.bdx.Bdx;
import com.nilunder.bdx.Scene;

import java.util.ArrayList;

public class UpdateThread implements Runnable {

    private boolean started;
    private boolean running;
    volatile private boolean update;

    public void run() {

        while (running) {

            if (update) {

                for (Scene scene : new ArrayList<Scene>(Bdx.scenes)) {
                    Bdx.profiler.start("__scene");
                    scene.update();
                    Bdx.profiler.stop("__scene");
                }

                update = false;

            }

        }

    }

    public void start() {
        started = true;
        running = true;
    }

    public boolean started() {
        return started;
    }

    public void update() {
        update = true;
    }

    public void stop() {
        running = false;
    }

}
