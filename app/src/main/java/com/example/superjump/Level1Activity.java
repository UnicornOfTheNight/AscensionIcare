package com.example.superjump;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class Level1Activity extends AppCompatActivity implements SensorEventListener {

    private ImageView characterImageView;
    private ConstraintLayout gameAreaLayout;

    // Variables pour l'accéléromètre
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float MOVEMENT_SENSITIVITY = 10.0f;
    private static final float ACCELEROMETER_FILTER_ALPHA = 0.1f; // Optionnel

    // Variables pour le saut
    private ValueAnimator jumpAnimator;
    private float groundY; // Position Y du sol
    private static final float JUMP_HEIGHT = 1000f; // Hauteur du saut en pixels
    private static final long JUMP_DURATION = 400; // Durée pour monter ou descendre (total 1s)
    private boolean isJumping = false;

    private ValueAnimator xPositionAnimator;
    private static final long HORIZONTAL_ANIMATION_DURATION = 50; // Durée courte pour la réactivité

    // Supprimé les variables liées au toucher et aux ValueAnimator X/Y précédents
    // pour se concentrer sur l'accéléromètre pour X et le saut pour Y.

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level1);

        characterImageView = findViewById(R.id.imageView_perso);
        gameAreaLayout = findViewById(R.id.main);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // Attendre que le layout soit dessiné pour obtenir les dimensions correctes
        // et initialiser la position du personnage et le "sol"
        gameAreaLayout.post(() -> {
            // Positionner le personnage initialement en bas de la gameArea
            // (ou à une position de départ souhaitée)
            groundY = gameAreaLayout.getHeight() - characterImageView.getHeight() - 135f; // 50f est une marge du bas
            characterImageView.setX(gameAreaLayout.getWidth() / 2f - characterImageView.getWidth() / 2f); // Centrer horizontalement
            characterImageView.setY(groundY);

            // Déclencher le premier saut (ou le configurer pour se répéter)
            // Pour un seul saut au démarrage:
            performJump();

            // Si vous voulez que le personnage saute continuellement (exemple simple)
             Handler handler = new Handler();
             Runnable repetitiveJump = new Runnable() {
                 @Override
                 public void run() {
                     if (!isJumping) {
                         performJump();
                     }
                     handler.postDelayed(this, 0); // Saute toutes les X secondes
                 }
             };
             handler.post(repetitiveJump);

            // Si le saut est déclenché par un toucher (exemple)
            /*
            gameAreaLayout.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (!isJumping) {
                        performJump();
                    }
                }
                return true; // Consomme l'événement tactile
            });
            */
        });
    }

    private void performJump() {
        if (isJumping) {
            return; // Déjà en train de sauter
        }
        isJumping = true;

        // Animateur pour la montée
        ValueAnimator riseAnimator = ValueAnimator.ofFloat(characterImageView.getY(), groundY - JUMP_HEIGHT);
        riseAnimator.setDuration(JUMP_DURATION);
        riseAnimator.setInterpolator(new DecelerateInterpolator()); // Monte rapidement puis ralentit
        riseAnimator.addUpdateListener(animation -> {
            characterImageView.setY((Float) animation.getAnimatedValue());
        });

        // Animateur pour la descente
        ValueAnimator fallAnimator = ValueAnimator.ofFloat(groundY - JUMP_HEIGHT, groundY);
        fallAnimator.setDuration(JUMP_DURATION);
        fallAnimator.setInterpolator(new AccelerateInterpolator()); // Descend lentement puis accélère
        fallAnimator.addUpdateListener(animation -> {
            characterImageView.setY((Float) animation.getAnimatedValue());
        });

        // Enchaîner les animations
        riseAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                fallAnimator.start();
            }
        });

        fallAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                isJumping = false;
                characterImageView.setY(groundY); // S'assurer qu'il est bien au sol
                // Ici, vous pourriez déclencher le prochain saut si vous voulez un saut continu
                // ou attendre une autre action.
            }
        });

        // Démarrer la montée
        riseAnimator.start();
        jumpAnimator = riseAnimator; // Garder une référence si besoin de l'annuler
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        if (xPositionAnimator != null && xPositionAnimator.isRunning()) {
            xPositionAnimator.cancel();
        }
        // Annuler l'animation de saut si l'activité est mise en pause
        if (jumpAnimator != null && jumpAnimator.isRunning()) {
            jumpAnimator.cancel();
            isJumping = false; // Réinitialiser l'état
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // ... (calcul de rawSensorValueX et currentFilteredAccX si vous utilisez le filtre) ...
            float rawSensorValueX;
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                rawSensorValueX = event.values[0];
            } else {
                rawSensorValueX = event.values[1];
            }
            // Optionnel: appliquer filtre si souhaité
            // currentFilteredAccX = ACCELEROMETER_FILTER_ALPHA * rawSensorValueX + (1 - ACCELEROMETER_FILTER_ALPHA) * currentFilteredAccX;
            // float valueToUseForMovement = currentFilteredAccX; // Ou rawSensorValueX si pas de filtre

            float movement = -rawSensorValueX * MOVEMENT_SENSITIVITY; // Utilisez la valeur appropriée
            float targetX = characterImageView.getX() + movement;

            // Limiter le mouvement aux bords
            float minX = 0;
            float maxX = gameAreaLayout.getWidth() - characterImageView.getWidth();
            targetX = Math.max(minX, Math.min(targetX, maxX));

            animateCharacterToX(targetX);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Peut être ignoré
    }

    private void animateCharacterToX(float targetX) {
        if (xPositionAnimator != null && xPositionAnimator.isRunning()) {
            xPositionAnimator.cancel(); // Annule l'animation en cours pour démarrer la nouvelle
        }
        xPositionAnimator = ValueAnimator.ofFloat(characterImageView.getX(), targetX);
        xPositionAnimator.setDuration(HORIZONTAL_ANIMATION_DURATION);
        // Utiliser un Interpolator linéaire ou un DecelerateInterpolator pour une sensation naturelle
        xPositionAnimator.setInterpolator(new DecelerateInterpolator());
        xPositionAnimator.addUpdateListener(animation ->
                characterImageView.setX((Float) animation.getAnimatedValue())
        );
        xPositionAnimator.start();
    }
    // Si vous aviez des animateurs X/Y spécifiques avant, ils peuvent être supprimés
    // ou adaptés si le saut doit aussi affecter X (ce qui n'est pas le cas dans cette demande)
}