package com.example.superjump;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class Level2Activity extends AppCompatActivity {

    private Button pauseButton, resumeButton;
    private LinearLayout pauseMenu;
    private boolean isManuallyPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level2);

        // Liaison avec les éléments de l'interface
        pauseButton = findViewById(R.id.pauseButton);
        resumeButton = findViewById(R.id.resumeButton);
        pauseMenu = findViewById(R.id.pauseMenu);

        // Bouton Pause
        pauseButton.setOnClickListener(v -> {
            isManuallyPaused = true;
            onPause(); // Appel manuel
        });

        // Bouton "Retourner"
        resumeButton.setOnClickListener(v -> resumeGame());
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isManuallyPaused) {
            pauseMenu.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);
            // Ici : arrêter animations, musique, timers...
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isManuallyPaused) {
            // L'app revient sans que ce soit une pause manuelle => continuer le jeu normalement
            pauseMenu.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            // Reprendre animations, timers...
        }
    }

    private void resumeGame() {
        isManuallyPaused = false;
        pauseMenu.setVisibility(View.GONE);
        pauseButton.setVisibility(View.VISIBLE);
        // Relancer timers, animations, etc.
    }
}
