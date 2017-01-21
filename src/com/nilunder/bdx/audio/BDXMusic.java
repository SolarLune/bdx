package com.nilunder.bdx.audio;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.nilunder.bdx.Bdx;

import java.util.ArrayList;

public class BDXMusic {

    Music data;

    static ArrayList<BDXMusic> playingMusic;

    public float pan;
    public float volume;                        // Manually handling volume because fading and global audio / music volume changes affect Music data's volume

    BDXMusic(FileHandle filePath){
        data = Gdx.audio.newMusic(filePath);
        pan = 0;
        volume = 1;
        if (playingMusic == null)
            playingMusic = new ArrayList<BDXMusic>();
    }

    public void play() {
        volume(volume);                 // Update the playing stream's volume and pan on play
        pan(pan);
        data.play();

        if (!playingMusic.contains(this))
            playingMusic.add(this);

        for (BDXMusic m : new ArrayList<BDXMusic>(playingMusic)) {
            if (!m.isPlaying())
                playingMusic.remove(m);
        }
    }

    public void pause() {
        data.pause();
    }

    public void stop() {
        data.stop();
    }

    public boolean isPlaying() {
        return data.isPlaying();
    }

    public void looping(boolean looping) {
        data.setLooping(looping);
    }

    public boolean looping() {
        return data.isLooping();
    }

    public void volume(float volume) {

        this.volume = volume;

        float tv = volume * Bdx.audio.music.volume() * Bdx.audio.volume();

        data.setVolume(Math.max(Float.MIN_VALUE, tv));      // If the volume on a Music stream gets to 0 or below, it can crash when switching to another music stream

    }

    public float volume() {
        return volume;
    }

    public void pan(float pan, float volume) {
        pan(pan);
        volume(volume);
    }

    public void pan(float pan) {
        this.pan = pan;

        data.setPan(pan + Bdx.audio.music.pan() + Bdx.audio.pan(), data.getVolume());
    }

    public float pan() {
        return pan;
    }

    public void position(float position) {
        data.setPosition(position);
    }

    public float position() {
        return data.getPosition();
    }

    public void dispose() {
        data.dispose();
    }

}
