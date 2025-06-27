package com.example.superjump;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class LevelEndActivity extends AppCompatActivity {
    public static Class<?> toGo;
    public static String score = "";
    private TextView textScore;
    private Button buttonReplay, buttonHome, buttonNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_end);

        textScore = findViewById(R.id.textScore);
        buttonReplay = findViewById(R.id.buttonReplay);
        buttonHome = findViewById(R.id.buttonHome);
        buttonNext = findViewById(R.id.buttonNext);

        // Récupérer le score depuis l'intent
        //int score = getIntent().getIntExtra("score", 0);
        textScore.setText("Score : " + score);

        // Bouton "Rejouer" → redirige vers l'accueil
        buttonReplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent replayIntent = new Intent(LevelEndActivity.this, MainActivity.class);
                replayIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                replayIntent.putExtra("goToTab", 0); // facultatif
                startActivity(replayIntent);
                finish();
            }
        });

        // Bouton "Accueil" → redirige aussi vers l'accueil
        buttonHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(LevelEndActivity.this, MainActivity.class);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                homeIntent.putExtra("goToTab", 0); // facultatif
                startActivity(homeIntent);
                finish();
            }
        });

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(LevelEndActivity.this, toGo);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                homeIntent.putExtra("goToTab", 0); // facultatif
                startActivity(homeIntent);
                finish();
            }
        });
    }
}
