package com.cacao.libgdxgame;

import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class MainActivity extends AndroidApplication implements IActivityRequestHandler {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.useAccelerometer = false;
        config.useCompass = false;
        config.useWakelock = false;
        config.useImmersiveMode = true;
        initialize(new MyGame(this), config);
    }

    @Override
    public void showAdMob(boolean show) {
        // The original build showed an AdMob banner here (ad unit ca-app-pub-1192341738902231/5308570509).
        // The 2014 Play Services SDK it used no longer works, so ads are disabled.
    }
}
