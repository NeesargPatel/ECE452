package com.example.neesarg.ece452;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Button singlePlayerButton = findViewById(R.id.single_player_button);
        singlePlayerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onSingleBtn(v);
            }
        });

        final Button multiPlayerButton = findViewById(R.id.multi_player_button);
        multiPlayerButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                onBlueBtn(v);
            }
        });
    }

    @Override
    protected  void onStart() {
        super.onStart();
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );
    }

    public void onSingleBtn(View view) {
        Intent singlePlayer = new Intent(this, GameplayActivity.class);
        singlePlayer.putExtra("gameMode", 0);
        startActivity(singlePlayer);
    }

    public void onBlueBtn(View view) {
        Intent bluetooth = new Intent(this, LobbyActivity.class);
        bluetooth.putExtra("gameMode", 1);
        startActivity(bluetooth);
    }
}