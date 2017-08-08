package com.nilunder.bdx.audio;

import com.badlogic.gdx.*;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.nilunder.bdx.Bdx;

public class BDXSound {

    Sound data;

    BDXSound(FileHandle filePath){
        data = Gdx.audio.newSound(filePath);
    }

    public long play() {
        return play(1, 1, 0);
    }

    public long play(float volume) {
        return play(volume, 1, 0);
    }

    public long play(float volume, float pitch, float pan) {

        long l = data.play(Math.max(volume * Bdx.audio.volume() * Bdx.audio.sounds.volume(), 0),
                pitch * Bdx.audio.sounds.pitch(),
                pan + Bdx.audio.pan() + Bdx.audio.sounds.pan());

        return l;
    }

    public long loop() {
        return loop(1, 1, 0);
    }

    public long loop(float volume) {
        return loop(volume, 1, 0);
    }

    public long loop(float volume, float pitch, float pan) {
        long l = data.loop(Math.max(volume * Bdx.audio.volume() * Bdx.audio.sounds.volume(), 0),
                pitch * Bdx.audio.sounds.pitch(),
                pan + Bdx.audio.pan() + Bdx.audio.sounds.pan());

        return l;
    }

    public void stop() {
        data.stop();
    }

    public void stop(long handleID) {
        data.stop(handleID);
    }

    public void pause() {
        data.pause();
    }

    public void resume() {
        data.resume();
    }

    public void resume(long handleID) {
        data.resume(handleID);
    }

    public void dispose() {
        data.dispose();
        data = null;
    }

    public void pause(long handleID) {
        data.pause(handleID);
    }

    public void setLooping(long handleID, boolean looping) {
        data.setLooping(handleID, looping);
    }

    public void setPitch(long handleID, float pitch) {
        data.setPitch(handleID, pitch);
    }

    public void setVolume(long handleID, float volume) {
        data.setVolume(handleID, Math.max(0, volume));
    }

    public void setPan(long handleID, float pan, float volume) {
        data.setPan(handleID, pan, Math.max(0, volume));
    }

}
