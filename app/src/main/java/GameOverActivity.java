import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.superjump.R;

public class GameOverActivity extends AppCompatActivity {

    private TextView gameOverText;
    private Button returnHomeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over);

        // Initialisation des éléments
        gameOverText = findViewById(R.id.gameOverText);
        returnHomeButton = findViewById(R.id.returnHomeButton);

        // Lancer l'animation de fin de jeu
        animateGameOverTransition();

        // Afficher le bouton pour retourner à l'accueil
        returnHomeButton.setVisibility(View.VISIBLE);

        // Gestion du clic sur le bouton de retour à l'accueil
        returnHomeButton.setOnClickListener(v -> returnToHomeScreen());
    }

    // Fonction pour animer la transition de fin de jeu
    private void animateGameOverTransition() {
        // Animation de fondu (fade-out) du texte
        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(gameOverText, "alpha", 1f, 0f);
        fadeOut.setDuration(1000); // 1 seconde
        fadeOut.start();
    }

    // Fonction pour revenir à l'écran d'accueil
    private void returnToHomeScreen() {
        // Exemple : naviguer vers l'activité d'accueil
        // startActivity(new Intent(GameOverActivity.this, HomeActivity.class));
        finish(); // Fermer l'activité actuelle et retourner à l'accueil
    }
}
