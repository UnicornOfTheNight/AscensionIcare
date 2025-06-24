package com.example.superjump;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameOverActivity extends AppCompatActivity {

    private TextView gameOverText;
    private Button returnHomeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        gameOverText = findViewById(R.id.gameOverText);
        returnHomeButton = findViewById(R.id.returnHomeButton);

        // Récupération du score total depuis l'intent
        int totalScore = getIntent().getIntExtra("totalScore", 0);
        gameOverText.setText("Score total : " + totalScore);

        // Retourner à l'accueil
        returnHomeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent homeIntent = new Intent(GameOverActivity.this, MainActivity.class);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                homeIntent.putExtra("goToTab", 0); // si tu veux ouvrir un onglet particulier
                startActivity(homeIntent);
                finish();
            }
        });
    }
}
