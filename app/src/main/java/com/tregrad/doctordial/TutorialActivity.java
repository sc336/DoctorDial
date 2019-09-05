package com.tregrad.doctordial;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.RelativeLayout;

public class TutorialActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Intent handling
        if(getIntent().getBooleanExtra("EXIT", false)) finish();
        final int themeId = getIntent().getIntExtra("THEME_ID", R.style.splashTheme1);
        setTheme(themeId);             //*|*\*-*/Look here!


        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_tutorial);

        final Activity splash = this;
        final RelativeLayout splashLayout = ((RelativeLayout)findViewById(R.id.tutorialLayout));

        findViewById(R.id.tutorialLayout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: create Drawable resource xml for splash screens to scale properly (Bitmap, gravity fill?)
                //TODO: figure out how to start main activity in the background, and switch to it later
                //  -OR maybe start the splash activity at the top of MainActivity's onCreate, closing it after
                int rid = nextTheme(themeId);
                Intent intent;
                if(rid == 0) {
                    intent = new Intent(getBaseContext(), MainActivity.class);
                    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    intent.putExtra("LAUNCH_TUTORIAL", false);

                    /*intent = new Intent(getBaseContext(), SplashActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("Exit", true);*/
                }
                else {
                    intent = new Intent(getBaseContext(), TutorialActivity.class);
                    intent.putExtra("THEME_ID", rid); }

                startActivity(intent);
                //finish();
            }
        });

    }

    private int nextTheme(int themeId) {
        switch (themeId) {
            case R.style.splashTheme1:
                return R.style.splashTheme2;
            case R.style.splashTheme2:
                return R.style.splashTheme3;
            case R.style.splashTheme3:
                return R.style.splashTheme4;
            case R.style.splashTheme4:
                return R.style.splashTheme5;
        }
        return 0;
    }
}
