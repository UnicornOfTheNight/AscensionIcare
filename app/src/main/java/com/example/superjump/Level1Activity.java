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
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.animation.Animator;
import androidx.core.animation.AnimatorListenerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Level1Activity extends AppCompatActivity implements SensorEventListener {
    private ImageView character;
    private ImageView startPlatform;
    private ImageView backgroundImage;
    private PlatformCreationHelper platformCreator;
    private List<ImageView> platforms;

    // Éléments de pause
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

    // Variables pour la physique
    private float velocityY = 0;
    private final float GRAVITY = 3f;
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
    private final float JUMP_FORCE = -65f;
    private long lastJumpTime = 0;
    private final long JUMP_COOLDOWN = 100; // Cooldown en millisecondes entre sauts

    // === NOUVELLES VARIABLES POUR LES ENNEMIS ===
    private Handler enemyHandler = new Handler();
    private Random random = new Random();
    private final int ENEMY_SIZE = 200;
    private final int ENEMY_COUNT = 3; // Moins d'ennemis pour Level1
    private final int SPAWN_DELAY = 2000; // Plus lent que Level4
    private boolean gameStarted = false;

    // === VARIABLES POUR L'ARRIÈRE-PLAN MOBILE ===
    private Handler backgroundHandler = new Handler();
    private Runnable backgroundRunnable;
    private boolean isBackgroundMoving = false;
    private float backgroundY = 0f;
    private final float BACKGROUND_SPEED = 10f; // Plus lent que Level4

    boolean isPaused = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level1);

        // Initialiser les éléments de pause
        initializePauseElements();

        // Initialiser et démarrer le chronomètre
        initTimer();
        startTimer();

        initializeGame();
        initializeSensors();
        startGameLoop();

        // === DÉMARRER LE JEU APRÈS UN COURT DÉLAI ===
        gameHandler.postDelayed(() -> {
            gameStarted = true;
            startBackgroundMovement();
            // Commencer les ennemis après 3 secondes
            gameHandler.postDelayed(this::startEnemySpawning, 3000);
        }, 1000);
    }

    private void initializePauseElements() {
        pauseButton = findViewById(R.id.pauseButton);
        resumeButton = findViewById(R.id.resumeButton);
        quitButton = findViewById(R.id.quitButton);
        pauseMenu = findViewById(R.id.pauseMenu);
        timerText = findViewById(R.id.timerText);

        pauseButton.setOnClickListener(v -> {
            isManuallyPaused = true;
            pauseTimer();
            onPause(); // Appelle pause manuelle
        });

        resumeButton.setOnClickListener(v -> {
            isPaused = false;
            resumeTimer();
            resumeGame();
        });

        quitButton.setOnClickListener(v -> {
            isManuallyPaused = false;
            stopTimer();
            Intent intent = new Intent(Level1Activity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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

    private void resumeGame() {
        isManuallyPaused = false;
        pauseMenu.setVisibility(View.GONE);
        pauseButton.setVisibility(View.VISIBLE);
        resumeBackgroundMovement();
    }

    private void initializeGame() {
        // Récupérer les dimensions de l'écran
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        // Initialiser le personnage, la plateforme de départ et le background
        character = findViewById(R.id.imageView_perso);
        startPlatform = findViewById(R.id.imageView_plateforme);
        backgroundImage = findViewById(R.id.imageView_background); // Assurez-vous que cet ID existe dans votre layout

        // Attendre que les vues soient mesurées avant de positionner le personnage
        character.post(() -> {
            // Positionner le personnage sur la plateforme de départ
            characterX = startPlatform.getX() + (startPlatform.getWidth() - character.getWidth()) / 2;
            characterY = startPlatform.getY() - character.getHeight();

            character.setX(characterX);
            character.setY(characterY);

            isOnGround = true; // Le personnage commence sur la plateforme

            Log.d("Game", "Position initiale - Character: (" + characterX + ", " + characterY +
                    "), Platform: (" + startPlatform.getX() + ", " + startPlatform.getY() + ")");
        });

        // Initialiser les plateformes après avoir positionné le personnage
        platformCreator = new PlatformCreationHelper(Level1Activity.this, findViewById(R.id.main), character, startPlatform);
        platforms = platformCreator.creerPlateformes();
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

    // === MÉTHODES POUR L'ARRIÈRE-PLAN MOBILE ===
    private void startBackgroundMovement() {
        if (!isBackgroundMoving && backgroundImage != null) {
            isBackgroundMoving = true;
            backgroundRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isBackgroundMoving && !isPaused) {
                        // Hauteur de l'écran et de l'image de fond
                        int screenHeight = findViewById(R.id.main).getHeight();
                        int backgroundImageHeight = backgroundImage.getHeight();

                        // Calcul du déplacement maximum possible vers le bas
                        float maxDownMovement = backgroundImageHeight - screenHeight;

                        // Déplacer l'arrière-plan vers le BAS
                        backgroundY += BACKGROUND_SPEED;

                        // Vérifier si on a atteint la fin du défilement
                        if (backgroundY >= maxDownMovement) {
                            backgroundY = maxDownMovement;
                            backgroundImage.setTranslationY(backgroundY);
                            isBackgroundMoving = false;
                            onBackgroundMovementFinished();
                            return;
                        }

                        // Appliquer le mouvement
                        backgroundImage.setTranslationY(backgroundY);

                        // Continuer l'animation
                        backgroundHandler.postDelayed(this, 50);
                    }
                }
            };
            backgroundHandler.post(backgroundRunnable);
        }
    }

    private void onBackgroundMovementFinished() {
        Log.d("Game", "Le background a atteint la fin !");
        // Ici vous pouvez ajouter des actions spéciales quand le background s'arrête
    }

    private void pauseBackgroundMovement() {
        isBackgroundMoving = false;
        if (backgroundHandler != null && backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
    }

    private void resumeBackgroundMovement() {
        if (!isBackgroundMoving && gameStarted && !isPaused) {
            startBackgroundMovement();
        }
    }

    private void stopBackgroundMovement() {
        isBackgroundMoving = false;
        if (backgroundHandler != null && backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
    }

    // === MÉTHODES POUR LES ENNEMIS ===
    private void startEnemySpawning() {
        if (gameStarted) {
            spawnEnemies();
        }
    }

    private void spawnEnemies() {
        for (int i = 0; i < ENEMY_COUNT; i++) {
            enemyHandler.postDelayed(this::createEnemy, i * SPAWN_DELAY);
        }
    }

    private void createEnemy() {
        if (!gameStarted || isPaused) return;

        ImageView enemy = new ImageView(this);
        enemy.setImageResource(R.drawable.evil); // Assurez-vous que cette ressource existe
        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(ENEMY_SIZE, ENEMY_SIZE);

        ConstraintLayout gameLayout = findViewById(R.id.main);
        int maxWidth = gameLayout.getWidth() - ENEMY_SIZE;
        if (maxWidth > 0) {
            int randomX = random.nextInt(maxWidth);
            enemy.setX(randomX);
            enemy.setY(0);
            enemy.setLayoutParams(params);

            gameLayout.addView(enemy);
            animateEnemy(enemy);
        }
    }

    private void animateEnemy(ImageView enemy) {
        int screenHeight = findViewById(R.id.main).getHeight();
        enemy.animate()
                .translationY(screenHeight)
                .setDuration(5000 + random.nextInt(2000)) // Plus lent que Level4
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> {
                    ConstraintLayout gameLayout = findViewById(R.id.main);
                    gameLayout.removeView(enemy);
                    // Recréer un ennemi après un délai
                    enemyHandler.postDelayed(this::createEnemy, SPAWN_DELAY);
                })
                .start();

        checkCollisionRepeatedly(enemy);
    }

    private void checkCollisionRepeatedly(ImageView enemy) {
        Runnable check = new Runnable() {
            @Override
            public void run() {
                if (enemy.getParent() == null || isPaused) return;

                if (checkCollision(character, enemy)) {
                    // Game Over - rediriger vers GameOverActivity ou respawn
                    handleEnemyCollision();
                } else {
                    enemyHandler.postDelayed(this, 100);
                }
            }
        };
        enemyHandler.post(check);
    }

    private void handleEnemyCollision() {
        Log.d("Game", "Collision avec un ennemi !");
        // Option 1: Game Over
        // Intent intent = new Intent(Level1Activity.this, GameOverActivity.class);
        // startActivity(intent);
        // finish();

        // Option 2: Respawn (plus adapté pour Level1)
        respawnCharacter();
    }

    private boolean checkCollision(View v1, View v2) {
        Rect r1 = new Rect();
        Rect r2 = new Rect();
        v1.getHitRect(r1);
        v2.getHitRect(r2);
        return Rect.intersects(r1, r2);
    }

    private void startGameLoop() {
        gameRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isPaused) {
                    updateCharacter();
                }
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
        if (isOnGround && !isJumping && (currentTime - lastJumpTime) > JUMP_COOLDOWN && !isPaused) {
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

        // Appliquer immédiatement la position
        character.setX(characterX);
        character.setY(characterY);
    }

    // Méthodes pour ajuster les paramètres en temps réel
    public void setTiltSensitivity(float sensitivity) {
        // Vous pouvez appeler cette méthode pour ajuster la sensibilité
    }

    public void setAutoJumpEnabled(boolean enabled) {
        autoJumpEnabled = enabled;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isManuallyPaused) {
            pauseMenu.setVisibility(View.VISIBLE);
            pauseMenu.setElevation(6);
            pauseButton.setVisibility(View.GONE);
            isPaused = true;
        } else {
            // Pause automatique (changement d'activité)
            pauseTimer();
            isPaused = true;
        }

        // === PAUSE DES NOUVELLES FONCTIONNALITÉS ===
        pauseBackgroundMovement();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isManuallyPaused) {
            pauseMenu.setVisibility(View.GONE);
            pauseButton.setVisibility(View.VISIBLE);
            resumeTimer();
            isPaused = false;
            resumeBackgroundMovement();
        }
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();

        // === ARRÊT DES NOUVELLES FONCTIONNALITÉS ===
        stopBackgroundMovement();

        // Arrêter la boucle de jeu
        if (gameHandler != null && gameRunnable != null) {
            gameHandler.removeCallbacks(gameRunnable);
        }

        // Arrêter les ennemis
        if (enemyHandler != null) {
            enemyHandler.removeCallbacksAndMessages(null);
        }

        // Désinscrire les sensors
        sensorManager.unregisterListener(this);
    }

    // Getters utiles
    public boolean isAutoJumpEnabled() { return autoJumpEnabled; }
    public float getCurrentTilt() { return currentTilt; }
    public boolean isCharacterOnGround() { return isOnGround; }
}