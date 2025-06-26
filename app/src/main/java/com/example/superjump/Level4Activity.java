package com.example.superjump;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.List;
import java.util.Random;

public class Level4Activity extends AppCompatActivity implements SensorEventListener {

    private ConstraintLayout gameLayout;
    private ImageView player;
    private ImageView backgroundImage;
    private TextView introTextView;
    private Handler handler = new Handler();
    private Random random = new Random();

    private final int ENEMY_SIZE = 200;
    private final int ENEMY_COUNT = 5;
    private final int SPAWN_DELAY = 1500;

    // === VARIABLES POUR L'ARRIÈRE-PLAN MOBILE ===
    private Handler backgroundHandler = new Handler();
    private Runnable backgroundRunnable;
    private boolean isBackgroundMoving = false;
    private float backgroundY = 0f;
    private final float BACKGROUND_SPEED = 18f; // Vitesse de défilement

    // === AJOUT POUR LES PLATEFORMES ===
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private PlatformCreationHelper platformCreator;
    private List<ImageView> activePlatforms;
    private CharacterMovementHelper characterMovementHelper;
    private final Handler jumpHandler = new Handler();
    private Runnable repetitiveJumpRunnable;

    // === VARIABLES POUR LE TEXTE D'INTRODUCTION ===
    private boolean gameStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level4);

        gameLayout = findViewById(R.id.main);
        player = findViewById(R.id.imageView_perso);
        backgroundImage = findViewById(R.id.imageView_background);
        introTextView = findViewById(R.id.introTextView);

        gameLayout.post(() -> {
            // === AFFICHAGE DU TEXTE D'INTRODUCTION ===
            showIntroText();
        });

        // === INITIALISATION DU CAPTEUR ===
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void showIntroText() {
        // Rendre le texte visible
        introTextView.setVisibility(View.VISIBLE);

        // Animation d'apparition
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(1000);
        introTextView.startAnimation(fadeIn);

        // Masquer le texte après 7 secondes et démarrer le jeu
        handler.postDelayed(() -> {
            // Animation de disparition
            AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeOut.setDuration(1000);
            fadeOut.setAnimationListener(new android.view.animation.Animation.AnimationListener() {
                @Override
                public void onAnimationStart(android.view.animation.Animation animation) {}

                @Override
                public void onAnimationEnd(android.view.animation.Animation animation) {
                    introTextView.setVisibility(View.GONE);
                    startGame();
                }

                @Override
                public void onAnimationRepeat(android.view.animation.Animation animation) {}
            });
            introTextView.startAnimation(fadeOut);
        }, 7000);
    }

    private void startGame() {
        gameStarted = true;

        // === DÉMARRAGE DE L'ARRIÈRE-PLAN MOBILE ===
        startBackgroundMovement();

        // === INITIALISATION DES PLATEFORMES ===
        characterMovementHelper = new CharacterMovementHelper(player, gameLayout);
        characterMovementHelper.updateGroundY();

        platformCreator = new PlatformCreationHelper(Level4Activity.this, gameLayout, player);
        activePlatforms = platformCreator.creerPlateformes();

        repetitiveJumpRunnable = new Runnable() {
            @Override
            public void run() {
                if (characterMovementHelper != null && !characterMovementHelper.getIsJumping()) {
                    characterMovementHelper.performJump();
                }
                jumpHandler.postDelayed(this, 800);
            }
        };
        jumpHandler.post(repetitiveJumpRunnable); // Saut immédiat au lancement

        // Délai de 2 secondes avant de commencer à faire apparaître les ennemis
        handler.postDelayed(() -> spawnEnemies(), 2000);
    }

    // === MÉTHODES POUR L'ARRIÈRE-PLAN MOBILE ===
    // Remplacez votre méthode startBackgroundMovement() par celle-ci :

    private void startBackgroundMovement() {
        if (!isBackgroundMoving) {
            isBackgroundMoving = true;
            backgroundRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isBackgroundMoving) {
                        // Hauteur de l'écran et de l'image de fond
                        int screenHeight = gameLayout.getHeight();
                        int backgroundImageHeight = findViewById(R.id.imageView_background).getHeight(); // Selon votre XML

                        // Calcul du déplacement maximum possible vers le bas
                        // L'image peut descendre jusqu'à ce que son haut soit aligné avec le bas de l'écran
                        float maxDownMovement = backgroundImageHeight - screenHeight;

                        // Déplacer l'arrière-plan vers le BAS (valeur positive)
                        backgroundY += BACKGROUND_SPEED;

                        // Vérifier si on a atteint la fin du défilement
                        if (backgroundY >= maxDownMovement) {
                            // Arrêter le mouvement à la position finale exacte
                            backgroundY = maxDownMovement;
                            backgroundImage.setTranslationY(backgroundY);
                            isBackgroundMoving = false;

                            // Optionnel : vous pouvez ajouter une action quand le background s'arrête
                            // Par exemple, déclencher un événement spécial ou passer au niveau suivant
                            onBackgroundMovementFinished();
                            return;
                        }

                        // Appliquer le mouvement
                        backgroundImage.setTranslationY(backgroundY);

                        // Continuer l'animation (16ms ≈ 60 FPS)
                        backgroundHandler.postDelayed(this, 30);
                    }
                }
            };
            backgroundHandler.post(backgroundRunnable);
        }
    }

    // Méthode appelée quand le background a fini de se déplacer
    private void onBackgroundMovementFinished() {
        // Vous pouvez ajouter ici ce que vous voulez faire quand le background s'arrête
        // Par exemple :
        // - Augmenter la difficulté du jeu
        // - Déclencher un boss
        // - Passer au niveau suivant
        // - Afficher un message

        System.out.println("Le background a atteint la fin !");

        // Exemple : vous pourriez vouloir accélérer l'apparition des ennemis
        // ou modifier d'autres paramètres du jeu
    }

    // Ajoutez aussi cette méthode pour réinitialiser le background si nécessaire
    private void resetBackgroundPosition() {
        backgroundY = 0f;
        backgroundImage.setTranslationY(backgroundY);
        isBackgroundMoving = false;
    }

    private void stopBackgroundMovement() {
        isBackgroundMoving = false;
        if (backgroundHandler != null && backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
    }

    private void pauseBackgroundMovement() {
        isBackgroundMoving = false;
        if (backgroundHandler != null && backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
    }

    private void resumeBackgroundMovement() {
        if (!isBackgroundMoving && gameStarted) {
            startBackgroundMovement();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // === ACTIVATION DU CAPTEUR ET REPRISE DE L'ARRIÈRE-PLAN ===
        if (accelerometer != null && gameStarted) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        resumeBackgroundMovement();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // === DÉSACTIVATION DU CAPTEUR ET ANIMATIONS ===
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        if (characterMovementHelper != null) {
            characterMovementHelper.cancelAnimations();
        }

        if (jumpHandler != null && repetitiveJumpRunnable != null) {
            jumpHandler.removeCallbacks(repetitiveJumpRunnable);
        }

        // === PAUSE DE L'ARRIÈRE-PLAN ===
        pauseBackgroundMovement();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // === ARRÊT COMPLET DE L'ARRIÈRE-PLAN ===
        stopBackgroundMovement();
    }

    // === GESTION DU CAPTEUR POUR LE MOUVEMENT ===
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (characterMovementHelper != null && gameStarted) {
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            characterMovementHelper.handleSensorEvent(event, rotation);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // === RESTE DU CODE INCHANGÉ ===
    private void spawnEnemies() {
        for (int i = 0; i < ENEMY_COUNT; i++) {
            handler.postDelayed(this::createEnemy, i * SPAWN_DELAY);
        }
    }

    private void createEnemy() {
        ImageView enemy = new ImageView(this);
        enemy.setImageResource(R.drawable.evil);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ENEMY_SIZE, ENEMY_SIZE);

        int maxWidth = gameLayout.getWidth() - ENEMY_SIZE;
        int randomX = random.nextInt(Math.max(maxWidth, 1));
        enemy.setX(randomX);
        enemy.setY(0);
        enemy.setLayoutParams(params);

        gameLayout.addView(enemy);

        animateEnemy(enemy);
    }

    private void animateEnemy(ImageView enemy) {
        int screenHeight = gameLayout.getHeight();
        enemy.animate()
                .translationY(screenHeight)
                .setDuration(4000 + random.nextInt(2000))
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> {
                    gameLayout.removeView(enemy);
                    createEnemy();
                })
                .start();

        checkCollisionRepeatedly(enemy);
    }

    private void checkCollisionRepeatedly(ImageView enemy) {
        Runnable check = new Runnable() {
            @Override
            public void run() {
                if (enemy.getParent() == null) return;

                if (checkCollision(player, enemy)) {
                    Intent intent = new Intent(Level4Activity.this, GameOverActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    handler.postDelayed(this, 100);
                }
            }
        };
        handler.post(check);
    }

    private boolean checkCollision(View v1, View v2) {
        Rect r1 = new Rect();
        Rect r2 = new Rect();
        v1.getHitRect(r1);
        v2.getHitRect(r2);
        return Rect.intersects(r1, r2);
    }
}