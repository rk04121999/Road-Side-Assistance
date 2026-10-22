
package com.example.roadsideassistance;

import android.content.Intent;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Splash_Activity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splashLogo);
        TextView title = findViewById(R.id.splashTitle);
        TextView tagline = findViewById(R.id.splashTagline);

        Animation zoom = AnimationUtils.loadAnimation(this, R.anim.splash_zoom);
        Animation fade = AnimationUtils.loadAnimation(this, android.R.anim.fade_in);

        logo.startAnimation(zoom);
        title.startAnimation(fade);
        tagline.startAnimation(fade);

        logo.postDelayed(() -> {
            Intent intent = new Intent(
                    Splash_Activity.this,
                    GetStartedActivity.class
            );
            startActivity(intent);
            finish();
        }, 2500);
    }
}
