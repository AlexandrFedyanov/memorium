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
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.StretchViewport;

public class GameScreen implements Screen, InputProcessor {
    private Label againL;
    private TextureRegion backGround;
    private int cardSize;
    private Sound cardsFlip;
    private dors dor1;
    private TextureRegion dor1Texture;
    private dors dor2;
    private TextureRegion dor2Texture;
    private Sound dorsClosedSound;
    private Sound dorsOpendSound;
    MyGame game;
    private Texture gameAtlas;
    private LanguagesManager lang;
    private Label menuL;
    private Label moveL;
    private Label moveMeanL;
    private Label scoreL;
    private Label scoreMeanL;
    private dors screenSwitchActor;
    private TextureRegion screenSwitchTexture;
    private Label timeL;
    private Label timeMeanL;
    private Sound touchDown;
    /** images[0] is the card back, images[1..20] are the card faces. */
    private TextureRegion[] images = new TextureRegion[21];
    private actor[] cards = new actor[40];
    private int[] presedCards = new int[40];
    /** 0 = easy (a wrong pair flips back), 1 = hard (a wrong pair flips back every open card). */
    private int complexity = 0;
    private int mapSize = 12;
    private int counter = 0;
    private int moveCounter = 0;
    private int gameEndCounter = 0;
    /** Columns and rows of the card grid. */
    private int[] layoutParam = new int[2];
    private CardListner listner = new CardListner();
    private int timer = 0;
    private boolean timerStart = false;
    private float timerCounter = 0.0f;
    private boolean isMusicOn = true;
    Preferences mSettings = Gdx.app.getPreferences("mySettings");
    private int displayHeight = Gdx.graphics.getHeight();
    private int displayWidth = Gdx.graphics.getWidth();
    private SpriteBatch spriteBatch = new SpriteBatch();
    private Stage stage = new Stage(new StretchViewport(displayWidth, displayHeight), spriteBatch);
    private BitmapFont font = new BitmapFont(Gdx.files.internal("font/b.fnt"), false);

    class CardListner extends ClickListener {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            for (int i = 0; i < mapSize; i++) {
                cards[i].removeListener(this);
            }
            actor act = (actor) event.getListenerActor();
            presedCards[counter] = act.cardIndex;
            act.animForward();
            if (isMusicOn) {
                cardsFlip.play(0.5f);
            }
            if (moveCounter == 1) {
                timerStart = true;
            }
            moveCounter++;
            counter++;
            gameEndCounter++;
            if (counter % 2 == 0) {
                erase();
            }
            if (gameEndCounter == mapSize) {
                new Timer().scheduleTask(new Timer.Task() {
                    @Override
                    public void run() {
                        if (gameEndCounter == mapSize) {
                            timerStart = false;
                            timer--;
                            gameEnd();
                        }
                    }
                }, 1.0f);
            }
            new Timer().scheduleTask(new Timer.Task() {
                @Override
                public void run() {
                    for (int i = 0; i < mapSize; i++) {
                        cards[i].addListener(listner);
                    }
                }
            }, 0.05f);
        }
    }

    class actor extends Actor {
        private actor act = this;
        private int cardIndex;
        private TextureRegion imageBack;
        private TextureRegion imageFront;
        private int imageIndex;
        private TextureRegion imageShow;

        public actor(TextureRegion image) {
            imageBack = image;
            imageShow = imageBack;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.draw(imageShow, getX(), getY(), getWidth(), getHeight());
        }

        public void animForward() {
            Action shrink = Actions.parallel(Actions.sizeTo(0.0f, cardSize, 0.2f), Actions.moveBy(cardSize / 2, 0.0f, 0.2f));
            Action grow = Actions.parallel(Actions.sizeTo(cardSize, cardSize, 0.2f), Actions.moveBy((-cardSize) / 2, 0.0f, 0.2f));
            Action dis = Actions.run(new Runnable() {
                @Override
                public void run() {
                    act.setTouchable(Touchable.disabled);
                }
            });
            Action frontImage = Actions.run(new Runnable() {
                @Override
                public void run() {
                    imageShow = imageFront;
                }
            });
            addAction(Actions.sequence(dis, shrink, frontImage, grow));
        }

        public void animBack() {
            Action shrink = Actions.parallel(Actions.sizeTo(0.0f, cardSize, 0.2f), Actions.moveBy(cardSize / 2, 0.0f, 0.2f));
            Action grow = Actions.parallel(Actions.sizeTo(cardSize, cardSize, 0.2f), Actions.moveBy((-cardSize) / 2, 0.0f, 0.2f));
            Action sound = Actions.run(new Runnable() {
                @Override
                public void run() {
                    if (isMusicOn) {
                        cardsFlip.play(0.25f);
                    }
                }
            });
            Action backImage = Actions.run(new Runnable() {
                @Override
                public void run() {
                    imageShow = imageBack;
                }
            });
            Action en = Actions.run(new Runnable() {
                @Override
                public void run() {
                    act.setTouchable(Touchable.enabled);
                }
            });
            addAction(Actions.delay(0.7f, Actions.sequence(sound, shrink, backImage, grow, en)));
        }
    }

    /** Simple tinted image actor: the closing doors and the fade-to-black overlay. */
    class dors extends Actor {
        private TextureRegion image;

        public dors(TextureRegion image) {
            this.image = image;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.setColor(getColor());
            batch.draw(image, getX(), getY(), getWidth(), getHeight());
            batch.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    public GameScreen(MyGame game) {
        this.game = game;
        gameAtlas = game.mainAtlas;
        backGround = new TextureRegion(gameAtlas, 540, 990, 540, 900);
        screenSwitchTexture = new TextureRegion(gameAtlas, 1648, 948, 100, 100);
        for (TextureRegion textureRegion : font.getRegions()) {
            textureRegion.getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        font.getData().setScale(displayWidth / 1080.0f);
        lang = LanguagesManager.getInstance();
        dorsOpendSound = Gdx.audio.newSound(Gdx.files.internal("data/dors_open2_1.mp3"));
        dorsClosedSound = Gdx.audio.newSound(Gdx.files.internal("data/dor_closed.mp3"));
        touchDown = Gdx.audio.newSound(Gdx.files.internal("data/click4.wav"));
        cardsFlip = Gdx.audio.newSound(Gdx.files.internal("data/cards2.mp3"));
        // 4x5 grid of 200x200 cards at (1248, 1048); the last face lives separately at (1848, 848)
        for (int i = 0; i < 20; i++) {
            images[i] = new TextureRegion(gameAtlas, 1248 + (i % 4) * 200, 1048 + (i / 4) * 200, 200, 200);
        }
        images[20] = new TextureRegion(gameAtlas, 1848, 848, 200, 200);
        dor1Texture = new TextureRegion(gameAtlas, 1080, 0, 270, 900);
        dor2Texture = new TextureRegion(gameAtlas, 1350, 0, 270, 900);
        gameAtlas.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    /** Lays the cards out (flying in from the right) and deals a random pair layout. */
    private void dealCards(float startDelay, boolean create) {
        int[] countColor = new int[20];
        int w1 = (cardSize * layoutParam[0]) + ((cardSize / 10) * (layoutParam[0] - 1));
        int h1 = (cardSize * layoutParam[1]) + ((cardSize / 10) * (layoutParam[1] - 1));
        int x = (displayWidth - w1) / 2;
        int y = (((displayHeight - h1) / 2) + h1) - cardSize;
        int cardIndex = 0;
        float n = 0.0f;
        for (int i = 0; i < layoutParam[1]; i++) {
            int y1 = y - ((cardSize + (cardSize / 10)) * i);
            for (int k = 0; k < layoutParam[0]; k++) {
                int x1 = x + ((cardSize + (cardSize / 10)) * k);
                actor card;
                if (create) {
                    card = new actor(images[0]);
                    cards[cardIndex] = card;
                    card.setSize(cardSize, cardSize);
                    stage.addActor(card);
                } else {
                    card = cards[cardIndex];
                    card.imageShow = images[0];
                    card.setTouchable(Touchable.enabled);
                }
                card.cardIndex = cardIndex;
                card.setPosition((w1 * 2) + x1, y1);
                card.addListener(listner);
                card.addAction(Actions.delay(startDelay + n, Actions.moveTo(x1, y1, 0.3f)));
                n += 0.05f;
                // Pick a random face that has been used fewer than twice
                int rand;
                do {
                    rand = (int) ((Math.random() * mapSize) / 2.0d);
                } while (countColor[rand] >= 2);
                countColor[rand]++;
                card.imageFront = images[rand + 1];
                card.imageIndex = rand;
                cardIndex++;
            }
        }
    }

    private void openDoors() {
        dor1.addAction(Actions.parallel(
                Actions.color(new Color(0.4f, 0.4f, 0.4f, 1.0f), 1.0f, Interpolation.exp5),
                Actions.sizeTo(0.0f, dor1.getHeight(), 1.0f),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        if (isMusicOn) {
                            dorsOpendSound.play();
                        }
                    }
                })));
        dor2.addAction(Actions.parallel(
                Actions.color(new Color(0.4f, 0.4f, 0.4f, 1.0f), 1.0f, Interpolation.exp5),
                Actions.sizeTo(0.0f, dor2.getHeight(), 1.0f),
                Actions.moveBy(dor2.getWidth(), 0.0f, 1.0f)));
    }

    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer(this, stage);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setCatchKey(Input.Keys.BACK, true);
        if (mSettings.contains("complexity")) {
            complexity = mSettings.getInteger("complexity", 0);
        }
        if (mSettings.contains("mapsize")) {
            mapSize = mSettings.getInteger("mapsize", 0);
        }
        if (mSettings.contains("isMusicOn")) {
            isMusicOn = mSettings.getBoolean("isMusicOn");
        }
        switch (mapSize) {
            case 12:
                layoutParam[0] = 3;
                layoutParam[1] = 4;
                break;
            case 24:
                layoutParam[0] = 4;
                layoutParam[1] = 6;
                break;
            case 40:
                layoutParam[0] = 5;
                layoutParam[1] = 8;
                break;
        }
        cardSize = displayWidth / (layoutParam[0] + 1);
        dealCards(2.0f, true);
        dor1 = new dors(dor1Texture);
        dor2 = new dors(dor2Texture);
        dor1.setSize(displayWidth / 2, displayHeight);
        dor2.setSize(displayWidth / 2, displayHeight);
        dor1.setPosition(0.0f, 0.0f);
        dor2.setPosition(displayWidth / 2, 0.0f);
        stage.addActor(dor1);
        stage.addActor(dor2);
        screenSwitchActor = new dors(screenSwitchTexture);
        screenSwitchActor.setSize(displayWidth, displayHeight);
        screenSwitchActor.setPosition(0.0f, 0.0f);
        stage.addActor(screenSwitchActor);
        screenSwitchActor.addAction(Actions.sequence(Actions.color(new Color(1.0f, 1.0f, 1.0f, 0.0f), 0.35f), Actions.removeActor()));
        dor1.addAction(Actions.delay(0.8f, Actions.parallel(
                Actions.color(new Color(0.4f, 0.4f, 0.4f, 1.0f), 1.0f, Interpolation.exp5),
                Actions.sizeTo(0.0f, dor1.getHeight(), 1.0f),
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        if (isMusicOn) {
                            dorsOpendSound.play();
                        }
                    }
                }))));
        dor2.addAction(Actions.delay(0.8f, Actions.parallel(
                Actions.color(new Color(0.4f, 0.4f, 0.4f, 1.0f), 1.0f, Interpolation.exp5),
                Actions.sizeTo(0.0f, dor2.getHeight(), 1.0f),
                Actions.moveBy(dor2.getWidth(), 0.0f, 1.0f))));
    }

    @Override
    public void render(float delta) {
        if (timerStart) {
            timerCounter += Gdx.graphics.getDeltaTime();
            if (timerCounter >= 1.0f) {
                timerCounter = 0.0f;
                timer++;
            }
        }
        spriteBatch.setProjectionMatrix(stage.getCamera().combined);
        spriteBatch.begin();
        showBG();
        spriteBatch.end();
        stage.draw();
        stage.act(Gdx.graphics.getDeltaTime());
    }

    public void showBG() {
        spriteBatch.disableBlending();
        spriteBatch.draw(backGround, 0.0f, 0.0f, displayWidth, displayHeight);
        spriteBatch.enableBlending();
    }

    /** Called after every second opened card: closes the pair if it does not match. */
    public void erase() {
        actor first = cards[presedCards[counter - 2]];
        actor second = cards[presedCards[counter - 1]];
        if (first.imageIndex == second.imageIndex) {
            return;
        }
        if (complexity == 0) {
            first.animBack();
            second.animBack();
            counter = 0;
            gameEndCounter -= 2;
        } else if (complexity == 1) {
            for (int i = 0; i < counter; i++) {
                cards[presedCards[i]].animBack();
            }
            gameEndCounter = 0;
            counter = 0;
        }
    }

    public void gameEnd() {
        int bonusMove;
        int bonusTime;
        String time = lang.getString("time");
        String move = lang.getString("move");
        String score = lang.getString("score");
        String again = lang.getString("again");
        String menu = lang.getString("menu");
        String RECORD_CASE = null;
        String RECORD_CASE_T = null;
        String RECORD_CASE_M = null;
        boolean isNewRecord = false;
        boolean isNewRecordT = false;
        boolean isNewRecordM = false;
        int borderMove = 0;
        int borderTime = 0;
        int record = 0;
        if (complexity == 0) {
            switch (mapSize) {
                case 12:
                    RECORD_CASE = "easy12";
                    RECORD_CASE_T = "easy12T";
                    RECORD_CASE_M = "easy12M";
                    borderMove = 24;
                    borderTime = 40;
                    break;
                case 24:
                    RECORD_CASE = "easy24";
                    RECORD_CASE_T = "easy24T";
                    RECORD_CASE_M = "easy24M";
                    borderMove = 60;
                    borderTime = 80;
                    break;
                case 40:
                    RECORD_CASE = "easy40";
                    RECORD_CASE_T = "easy40T";
                    RECORD_CASE_M = "easy40M";
                    borderMove = 120;
                    borderTime = 160;
                    break;
            }
        } else {
            switch (mapSize) {
                case 12:
                    RECORD_CASE = "hard12";
                    RECORD_CASE_T = "hard12T";
                    RECORD_CASE_M = "hard12M";
                    borderMove = 36;
                    borderTime = 120;
                    break;
                case 24:
                    RECORD_CASE = "hard24";
                    RECORD_CASE_T = "hard24T";
                    RECORD_CASE_M = "hard24M";
                    borderMove = 140;
                    borderTime = 300;
                    break;
                case 40:
                    RECORD_CASE = "hard40";
                    RECORD_CASE_T = "hard40T";
                    RECORD_CASE_M = "hard40M";
                    borderMove = 400;
                    borderTime = 800;
                    break;
            }
        }
        bonusMove = borderMove > moveCounter ? (borderMove - moveCounter) * 5 : 0;
        bonusTime = borderTime > timer ? borderTime - timer : 0;
        if (mSettings.contains(RECORD_CASE)) {
            record = mSettings.getInteger(RECORD_CASE, 0);
        }
        if (bonusMove + bonusTime != 0 && record < bonusMove + bonusTime) {
            isNewRecord = true;
            mSettings.putInteger(RECORD_CASE, bonusMove + bonusTime);
            mSettings.flush();
        }
        int recordT = mSettings.contains(RECORD_CASE_T) ? mSettings.getInteger(RECORD_CASE_T, 0) : 60000;
        if (recordT > timer) {
            isNewRecordT = true;
            mSettings.putInteger(RECORD_CASE_T, timer);
            mSettings.flush();
        }
        int recordM = mSettings.contains(RECORD_CASE_M) ? mSettings.getInteger(RECORD_CASE_M, 0) : 1000;
        if (recordM > moveCounter) {
            isNewRecordM = true;
            mSettings.putInteger(RECORD_CASE_M, moveCounter);
            mSettings.flush();
        }
        Label.LabelStyle white = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle yellow = new Label.LabelStyle(font, Color.YELLOW);
        String timeMean = MyGame.formatTime(timer);
        String moveMean = Integer.toString(moveCounter);
        String scoreMean = Integer.toString(bonusTime + bonusMove);
        // Captions start off-screen on the left, values on the right; both slide in.
        timeL = new Label(time, white);
        timeL.setPosition((((-displayWidth) / 2) - timeL.getWidth()) - (displayWidth / 20), (0.75f * displayHeight) + (0.6f * timeL.getHeight()));
        moveL = new Label(move, white);
        moveL.setPosition((((-displayWidth) / 2) - moveL.getWidth()) - (displayWidth / 20), (0.75f * displayHeight) - (moveL.getHeight() / 2.0f));
        scoreL = new Label(score, white);
        scoreL.setPosition((((-displayWidth) / 2) - scoreL.getWidth()) - (displayWidth / 20), (0.75f * displayHeight) - (1.6f * scoreL.getHeight()));
        timeMeanL = new Label(timeMean, isNewRecordT ? yellow : white);
        timeMeanL.setPosition(((3.0f * displayWidth) / 2.0f) + (displayWidth / 20), (0.75f * displayHeight) + (0.6f * timeMeanL.getHeight()));
        moveMeanL = new Label(moveMean, isNewRecordM ? yellow : white);
        moveMeanL.setPosition(((3.0f * displayWidth) / 2.0f) + (displayWidth / 20), (0.75f * displayHeight) - (moveMeanL.getHeight() / 2.0f));
        scoreMeanL = new Label(scoreMean, isNewRecord ? yellow : white);
        scoreMeanL.setPosition(((3.0f * displayWidth) / 2.0f) + (displayWidth / 20), (0.75f * displayHeight) - (1.6f * scoreMeanL.getHeight()));
        menuL = new Label(menu, white);
        menuL.setSize(menuL.getHeight(), menuL.getHeight());
        menuL.setPosition((((-3.0f) * displayWidth) / 4.0f) - (menuL.getWidth() / 2.0f), 0.1f * displayHeight);
        againL = new Label(again, white);
        againL.setPosition(((7.0f * displayWidth) / 4.0f) - (againL.getWidth() / 2.0f), 0.1f * displayHeight);
        menuL.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                pressLabel((Label) event.getListenerActor());
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                releaseLabel((Label) event.getListenerActor());
                screenSwitchActor = new dors(screenSwitchTexture);
                screenSwitchActor.setSize(displayWidth, displayHeight);
                screenSwitchActor.setPosition(0.0f, 0.0f);
                screenSwitchActor.setColor(new Color(1.0f, 1.0f, 1.0f, 0.0f));
                stage.addActor(screenSwitchActor);
                screenSwitchActor.addAction(Actions.sequence(
                        Actions.color(new Color(1.0f, 1.0f, 1.0f, 1.0f), 0.35f),
                        Actions.run(new Runnable() {
                            @Override
                            public void run() {
                                game.setScreen(game.firstScreen);
                                removeResultLabels();
                            }
                        }),
                        Actions.removeActor()));
            }
        });
        againL.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                pressLabel((Label) event.getListenerActor());
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                releaseLabel((Label) event.getListenerActor());
                timeL.addAction(Actions.moveTo(timeL.getX() - displayWidth, timeL.getY(), 0.3f));
                moveL.addAction(Actions.moveTo(moveL.getX() - displayWidth, moveL.getY(), 0.3f));
                scoreL.addAction(Actions.moveTo(scoreL.getX() - displayWidth, scoreL.getY(), 0.3f));
                timeMeanL.addAction(Actions.moveTo(timeMeanL.getX() + displayWidth, timeMeanL.getY(), 0.3f));
                moveMeanL.addAction(Actions.moveTo(moveMeanL.getX() + displayWidth, moveMeanL.getY(), 0.3f));
                scoreMeanL.addAction(Actions.moveTo(scoreMeanL.getX() + displayWidth, scoreMeanL.getY(), 0.3f));
                menuL.addAction(Actions.moveTo(menuL.getX() - displayWidth, menuL.getY(), 0.3f));
                againL.addAction(Actions.moveTo(againL.getX() + displayWidth, againL.getY(), 0.3f));
                new Timer().scheduleTask(new Timer.Task() {
                    @Override
                    public void run() {
                        update();
                    }
                }, 0.3f);
            }
        });
        stage.addActor(timeL);
        stage.addActor(moveL);
        stage.addActor(scoreL);
        stage.addActor(timeMeanL);
        stage.addActor(moveMeanL);
        stage.addActor(scoreMeanL);
        stage.addActor(againL);
        stage.addActor(menuL);
        if (isMusicOn) {
            dorsClosedSound.play();
        }
        // Close the doors, then slide the results in one by one
        dor1.addAction(Actions.parallel(Actions.color(Color.WHITE, 1.0f, Interpolation.exp5In), Actions.sizeTo(displayWidth / 2, dor1.getHeight(), 1.0f)));
        dor2.addAction(Actions.parallel(Actions.color(Color.WHITE, 1.0f, Interpolation.exp5In), Actions.sizeTo(displayWidth / 2, dor2.getHeight(), 1.0f), Actions.moveTo(displayWidth / 2, 0.0f, 1.0f)));
        slideIn(timeL, displayWidth, 1.5f, true);
        slideIn(timeMeanL, -displayWidth, 1.75f, true);
        slideIn(moveL, displayWidth, 2.0f, true);
        slideIn(moveMeanL, -displayWidth, 2.25f, true);
        slideIn(scoreL, displayWidth, 2.5f, true);
        slideIn(scoreMeanL, -displayWidth, 2.75f, true);
        slideIn(menuL, displayWidth, 3.15f, true);
        slideIn(againL, -displayWidth, 3.15f, false);
    }

    private void slideIn(Label label, float dx, float delay, boolean click) {
        Action move = Actions.moveTo(label.getX() + dx, label.getY(), 0.25f);
        if (click) {
            move = Actions.sequence(move, Actions.run(new Runnable() {
                @Override
                public void run() {
                    if (isMusicOn) {
                        touchDown.play();
                    }
                }
            }));
        }
        label.addAction(Actions.delay(delay, move));
    }

    private void pressLabel(Label l) {
        if (isMusicOn) {
            touchDown.play();
        }
        font.getData().setScale(font.getScaleX() + 0.15f);
        l.setStyle(new Label.LabelStyle(font, Color.DARK_GRAY));
        l.setPosition(l.getX() - (0.075f * l.getWidth()), l.getY());
    }

    private void releaseLabel(Label l) {
        font.getData().setScale(font.getScaleX() - 0.15f);
        l.setStyle(new Label.LabelStyle(font, Color.WHITE));
        l.setPosition(l.getX() + (0.075f * l.getWidth()), l.getY());
    }

    private void removeResultLabels() {
        timeL.remove();
        timeMeanL.remove();
        moveL.remove();
        moveMeanL.remove();
        scoreL.remove();
        scoreMeanL.remove();
        menuL.remove();
        againL.remove();
    }

    @Override
    public void dispose() {
        spriteBatch.dispose();
        gameAtlas.dispose();
        font.dispose();
        dorsOpendSound.dispose();
        dorsClosedSound.dispose();
        touchDown.dispose();
        cardsFlip.dispose();
        stage.dispose();
    }

    /** Restarts the round ("Again" button). */
    public void update() {
        counter = 0;
        moveCounter = 0;
        gameEndCounter = 0;
        timer = 0;
        timerCounter = 0.0f;
        dealCards(1.5f, false);
        openDoors();
    }

    @Override
    public void hide() {
        if (gameEndCounter == mapSize && timeL != null) {
            removeResultLabels();
        }
        counter = 0;
        moveCounter = 0;
        gameEndCounter = 0;
        timer = 0;
        timerCounter = 0.0f;
        timerStart = false;
        dor1.remove();
        dor2.remove();
        for (int i = 0; i < mapSize; i++) {
            cards[i].remove();
        }
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

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.BACK) {
            screenSwitchActor = new dors(screenSwitchTexture);
            screenSwitchActor.setSize(displayWidth, displayHeight);
            screenSwitchActor.setPosition(0.0f, 0.0f);
            screenSwitchActor.setColor(new Color(1.0f, 1.0f, 1.0f, 0.0f));
            stage.addActor(screenSwitchActor);
            screenSwitchActor.addAction(Actions.sequence(
                    Actions.color(new Color(1.0f, 1.0f, 1.0f, 1.0f), 0.35f),
                    Actions.run(new Runnable() {
                        @Override
                        public void run() {
                            game.setScreen(game.firstScreen);
                        }
                    }),
                    Actions.removeActor()));
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

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
