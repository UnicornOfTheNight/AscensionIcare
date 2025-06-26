package com.example.superjump;

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
import android.util.Log;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.animation.Animator;
import androidx.core.animation.AnimatorListenerAdapter;

import java.util.ArrayList;
import java.util.List;

public class Level2Activity extends AppCompatActivity implements SensorEventListener {
    private ImageView character;
    private ImageView startPlatform;
    private PlatformCreationHelper platformCreator;
    private List<ImageView> platforms;

    // Variables pour la physique
    private float velocityY = 0;
    private final float GRAVITY = 4f;
    private final float TERMINAL_VELOCITY = 300f;
    private boolean isOnGround = false;
    private boolean isJumping = false;

    // Variables pour la position
    private float characterX, characterY;
    private boolean firstJump = true;

    // Handler pour la boucle de jeu
    private Handler gameHandler = new Handler();
    private Runnable gameRunnable;

    // Dimensions de l'écran
    private int screenWidth, screenHeight;

    // Variables pour le gyroscope/accéléromètre
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float currentTilt = 0f;
    private final float TILT_SENSITIVITY = 0.01f; // Sensibilité du mouvement
    private final float MOVEMENT_SPEED = 1000f; // Vitesse de déplacement

    // Variables pour les sauts automatiques
    private boolean autoJumpEnabled = true;
    private final float JUMP_FORCE = -55f;
    private long lastJumpTime = 0;
    private final long JUMP_COOLDOWN = 100; // Cooldown en millisecondes entre sauts

    // NOUVEAU : Variable pour tracker la dernière plateforme sur laquelle le joueur a atterri
    private ImageView lastLandedPlatform = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_level2);

        initializeGame();
        initializeSensors();
        startGameLoop();
    }
    private void initializeGame() {
        // Récupérer les dimensions de l'écran
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        // Initialiser le personnage et la plateforme de départ
        character = findViewById(R.id.imageView_perso);
        startPlatform = findViewById(R.id.imageView_plateforme);

        // Attendre que les vues soient mesurées avant de positionner le personnage
        character.post(() -> {
            // Positionner le personnage sur la plateforme de départ
            characterX = startPlatform.getX() + (startPlatform.getWidth() - character.getWidth()) / 2;
            characterY = startPlatform.getY() - character.getHeight();

            character.setX(characterX);
            character.setY(characterY);

            isOnGround = true; // Le personnage commence sur la plateforme
            lastLandedPlatform = startPlatform; // NOUVEAU : Définir la plateforme de départ comme première plateforme

            Log.d("Game", "Position initiale - Character: (" + characterX + ", " + characterY +
                    "), Platform: (" + startPlatform.getX() + ", " + startPlatform.getY() + ")");
        });

        // Initialiser les plateformes après avoir positionné le personnage
        platformCreator = new PlatformCreationHelper(Level2Activity.this, findViewById(R.id.main), character, startPlatform);
        platforms = platformCreator.creerPlateformes(true);
    }

    private void initializeSensors() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Log.w("Game", "Accéléromètre non disponible");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            // Récupérer l'inclinaison sur l'axe X (gauche/droite)
            currentTilt = event.values[0];

            // Appliquer un seuil pour éviter les mouvements involontaires
            if (Math.abs(currentTilt) < 1.0f) {
                currentTilt = 0;
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Pas nécessaire pour ce cas d'usage
    }

    private void startGameLoop() {
        gameRunnable = new Runnable() {
            @Override
            public void run() {
                updateCharacter();
                gameHandler.postDelayed(this, 16); // ~60 FPS
            }
        };
        gameHandler.post(gameRunnable);
    }

    private void updateCharacter() {
        // Mouvement horizontal basé sur l'inclinaison
        updateHorizontalMovement();

        // Appliquer la gravité si le personnage n'est pas au sol
        if (!isOnGround) {
            velocityY += GRAVITY;
//            if (velocityY > TERMINAL_VELOCITY) {
//                velocityY = TERMINAL_VELOCITY;
//            }
        }

        // Mettre à jour la position Y
        characterY += velocityY;

        // Vérifier les collisions avec les plateformes
        checkPlatformCollisions();

        // Gestion des sauts automatiques
        handleAutoJump();

        // Vérifier si le personnage tombe dans le vide
        checkFallIntoVoid();

        // Appliquer la nouvelle position
        if(!firstJump){
            character.setX(characterX);
            character.setY(characterY);

            if(character.getY() >= screenHeight - character.getHeight() - 50){
                Intent homeIntent = new Intent(Level2Activity.this, Level4Activity.class);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                homeIntent.putExtra("goToTab", 0); // si tu veux ouvrir un onglet particulier
                startActivity(homeIntent);
                finish();
            }
        }
    }

    private void updateHorizontalMovement() {
        // Calculer le mouvement basé sur l'inclinaison
        float movement = -currentTilt * TILT_SENSITIVITY * MOVEMENT_SPEED;
        characterX += movement;

        // Vérifier les limites de l'écran
        if (characterX < 0) {
            characterX = 0;
        } else if (characterX + character.getWidth() > screenWidth) {
            characterX = screenWidth - character.getWidth();
        }
    }

    private void handleAutoJump() {
        if (!autoJumpEnabled) return;

        long currentTime = System.currentTimeMillis();

        // Si le personnage vient d'atterrir et que le cooldown est passé
        if (isOnGround && !isJumping && (currentTime - lastJumpTime) > JUMP_COOLDOWN) {
            performJump();
            lastJumpTime = currentTime;
        }
    }

    private void performJump() {
        velocityY = JUMP_FORCE;
        isOnGround = false;
        isJumping = true;
    }

    private void checkPlatformCollisions() {
        isOnGround = false;

        for (ImageView platform : platforms) {
            // NOUVEAU : Vérifier si la plateforme existe encore (n'a pas disparu)
            if (platform.getParent() == null) {
                continue; // Ignorer les plateformes qui ont disparu
            }

            if (isCollidingWithPlatform(platform)) {
                // Si le personnage tombe et touche le dessus de la plateforme
                if (velocityY > 0 &&
                        characterY + character.getHeight() >= platform.getY() &&
                        characterY + character.getHeight() <= platform.getY() + platform.getHeight()) {

                    // Placer le personnage sur la plateforme
                    characterY = platform.getY() - character.getHeight();
                    velocityY = 0;
                    isOnGround = true;
                    isJumping = false;
                    firstJump = false;

                    // NOUVEAU : Vérifier si c'est une nouvelle plateforme
                    if (lastLandedPlatform != platform) {
                        lastLandedPlatform = platform;
                        // Notifier le PlatformCreationHelper que le joueur a atterri sur cette plateforme
                        platformCreator.onPlayerLandedOnPlatform(platform);
                        Log.d("Game", "Joueur atterri sur une nouvelle plateforme - Timer de disparition démarré");
                    }
                    break;
                }
            }
        }
    }

    private boolean isCollidingWithPlatform(ImageView platform) {
        // Récupérer les dimensions et positions
        float charLeft = characterX;
        float charRight = characterX + character.getWidth();
        float charTop = characterY;
        float charBottom = characterY + character.getHeight();

        float platLeft = platform.getX();
        float platRight = platform.getX() + platform.getWidth();
        float platTop = platform.getY();
        float platBottom = platform.getY() + platform.getHeight();

        // Vérifier la collision (intersection des rectangles)
        return charLeft < platRight &&
                charRight > platLeft &&
                charTop < platBottom &&
                charBottom > platTop;
    }

    private void checkFallIntoVoid() {
        // Si le personnage tombe en dessous de l'écran
        if (characterY > screenHeight + 100) {
            handleFallIntoVoid();
        }
    }

    private void handleFallIntoVoid() {
        Log.d("Game", "Le personnage est tombé dans le vide !");
        respawnCharacter();
    }

    private void respawnCharacter() {
        // Remettre le personnage sur la plateforme de départ
        characterX = startPlatform.getX() + (startPlatform.getWidth() - character.getWidth()) / 2;
        characterY = startPlatform.getY() - character.getHeight();
        velocityY = 0;
        isOnGround = true; // Il respawn sur la plateforme
        isJumping = false;
        lastJumpTime = 0; // Reset du timer de saut
        lastLandedPlatform = startPlatform; // NOUVEAU : Reset de la dernière plateforme

        // Appliquer immédiatement la position
        character.setX(characterX);
        character.setY(characterY);

        //Toast.makeText(this, "Respawn !", Toast.LENGTH_SHORT).show();
    }

    // Méthodes pour ajuster les paramètres en temps réel
    public void setTiltSensitivity(float sensitivity) {
        // Vous pouvez appeler cette méthode pour ajuster la sensibilité
        // TILT_SENSITIVITY = sensitivity; (rendre la variable non-final)
    }

    public void setAutoJumpEnabled(boolean enabled) {
        autoJumpEnabled = enabled;
    }

    public void setJumpCooldown(long cooldown) {
        // JUMP_COOLDOWN = cooldown; (rendre la variable non-final)
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
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Arrêter la boucle de jeu
        if (gameHandler != null && gameRunnable != null) {
            gameHandler.removeCallbacks(gameRunnable);
        }
        // Désinscrire les sensors
        sensorManager.unregisterListener(this);

        // NOUVEAU : Nettoyer les handlers du PlatformCreationHelper
        if (platformCreator != null) {
            platformCreator.cleanup();
        }
    }

    // Getters utiles
    public boolean isAutoJumpEnabled() { return autoJumpEnabled; }
    public float getCurrentTilt() { return currentTilt; }
    public boolean isCharacterOnGround() { return isOnGround; }
}