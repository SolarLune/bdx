package com.nilunder.bdx.audio;

public class Audio {

    private float volume = 1;
    private float pan = 0;
    // There's no pitch controls for music, so it doesn't make sense to have it at the audio level, either

    public Music music;
    public Sounds sounds;

    public Audio(){
        music = new Music();
        sounds = new Sounds();
    }

    public void dispose(){
        music.dispose();
        sounds.dispose();
    }

    public void volume(float volume){
        this.volume = volume;
        music.volume(music.volume());
        sounds.volume(sounds.volume());
    }

    public float volume() {
        return volume;
    }

    public void pan(float pan) {
        this.pan = pan;
        music.pan(music.pan());
        sounds.pan(sounds.pan());
    }

    public float pan() {
        return pan;
    }

}
