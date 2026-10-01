package com.cacao.libgdxgame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Texture;

public class MyGame extends Game {
    public IActivityRequestHandler application;
    public FirstScreen firstScreen;
    public GameScreen gameScreen;
    public LogoScreen logoScreen;
    Texture mainAtlas;
    public Music music;

    public MyGame(IActivityRequestHandler app) {
        this.application = app;
    }

    @Override
    public void create() {
        mainAtlas = new Texture(Gdx.files.internal("texture/atlas.png"));
        music = Gdx.audio.newMusic(Gdx.files.internal("data/music.mp3"));
        music.setLooping(true);
        firstScreen = new FirstScreen(this);
        gameScreen = new GameScreen(this);
        logoScreen = new LogoScreen(this);
        setScreen(logoScreen);
    }

    /** Formats seconds as mm:ss. */
    static String formatTime(int seconds) {
        return String.format(java.util.Locale.US, "%02d:%02d", seconds / 60, seconds % 60);
    }
}
