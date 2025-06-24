package com.example.superjump;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class Level2Activity extends AppCompatActivity {

    private Button pauseButton, resumeButton, quitButton;
    private LinearLayout pauseMenu;
    private boolean isManuallyPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level2);

        pauseButton = findViewById(R.id.pauseButton);
        resumeButton = findViewById(R.id.resumeButton);
        quitButton = findViewById(R.id.quitButton);
        pauseMenu = findViewById(R.id.pauseMenu);

        pauseButton.setOnClickListener(v -> {
            isManuallyPaused = true;
            onPause(); // Appelle pause manuelle
        });

        resumeButton.setOnClickListener(v -> resumeGame());

        quitButton.setOnClickListener(v -> {
            isManuallyPaused = false;
            Intent intent = new Intent(Level2Activity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            //intent.putExtra("goToTab", 0); // ouvrir l’onglet "Jouer"
            startActivity(intent);
            finish();
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isManuallyPaused) {
            pauseMenu.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isManuallyPaused) {
            pauseMenu.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
        }
    }

    private void resumeGame() {
        isManuallyPaused = false;
        pauseMenu.setVisibility(View.GONE);
        pauseButton.setVisibility(View.VISIBLE);
    }
}
