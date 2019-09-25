package com.example.xchanger;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        TextView tx = findViewById(R.id.logo);
        ImageView iv = findViewById(R.id.iv);
        Animation myanim= AnimationUtils.loadAnimation(this,R.anim.myanim);
        iv.setAnimation(myanim);
        tx.setAnimation(myanim);
        Typeface custom_font = Typeface.createFromAsset(getAssets(), "font/Anurati-Regular.otf");
        tx.setTypeface(custom_font);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                startActivity(new Intent(splash.this, MainActivity.class));

            }
        },2000);
    }
}
