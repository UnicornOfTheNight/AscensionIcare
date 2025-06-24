package com.example.superjump;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator; // Pour un mouvement plus doux
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class Level1Activity extends AppCompatActivity {

    private ImageView characterImageView;
    private ConstraintLayout gameAreaLayout;

    private float targetCharacterX;
    private float targetCharacterY;

    private ValueAnimator xAnimator;
    private ValueAnimator yAnimator;

    private static final long ANIMATION_DURATION = 300; // Durée de l'animation en ms pour atteindre la cible

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level1);

        characterImageView = findViewById(R.id.imageView3);
        gameAreaLayout = findViewById(R.id.main);

        // Attendre que le layout soit dessiné pour initialiser targetX/Y
        characterImageView.post(() -> {
            targetCharacterX = characterImageView.getX();
            targetCharacterY = characterImageView.getY();
        });

        gameAreaLayout.setOnTouchListener((v, event) -> {
            float currentTouchX = event.getX();
            float currentTouchY = event.getY();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    // Mettre à jour la position cible vers laquelle le personnage doit se déplacer
                    // Centrer le personnage sur le doigt
                    targetCharacterX = currentTouchX - (characterImageView.getWidth() / 2f);
                    targetCharacterY = currentTouchY - (characterImageView.getHeight() / 2f);

                    // Limiter la cible aux bords de gameAreaLayout
                    targetCharacterX = Math.max(0, Math.min(targetCharacterX, gameAreaLayout.getWidth() - characterImageView.getWidth()));
                    targetCharacterY = Math.max(0, Math.min(targetCharacterY, gameAreaLayout.getHeight() - characterImageView.getHeight()));

                    // Démarrer ou mettre à jour les animations pour atteindre la nouvelle cible
                    animateCharacterTo(targetCharacterX, targetCharacterY);
                    break;

                case MotionEvent.ACTION_UP:
                    // Optionnel : Que faire quand le doigt est relevé ?
                    // Le personnage s'arrêtera à sa dernière position animée.
                    // Si vous voulez qu'il continue vers la dernière cible touchée : ne rien faire de spécial ici.
                    // Si vous voulez qu'il s'arrête immédiatement :
                    // if (xAnimator != null) xAnimator.cancel();
                    // if (yAnimator != null) yAnimator.cancel();
                    break;
            }
            return true;
        });
    }

    private void animateCharacterTo(float targetX, float targetY) {
        // Animation pour X
        if (xAnimator != null && xAnimator.isRunning()) {
            xAnimator.cancel(); // Annule l'animation précédente si elle est en cours
        }
        xAnimator = ValueAnimator.ofFloat(characterImageView.getX(), targetX);
        xAnimator.setDuration(ANIMATION_DURATION);
        xAnimator.setInterpolator(new DecelerateInterpolator()); // Pour un mouvement doux
        xAnimator.addUpdateListener(animation ->
                characterImageView.setX((Float) animation.getAnimatedValue())
        );
        xAnimator.start();

        // Animation pour Y
        if (yAnimator != null && yAnimator.isRunning()) {
            yAnimator.cancel(); // Annule l'animation précédente
        }
        yAnimator = ValueAnimator.ofFloat(characterImageView.getY(), targetY);
        yAnimator.setDuration(ANIMATION_DURATION);
        yAnimator.setInterpolator(new DecelerateInterpolator());
        yAnimator.addUpdateListener(animation ->
                characterImageView.setY((Float) animation.getAnimatedValue())
        );
        yAnimator.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Arrêter les animations si l'activité est mise en pause pour éviter les fuites
        if (xAnimator != null && xAnimator.isRunning()) {
            xAnimator.cancel();
        }
        if (yAnimator != null && yAnimator.isRunning()) {
            yAnimator.cancel();
        }
    }
}