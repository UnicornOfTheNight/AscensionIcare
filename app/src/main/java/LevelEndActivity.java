import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.superjump.R;

public class LevelEndActivity extends AppCompatActivity {

    private TextView levelCompletedText;
    private Button nextLevelButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_end);

        // Initialisation des éléments
        levelCompletedText = findViewById(R.id.levelCompletedText);
        nextLevelButton = findViewById(R.id.nextLevelButton);

        // Lancer l'animation de fin de niveau
        animateLevelTransition();

        // Afficher le bouton pour le niveau suivant
        nextLevelButton.setVisibility(View.VISIBLE);

        // Gestion du clic sur le bouton de niveau suivant
        nextLevelButton.setOnClickListener(v -> goToNextLevel());
    }

    // Fonction pour animer la transition de fin de niveau
    private void animateLevelTransition() {
        // Animation de fondu (fade-out) du texte
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(levelCompletedText, "alpha", 1f, 0f);
        fadeOut.setDuration(1000); // 1 seconde
        fadeOut.start();
    }

    // Fonction pour passer au niveau suivant
    private void goToNextLevel() {
        // Ici, on pourrait naviguer vers l'activité du niveau suivant
        // Exemple : startActivity(new Intent(LevelEndActivity.this, NextLevelActivity.class));
        finish(); // Fermer l'activité actuelle et retourner à la précédente
    }
}
