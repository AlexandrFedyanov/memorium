package com.cacao.libgdxgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.StretchViewport;

/**
 * Main menu. Three panels live in one vertically scrollable group:
 * the title panel (y = 0), game settings above it (y = +H) and records below it (y = -1.1H).
 */
public class FirstScreen implements Screen, InputProcessor {
    /** Records tabs: 1 = score, 2 = time, 3 = moves. */
    private static final int TAB_SCORE = 1;
    private static final int TAB_TIME = 2;
    private static final int TAB_MOVES = 3;

    private MyActor arrowDown;
    private MyActor arrowUp;
    Texture atlas;
    private Label button12;
    private Label button24;
    private Label button40;
    private Label buttonEasy;
    private Label buttonHard;
    private Label buttonNew;
    private Label buttonRecords;
    private Label buttonRun;
    private int complexity;
    private TextureRegion down;
    private TextureRegion firstBG;
    private Group firstGroup;
    MyGame game;
    private MyActor group1BGActor;
    private MyActor group2BGActor;
    private MyActor group3BGActor;
    private int mapSize;
    private TextureRegion movesT;
    private MyActor musicButton;
    private TextureRegion musicOff;
    private TextureRegion musicOn;
    private TextureRegion presed1;
    private MyActor presed1Actor;
    private TextureRegion presed2;
    private MyActor presed2Actor;
    private Label recordE12;
    private Label recordE24;
    private Label recordE40;
    private Label recordEasy;
    private Label recordH12;
    private Label recordH24;
    private Label recordH40;
    private Label recordHard;
    private Label recordMapSize;
    private Label recordSize12;
    private Label recordSize24;
    private Label recordSize40;
    private MyActor recordTabelM;
    private MyActor recordTabelS;
    private MyActor recordTabelT;
    private TextureRegion recordsBG;
    private Group recordsGroup;
    private TextureRegion scoreT;
    private ScreenSwitchActor screenSwitch;
    private TextureRegion screenSwitchTexture;
    private Group settingsGroup;
    private Group stageGroup;
    private TextureRegion startBG;
    private TextureRegion timeT;
    private int touchDownY;
    private int touchDraggedY;
    private TextureRegion up;
    private TextureRegion whiteLine;
    private MyActor whiteLineActor;
    Preferences mSettings = Gdx.app.getPreferences("mySettings");
    private LabelListener labelListener = new LabelListener();
    private MyAtorListener actorListener = new MyAtorListener();
    /** Which panel is shown: 0 = title, 1 = settings ("New game"), 2 = records. */
    private int backFlag = 0;
    private boolean isMusicOn = true;
    private int recordTabel = TAB_SCORE;
    private boolean isDragged = false;
    private LanguagesManager lang = LanguagesManager.getInstance();
    private SpriteBatch spriteBatch = new SpriteBatch();
    private float displayHeight = Gdx.graphics.getHeight();
    private float displayWidth = Gdx.graphics.getWidth();
    private Stage stage = new Stage(new StretchViewport(displayWidth, displayHeight), spriteBatch);
    private Sound touchDown = Gdx.audio.newSound(Gdx.files.internal("data/click4.wav"));
    private BitmapFont font = new BitmapFont(Gdx.files.internal("font/b.fnt"), false);

    class LabelListener extends ClickListener {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (isMusicOn) {
                touchDown.play();
            }
            Label l = (Label) event.getListenerActor();
            font.getData().setScale(font.getScaleX() + 0.15f);
            l.setStyle(new Label.LabelStyle(font, Color.DARK_GRAY));
            l.setPosition(l.getX() - (0.075f * l.getWidth()), l.getY());
            return true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            Label l = (Label) event.getListenerActor();
            font.getData().setScale(font.getScaleX() - 0.15f);
            l.setPosition(l.getX() + (0.075f * l.getWidth()), l.getY());
            // Title panel buttons are black, the others white.
            Color color = (l == buttonNew || l == buttonRecords) ? Color.BLACK : Color.WHITE;
            l.setStyle(new Label.LabelStyle(font, color));
            if (isDragged) {
                return;
            }
            if (l == buttonNew) {
                stageGroup.addAction(Actions.moveTo(0.0f, -1.1f * displayHeight, 0.5f, Interpolation.sine));
                backFlag = 1;
                Gdx.input.setCatchKey(Input.Keys.BACK, true);
            } else if (l == buttonRecords) {
                stageGroup.addAction(Actions.moveTo(0.0f, 1.1f * displayHeight, 0.5f, Interpolation.sine));
                backFlag = 2;
                Gdx.input.setCatchKey(Input.Keys.BACK, true);
            } else if (l == buttonRun) {
                game.application.showAdMob(false);
                screenSwitch = new ScreenSwitchActor(screenSwitchTexture);
                screenSwitch.setSize(displayWidth, displayHeight);
                screenSwitch.setPosition(0.0f, 0.0f);
                screenSwitch.setColor(new Color(1.0f, 1.0f, 1.0f, 0.0f));
                stage.addActor(screenSwitch);
                screenSwitch.addAction(Actions.sequence(
                        Actions.color(new Color(1.0f, 1.0f, 1.0f, 1.0f), 0.35f),
                        Actions.run(new Runnable() {
                            @Override
                            public void run() {
                                game.setScreen(game.gameScreen);
                                screenSwitch.remove();
                            }
                        })));
            } else if (l == buttonEasy) {
                selectComplexity(0);
            } else if (l == buttonHard) {
                selectComplexity(1);
            } else if (l == button12) {
                selectMapSize(12);
            } else if (l == button24) {
                selectMapSize(24);
            } else if (l == button40) {
                selectMapSize(40);
            }
        }
    }

    class MyAtorListener extends ClickListener {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
            if (isMusicOn) {
                touchDown.play();
            }
            MyActor a = (MyActor) event.getListenerActor();
            a.setSize(a.getWidth() + ((displayWidth * 0.2f) / 6.0f), a.getHeight() + ((displayWidth * 0.2f) / 6.0f));
            a.setPosition(a.getX() - ((0.1f * displayWidth) / 6.0f), a.getY());
            return true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
            MyActor a = (MyActor) event.getListenerActor();
            // A selected records tab stays enlarged; everything else shrinks back.
            if (isDragged || a == musicButton) {
                a.setSize(a.getWidth() - ((0.2f * displayWidth) / 6.0f), a.getHeight() - ((0.2f * displayWidth) / 6.0f));
                a.setPosition(a.getX() + ((0.1f * displayWidth) / 6.0f), a.getY());
            }
            if (isDragged) {
                return;
            }
            if (a == musicButton) {
                toggleMusic();
            } else if (a == recordTabelS) {
                selectRecordTab(TAB_SCORE);
            } else if (a == recordTabelT) {
                selectRecordTab(TAB_TIME);
            } else if (a == recordTabelM) {
                selectRecordTab(TAB_MOVES);
            }
        }
    }

    class MyActor extends Actor {
        private TextureRegion image;

        public MyActor(TextureRegion image) {
            this.image = image;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.draw(image, getX(), getY(), getWidth(), getHeight());
        }
    }

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

    public FirstScreen(MyGame game) {
        this.game = game;
        for (TextureRegion textureRegion : font.getRegions()) {
            textureRegion.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        atlas = game.mainAtlas;
        firstBG = new TextureRegion(atlas, 0, 0, 540, 900);
        recordsBG = new TextureRegion(atlas, 0, 900, 540, 990);
        startBG = new TextureRegion(atlas, 540, 0, 540, 990);
        presed1 = new TextureRegion(atlas, 1686, 0, 212, 110);
        presed2 = new TextureRegion(atlas, 1940, 0, 108, 112);
        musicOn = new TextureRegion(atlas, 1748, 450, 150, 150);
        musicOff = new TextureRegion(atlas, 1898, 450, 150, 150);
        timeT = new TextureRegion(atlas, 1748, 150, 150, 150);
        movesT = new TextureRegion(atlas, 1898, 150, 150, 150);
        scoreT = new TextureRegion(atlas, 1748, 300, 300, 150);
        up = new TextureRegion(atlas, 1648, 600, 200, 100);
        down = new TextureRegion(atlas, 1848, 600, 200, 100);
        screenSwitchTexture = new TextureRegion(atlas, 1648, 948, 100, 100);
        whiteLine = new TextureRegion(atlas, 1748, 948, 100, 100);
    }

    private void selectComplexity(int value) {
        Label button = value == 0 ? buttonEasy : buttonHard;
        presed1Actor.setSize(1.3f * button.getWidth(), button.getHeight());
        presed1Actor.setPosition(button.getX() - (button.getWidth() * 0.2f), button.getY());
        mSettings.putInteger("complexity", value);
        mSettings.flush();
    }

    private void selectMapSize(int value) {
        Label button = value == 12 ? button12 : value == 24 ? button24 : button40;
        presed2Actor.setSize(button.getWidth() * 1.5f, button.getHeight());
        presed2Actor.setPosition(button.getX() - (button.getWidth() * 0.2f), button.getY());
        mSettings.putInteger("mapsize", value);
        mSettings.flush();
    }

    private void toggleMusic() {
        isMusicOn = !isMusicOn;
        musicButton.image = isMusicOn ? musicOn : musicOff;
        if (isMusicOn) {
            game.music.play();
        } else {
            game.music.pause();
        }
        mSettings.putBoolean("isMusicOn", isMusicOn);
        mSettings.flush();
    }

    private float tabY(MyActor tab) {
        return (-0.6f * displayHeight) - (3.6f * tab.getHeight());
    }

    /** Puts a records tab back to its normal (unselected) size and position. */
    private void resetTab(MyActor tab, float centerX) {
        float width = tab == recordTabelS ? displayWidth / 4.0f : displayWidth / 8.0f;
        tab.setSize(width, displayWidth / 8.0f);
        tab.setPosition(centerX - (tab.getWidth() / 2.0f), tabY(tab));
        tab.setTouchable(Touchable.enabled);
    }

    private void selectRecordTab(int tab) {
        MyActor selected = tab == TAB_SCORE ? recordTabelS : tab == TAB_TIME ? recordTabelT : recordTabelM;
        whiteLineActor.addAction(Actions.parallel(
                Actions.moveTo(selected.getX(), whiteLineActor.getY(), 0.15f),
                Actions.sizeTo(selected.getWidth(), whiteLineActor.getHeight(), 0.15f)));
        if (selected != recordTabelT) resetTab(recordTabelT, displayWidth / 5.0f);
        if (selected != recordTabelS) resetTab(recordTabelS, displayWidth / 2.0f);
        if (selected != recordTabelM) resetTab(recordTabelM, (displayWidth * 4.0f) / 5.0f);
        selected.setTouchable(Touchable.disabled);
        recordTabel = tab;
        mSettings.putInteger("recordTabel", tab);
        mSettings.flush();
        fillRecords();
    }

    private String recordText(String key) {
        String suffix = recordTabel == TAB_TIME ? "T" : recordTabel == TAB_MOVES ? "M" : "";
        if (!mSettings.contains(key + suffix)) {
            return "---";
        }
        int value = mSettings.getInteger(key + suffix, 0);
        return recordTabel == TAB_TIME ? MyGame.formatTime(value) : Integer.toString(value);
    }

    private void setRecord(Label label, String key, float centerX, float rowOffset) {
        label.setText(recordText(key));
        label.pack();
        label.setPosition(centerX - (label.getWidth() / 2.0f), (-0.6f * displayHeight) + (rowOffset * label.getHeight()));
    }

    /** Fills the records table for the current tab. */
    private void fillRecords() {
        float easyX = displayWidth / 2.0f;
        float hardX = (displayWidth * 4.0f) / 5.0f;
        setRecord(recordE12, "easy12", easyX, 0.1f);
        setRecord(recordE24, "easy24", easyX, -1.1f);
        setRecord(recordE40, "easy40", easyX, -2.3f);
        setRecord(recordH12, "hard12", hardX, 0.1f);
        setRecord(recordH24, "hard24", hardX, -1.1f);
        setRecord(recordH40, "hard40", hardX, -2.3f);
    }

    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer(this, stage);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setCatchKey(Input.Keys.BACK, false);
        if (mSettings.contains("isMusicOn")) {
            isMusicOn = mSettings.getBoolean("isMusicOn");
        }
        if (isMusicOn) {
            musicButton = new MyActor(musicOn);
            game.music.play();
        } else {
            musicButton = new MyActor(musicOff);
        }
        musicButton.setSize(displayWidth / 6.0f, displayWidth / 6.0f);
        musicButton.setPosition(displayWidth - (1.5f * musicButton.getWidth()), 0.5f * musicButton.getHeight());
        musicButton.addListener(actorListener);
        String newGame = lang.getString("new_game");
        String records = lang.getString("records");
        String run = lang.getString("start");
        String easy = lang.getString("easy");
        String hard = lang.getString("hard");
        String size = lang.getString("map_size");
        font.getData().setScale(displayWidth / 1080.0f);
        font.setColor(Color.BLACK);

        // Title panel
        Label.LabelStyle style = new Label.LabelStyle(font, Color.BLACK);
        buttonNew = new Label(newGame, style);
        buttonNew.addListener(labelListener);
        buttonNew.setSize(buttonNew.getWidth(), buttonNew.getHeight() + (displayHeight / 40.0f));
        buttonNew.setPosition((displayWidth / 2.0f) - (buttonNew.getWidth() / 2.0f), displayHeight / 2.0f);
        buttonRecords = new Label(records, style);
        buttonRecords.addListener(labelListener);
        buttonRecords.setSize(buttonRecords.getWidth(), buttonRecords.getHeight() + (displayHeight / 40.0f));
        buttonRecords.setPosition((displayWidth / 2.0f) - (buttonRecords.getWidth() / 2.0f), buttonNew.getY() - buttonRecords.getHeight());
        arrowUp = new MyActor(up);
        arrowUp.setSize(displayWidth / 8.0f, displayWidth / 16.0f);
        arrowDown = new MyActor(down);
        arrowDown.setSize(displayWidth / 8.0f, displayWidth / 16.0f);
        arrowUp.setPosition((displayWidth / 2.0f) - (arrowUp.getWidth() / 2.0f), buttonNew.getY() + buttonNew.getHeight());
        arrowDown.setPosition((displayWidth / 2.0f) - (arrowDown.getWidth() / 2.0f), buttonRecords.getY() - arrowDown.getHeight());
        group1BGActor = new MyActor(firstBG);
        group1BGActor.setSize(displayWidth, displayHeight);
        group1BGActor.setPosition(0.0f, 0.0f);
        firstGroup = new Group();
        firstGroup.addActor(group1BGActor);
        firstGroup.addActor(buttonNew);
        firstGroup.addActor(buttonRecords);
        firstGroup.addActor(arrowUp);
        firstGroup.addActor(arrowDown);

        // Settings panel (above the title panel)
        font.setColor(Color.WHITE);
        Label.LabelStyle style2 = new Label.LabelStyle(font, Color.WHITE);
        buttonEasy = new Label(easy, style2);
        buttonHard = new Label(hard, style2);
        button12 = new Label("12", style2);
        button24 = new Label("24", style2);
        button40 = new Label("40", style2);
        buttonRun = new Label(run, style2);
        buttonEasy.addListener(labelListener);
        buttonHard.addListener(labelListener);
        button12.addListener(labelListener);
        button24.addListener(labelListener);
        button40.addListener(labelListener);
        buttonRun.addListener(labelListener);
        button24.setPosition((displayWidth / 2.0f) - (button24.getWidth() / 2.0f), (1.6f * displayHeight) - (button12.getHeight() / 2.0f));
        button12.setPosition((button24.getX() - button12.getWidth()) - (displayHeight / 20.0f), button24.getY());
        button40.setPosition(button24.getX() + button40.getWidth() + (displayHeight / 20.0f), button24.getY());
        buttonEasy.setPosition((displayWidth / 3.0f) - (buttonEasy.getWidth() / 2.0f), button24.getY() + (1.2f * buttonEasy.getHeight()));
        buttonHard.setPosition(((displayWidth / 3.0f) * 2.0f) - (buttonHard.getWidth() / 2.0f), button24.getY() + (1.2f * buttonHard.getHeight()));
        buttonRun.setPosition((displayWidth / 2.0f) - (buttonRun.getWidth() / 2.0f), button24.getY() - (1.2f * buttonRun.getHeight()));
        presed1Actor = new MyActor(presed1);
        presed2Actor = new MyActor(presed2);
        presed1Actor.setSize(1.3f * buttonEasy.getWidth(), buttonEasy.getHeight());
        presed2Actor.setSize(1.3f * button12.getWidth(), button12.getHeight());
        presed1Actor.setPosition(buttonEasy.getX() - (0.2f * buttonEasy.getWidth()), buttonEasy.getY());
        presed2Actor.setPosition(button12.getX() - (0.2f * button12.getWidth()), button12.getY());
        group2BGActor = new MyActor(startBG);
        group2BGActor.setSize(displayWidth, 1.1f * displayHeight);
        group2BGActor.setPosition(0.0f, displayHeight);
        settingsGroup = new Group();
        settingsGroup.addActor(group2BGActor);
        settingsGroup.addActor(button12);
        settingsGroup.addActor(button24);
        settingsGroup.addActor(button40);
        settingsGroup.addActor(buttonEasy);
        settingsGroup.addActor(buttonHard);
        settingsGroup.addActor(buttonRun);
        settingsGroup.addActor(presed1Actor);
        settingsGroup.addActor(presed2Actor);

        // Records panel (below the title panel)
        recordMapSize = new Label(size, style2);
        recordEasy = new Label(easy, style2);
        recordHard = new Label(hard, style2);
        recordSize12 = new Label("12", style2);
        recordSize24 = new Label("24", style2);
        recordSize40 = new Label("40", style2);
        recordTabelS = new MyActor(scoreT);
        recordTabelT = new MyActor(timeT);
        recordTabelM = new MyActor(movesT);
        recordTabelS.addListener(actorListener);
        recordTabelT.addListener(actorListener);
        recordTabelM.addListener(actorListener);
        whiteLineActor = new MyActor(whiteLine);
        if (mSettings.contains("recordTabel")) {
            recordTabel = mSettings.getInteger("recordTabel", TAB_SCORE);
        }
        recordE12 = new Label("", style2);
        recordE24 = new Label("", style2);
        recordE40 = new Label("", style2);
        recordH12 = new Label("", style2);
        recordH24 = new Label("", style2);
        recordH40 = new Label("", style2);
        fillRecords();
        recordMapSize.setPosition((displayWidth / 5.0f) - (recordMapSize.getWidth() / 2.0f), (-0.6f * displayHeight) + (1.3f * recordMapSize.getHeight()));
        recordSize12.setPosition((displayWidth / 5.0f) - (recordSize12.getWidth() / 2.0f), (-0.6f * displayHeight) + (0.1f * recordSize12.getHeight()));
        recordSize24.setPosition((displayWidth / 5.0f) - (recordSize24.getWidth() / 2.0f), (-0.6f * displayHeight) - (1.1f * recordSize12.getHeight()));
        recordSize40.setPosition((displayWidth / 5.0f) - (recordSize40.getWidth() / 2.0f), (-0.6f * displayHeight) - (2.3f * recordSize12.getHeight()));
        recordEasy.setPosition((displayWidth / 2.0f) - (recordEasy.getWidth() / 2.0f), (-0.6f * displayHeight) + (1.3f * recordEasy.getHeight()));
        recordHard.setPosition(((displayWidth * 4.0f) / 5.0f) - (recordHard.getWidth() / 2.0f), (-0.6f * displayHeight) + (1.3f * recordHard.getHeight()));
        resetTab(recordTabelT, displayWidth / 5.0f);
        resetTab(recordTabelS, displayWidth / 2.0f);
        resetTab(recordTabelM, (displayWidth * 4.0f) / 5.0f);
        MyActor selected = recordTabel == TAB_TIME ? recordTabelT : recordTabel == TAB_MOVES ? recordTabelM : recordTabelS;
        selected.setTouchable(Touchable.disabled);
        selected.setSize(selected.getWidth() + ((0.2f * displayWidth) / 6.0f), selected.getHeight() + ((0.2f * displayWidth) / 6.0f));
        selected.setPosition(selected.getX() - ((0.1f * displayWidth) / 6.0f), selected.getY());
        whiteLineActor.setSize(selected.getWidth(), displayHeight / 120.0f);
        whiteLineActor.setPosition(selected.getX(), selected.getY() - (1.5f * whiteLineActor.getHeight()));
        group3BGActor = new MyActor(recordsBG);
        group3BGActor.setSize(displayWidth, 1.1f * displayHeight);
        group3BGActor.setPosition(0.0f, -1.1f * displayHeight);
        recordsGroup = new Group();
        recordsGroup.addActor(group3BGActor);
        recordsGroup.addActor(recordMapSize);
        recordsGroup.addActor(recordSize12);
        recordsGroup.addActor(recordSize24);
        recordsGroup.addActor(recordSize40);
        recordsGroup.addActor(recordEasy);
        recordsGroup.addActor(recordE12);
        recordsGroup.addActor(recordE24);
        recordsGroup.addActor(recordE40);
        recordsGroup.addActor(recordHard);
        recordsGroup.addActor(recordH12);
        recordsGroup.addActor(recordH24);
        recordsGroup.addActor(recordH40);
        recordsGroup.addActor(recordTabelS);
        recordsGroup.addActor(recordTabelT);
        recordsGroup.addActor(recordTabelM);
        recordsGroup.addActor(whiteLineActor);

        stageGroup = new Group();
        stageGroup.setPosition(0.0f, 0.0f);
        stageGroup.addActor(firstGroup);
        stageGroup.addActor(settingsGroup);
        stageGroup.addActor(recordsGroup);
        stage.addActor(stageGroup);
        stage.addActor(musicButton);

        // Fade in from black
        screenSwitch = new ScreenSwitchActor(screenSwitchTexture);
        screenSwitch.setSize(displayWidth, displayHeight);
        screenSwitch.setPosition(0.0f, 0.0f);
        stage.addActor(screenSwitch);
        screenSwitch.addAction(Actions.sequence(
                Actions.color(new Color(1.0f, 1.0f, 1.0f, 0.0f), 0.35f),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        game.application.showAdMob(true);
                        screenSwitch.remove();
                    }
                })));

        if (mSettings.contains("complexity")) {
            complexity = mSettings.getInteger("complexity", 0);
            Label button = complexity == 1 ? buttonHard : buttonEasy;
            presed1Actor.setSize(1.3f * button.getWidth(), button.getHeight());
            presed1Actor.setPosition(button.getX() - (0.2f * button.getWidth()), button.getY());
        }
        if (mSettings.contains("mapsize")) {
            mapSize = mSettings.getInteger("mapsize", 0);
            Label button = mapSize == 24 ? button24 : mapSize == 40 ? button40 : button12;
            presed2Actor.setSize(1.5f * button.getWidth(), button.getHeight());
            presed2Actor.setPosition(button.getX() - (0.2f * button.getWidth()), button.getY());
        }
    }

    @Override
    public void render(float delta) {
        stage.draw();
        stage.act(Gdx.graphics.getDeltaTime());
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        atlas.dispose();
        font.dispose();
        touchDown.dispose();
        stage.dispose();
    }

    @Override
    public void hide() {
        firstGroup.remove();
        settingsGroup.remove();
        recordsGroup.remove();
        // show() creates a new music button every time
        musicButton.remove();
        backFlag = 0;
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
        if (backFlag == 1 || backFlag == 2) {
            Gdx.input.setCatchKey(Input.Keys.BACK, false);
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK && (backFlag == 1 || backFlag == 2)) {
            stageGroup.addAction(Actions.moveTo(0.0f, 0.0f, 0.5f, Interpolation.sine));
            backFlag = 0;
            Gdx.input.setCatchKey(Input.Keys.BACK, false);
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    /** Converts a screen Y coordinate into stage units (the stage may be stretched after a resize). */
    private int toStageY(int screenY) {
        return (int) (screenY * displayHeight / Gdx.graphics.getHeight());
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        screenY = toStageY(screenY);
        touchDownY = screenY;
        touchDraggedY = screenY;
        isDragged = false;
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        screenY = toStageY(screenY);
        float y = stageGroup.getY();
        if (isDragged) {
            if (y <= displayHeight && y >= -displayHeight) {
                stageGroup.moveBy(0.0f, touchDraggedY - screenY);
            } else if (y > displayHeight && touchDraggedY - screenY < 0) {
                stageGroup.moveBy(0.0f, touchDraggedY - screenY);
            } else if (y < -displayHeight && touchDraggedY - screenY > 0) {
                stageGroup.moveBy(0.0f, touchDraggedY - screenY);
            }
        } else if (Math.abs(touchDownY - screenY) > displayHeight / 30.0f) {
            isDragged = true;
        }
        touchDraggedY = screenY;
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        float y = stageGroup.getY();
        if (y >= displayHeight / 15.0f && backFlag == 0) {
            backFlag = 2;
            stageGroup.addAction(Actions.moveTo(0.0f, 1.1f * displayHeight, (0.5f * ((1.1f * displayHeight) - y)) / (1.1f * displayHeight)));
        } else if (y >= 0.0f && y < displayHeight / 15.0f && backFlag == 0) {
            backFlag = 0;
            stageGroup.addAction(Actions.moveTo(0.0f, 0.0f, 0.2f));
            Gdx.input.setCatchKey(Input.Keys.BACK, false);
        } else if (y <= -displayHeight / 15.0f && backFlag == 0) {
            backFlag = 1;
            stageGroup.addAction(Actions.moveTo(0.0f, displayHeight * -1.1f, (0.5f * ((displayHeight * -1.1f) - y)) / (displayHeight * -1.1f)));
        } else if (y < 0.0f && y > -displayHeight / 15.0f && backFlag == 0) {
            backFlag = 0;
            stageGroup.addAction(Actions.moveTo(0.0f, 0.0f, 0.2f));
            Gdx.input.setCatchKey(Input.Keys.BACK, false);
        } else if (y <= displayHeight - (displayHeight / 15.0f) && backFlag == 2) {
            backFlag = 0;
            stageGroup.addAction(Actions.moveTo(0.0f, 0.0f, (0.5f * y) / (1.1f * displayHeight)));
            Gdx.input.setCatchKey(Input.Keys.BACK, false);
        } else if (y > displayHeight - (displayHeight / 15.0f) && backFlag == 2) {
            backFlag = 2;
            stageGroup.addAction(Actions.moveTo(0.0f, 1.1f * displayHeight, 0.2f));
            Gdx.input.setCatchKey(Input.Keys.BACK, true);
        } else if (y >= (displayHeight / 15.0f) - displayHeight && backFlag == 1) {
            backFlag = 0;
            stageGroup.addAction(Actions.moveTo(0.0f, 0.0f, (0.5f * y) / (displayHeight * -1.1f)));
            Gdx.input.setCatchKey(Input.Keys.BACK, false);
        } else if (y < (displayHeight / 15.0f) - displayHeight && backFlag == 1) {
            backFlag = 1;
            stageGroup.addAction(Actions.moveTo(0.0f, displayHeight * -1.1f, 0.2f));
            Gdx.input.setCatchKey(Input.Keys.BACK, true);
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
