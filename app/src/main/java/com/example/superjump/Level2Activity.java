package com.example.superjump;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class Level2Activity extends AppCompatActivity {

    private Button pauseButton, resumeButton, quitButton;
    private LinearLayout pauseMenu;
    private TextView timerText;
    private boolean isManuallyPaused = false;

    // Variables pour le chronomètre
    private Handler timerHandler;
    private Runnable timerRunnable;
    private long startTime;
    private long pausedTime = 0;
    private boolean isTimerRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level2);

        pauseButton = findViewById(R.id.pauseButton);
        resumeButton = findViewById(R.id.resumeButton);
        quitButton = findViewById(R.id.quitButton);
        pauseMenu = findViewById(R.id.pauseMenu);
        timerText = findViewById(R.id.timerText);

        // Initialiser le chronomètre
        initTimer();
        startTimer();

        pauseButton.setOnClickListener(v -> {
            isManuallyPaused = true;
            pauseTimer();
            onPause(); // Appelle pause manuelle
        });

        resumeButton.setOnClickListener(v -> {
            resumeTimer();
            resumeGame();
        });

        quitButton.setOnClickListener(v -> {
            isManuallyPaused = false;
            stopTimer();
            Intent intent = new Intent(Level2Activity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            //intent.putExtra("goToTab", 0); // ouvrir l'onglet "Jouer"
            startActivity(intent);
            finish();
        });
    }

    private void initTimer() {
        timerHandler = new Handler();
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (isTimerRunning) {
                    long currentTime = System.currentTimeMillis();
                    long elapsedTime = currentTime - startTime + pausedTime;
                    updateTimerDisplay(elapsedTime);
                    timerHandler.postDelayed(this, 10); // Mise à jour toutes les 10ms
                }
            }
        };
    }

    private void startTimer() {
        if (!isTimerRunning) {
            startTime = System.currentTimeMillis();
            isTimerRunning = true;
            timerHandler.post(timerRunnable);
        }
    }

    private void pauseTimer() {
        if (isTimerRunning) {
            isTimerRunning = false;
            pausedTime += System.currentTimeMillis() - startTime;
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void resumeTimer() {
        if (!isTimerRunning) {
            startTime = System.currentTimeMillis();
            isTimerRunning = true;
            timerHandler.post(timerRunnable);
        }
    }

    private void stopTimer() {
        isTimerRunning = false;
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }

    private void updateTimerDisplay(long elapsedTime) {
        int minutes = (int) (elapsedTime / 60000);
        int seconds = (int) ((elapsedTime % 60000) / 1000);
        int milliseconds = (int) ((elapsedTime % 1000) / 10);

        String timeText = String.format("%02d:%02d:%02d", minutes, seconds, milliseconds);
        timerText.setText(timeText);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isManuallyPaused) {
            pauseMenu.setVisibility(View.VISIBLE);
            pauseButton.setVisibility(View.GONE);
        } else {
            // Pause automatique (changement d'activité)
            pauseTimer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isManuallyPaused) {
            pauseMenu.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            resumeTimer();
        }
    }

    private void resumeGame() {
        isManuallyPaused = false;
        pauseMenu.setVisibility(View.GONE);
        pauseButton.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
    }
}