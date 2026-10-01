package com.cacao.libgdxgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.StretchViewport;

public class LogoScreen implements Screen {
    private ScreenSwitchActor actor;
    private TextureRegion actorTexture;
    private Animation<TextureRegion> animation;
    private TextureRegion backGround;
    private TextureRegion currentFrame;
    public MyGame game;
    private TextureRegion logo;
    private SpriteBatch spriteBatch;
    private Stage stage;
    private float stateTime;
    private TextureRegion[] frames = new TextureRegion[20];
    private boolean isStartAnim = false;
    private float displayHeight = Gdx.graphics.getHeight();
    private float displayWidth = Gdx.graphics.getWidth();
    private Texture logoAtlas = new Texture(Gdx.files.internal("texture/logoAtlas2.png"));

    class ScreenSwitchActor extends Actor {
        private TextureRegion image;

        public ScreenSwitchActor(TextureRegion image) {
            this.image = image;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.setColor(getColor());
            batch.draw(image, getX(), getY(), getWidth(), getHeight());
            batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public LogoScreen(MyGame game) {
        this.game = game;
        logoAtlas.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        backGround = new TextureRegion(logoAtlas, 700, 0, 100, 100);
        actorTexture = new TextureRegion(logoAtlas, 600, 0, 100, 100);
        actor = new ScreenSwitchActor(actorTexture);
        actor.setSize(displayWidth, displayHeight);
        stage = new Stage(new StretchViewport(displayWidth, displayHeight));
        stage.addActor(actor);
        logo = new TextureRegion(logoAtlas, 0, 0, 600, 150);
        // 20 animation frames of 200x200, 5 per row, starting at y=150
        for (int i = 0; i < frames.length; i++) {
            frames[i] = new TextureRegion(logoAtlas, (i % 5) * 200, 150 + (i / 5) * 200, 200, 200);
        }
        animation = new Animation<>(0.05f, frames);
        spriteBatch = new SpriteBatch();
        stateTime = 0.0f;
    }

    @Override
    public void show() {
        actor.setColor(new Color(1.0f, 1.0f, 1.0f, 0.0f));
        new Timer().scheduleTask(new Timer.Task() {
            @Override
            public void run() {
                isStartAnim = true;
            }
        }, 0.7f);
    }

    @Override
    public void render(float delta) {
        if (isStartAnim) {
            stateTime += Gdx.graphics.getDeltaTime();
        }
        currentFrame = animation.getKeyFrame(stateTime, true);
        spriteBatch.setProjectionMatrix(stage.getCamera().combined);
        spriteBatch.begin();
        spriteBatch.draw(backGround, 0.0f, 0.0f, displayWidth, displayHeight);
        spriteBatch.draw(logo, 0.1f * displayWidth, (displayHeight / 2.0f) - (0.075f * displayWidth), 0.6f * displayWidth, 0.15f * displayWidth);
        spriteBatch.draw(currentFrame, 0.68f * displayWidth, (displayHeight / 2.0f) - (0.025f * displayWidth), displayWidth * 0.2f, displayWidth * 0.2f);
        spriteBatch.end();
        stage.draw();
        stage.act(Gdx.graphics.getDeltaTime());
        if (animation.isAnimationFinished(stateTime) && isStartAnim) {
            isStartAnim = false;
            actor.addAction(Actions.delay(0.25f, Actions.sequence(
                    Actions.color(new Color(1.0f, 1.0f, 1.0f, 1.0f), 0.35f),
                    Actions.run(new Runnable() {
                        @Override
                        public void run() {
                            // Deferred: hide() disposes this stage, which must not happen mid-act().
                            Gdx.app.postRunnable(new Runnable() {
                                @Override
                                public void run() {
                                    game.setScreen(game.firstScreen);
                                }
                            });
                        }
                    }))));
        }
    }

    @Override
    public void dispose() {
        logoAtlas.dispose();
        spriteBatch.dispose();
        stage.dispose();
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void resume() {
    }
}
