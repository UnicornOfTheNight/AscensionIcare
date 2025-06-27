package com.example.superjump;

import static android.view.View.VISIBLE;

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
import android.view.ViewPropertyAnimator;
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Level4Activity extends AppCompatActivity implements SensorEventListener {

    private ConstraintLayout gameLayout;
    private ImageView player;
    private ImageView backgroundImage;
    private FrameLayout backgroundFrame;
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
    private boolean isPaused = false;
    private Handler platHandler = new Handler();
    private Runnable platRunnable;
    private boolean isPlatMoving = false;

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

    // === NOUVELLES VARIABLES POUR LA PHYSIQUE (comme Level1) ===
    private float velocityY = 0;
    private final float GRAVITY = 4f;
    private final float TERMINAL_VELOCITY = 300f;
    private boolean isOnGround = false;
    private boolean isJumping = false;
    private float characterX, characterY;
    private int screenWidth, screenHeight;
    private final float JUMP_FORCE = -55f;
    private long lastJumpTime = 0;
    private final long JUMP_COOLDOWN = 100;
    private ImageView startPlatform;

    // Handler pour la boucle de jeu
    private Handler gameHandler = new Handler();
    private Runnable gameRunnable;

    // Variables pour le mouvement horizontal
    private float currentTilt = 0f;
    private final float TILT_SENSITIVITY = 0.01f;
    private final float MOVEMENT_SPEED = 200f;
    private boolean firstJump = true;

    // === NOUVELLES VARIABLES POUR LA GESTION DES ENNEMIS ===
    private List<EnemyInfo> activeEnemies = new ArrayList<>();

    // Classe pour stocker les informations d'un ennemi
    private static class EnemyInfo {
        ImageView enemyView;
        ViewPropertyAnimator animator;
        float currentY;
        float targetY;
        long remainingDuration;
        long startTime;
        boolean isPaused;
        Runnable collisionChecker;

        EnemyInfo(ImageView view, ViewPropertyAnimator anim, float target, long duration) {
            this.enemyView = view;
            this.animator = anim;
            this.currentY = view.getY();
            this.targetY = target;
            this.remainingDuration = duration;
            this.startTime = System.currentTimeMillis();
            this.isPaused = false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level4);

        // Initialiser les vues d'abord
        initializeViews();
        initializeScreenDimensions();

        // === INITIALISATION DU CAPTEUR ===
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Log.w("Game", "Accéléromètre non disponible");
        }

        // Attendre que le layout soit prêt avant d'afficher le texte
        gameLayout.post(() -> {
            showIntroText();
        });

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
            pauseGame(); // Nouvelle méthode pour pause complète
            pauseButton.setVisibility(VISIBLE);
            resumeButton.setVisibility(VISIBLE);
            Log.d("HHH", "pauseButton.getVisibility(): " + pauseButton.getVisibility());
        });

        resumeButton.setOnClickListener(v -> {
            resumeTimer();
            resumeGame();
        });

        quitButton.setOnClickListener(v -> {
            isManuallyPaused = false;
            stopTimer();
            Intent intent = new Intent(Level4Activity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
        });
    }

    // === NOUVELLES MÉTHODES POUR LA GESTION DE LA PAUSE ===
    private void pauseGame() {
        isPaused = true;

        // Pause des animations d'ennemis
        pauseAllEnemyAnimations();

        // Afficher le menu de pause
        findViewById(R.id.pauseMenu).setVisibility(VISIBLE);
        findViewById(R.id.resumeButton).setVisibility(VISIBLE);
        findViewById(R.id.quitButton).setVisibility(VISIBLE);

        // Désactiver le capteur
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // Pause de l'arrière-plan
        pauseBackgroundMovement();
        pausePlatMovement();
    }

    private void pauseAllEnemyAnimations() {
        for (EnemyInfo enemyInfo : activeEnemies) {
            if (!enemyInfo.isPaused && enemyInfo.animator != null) {
                // Calculer le temps écoulé
                long elapsedTime = System.currentTimeMillis() - enemyInfo.startTime;
                enemyInfo.remainingDuration = Math.max(0, enemyInfo.remainingDuration - elapsedTime);

                // Sauvegarder la position actuelle
                enemyInfo.currentY = enemyInfo.enemyView.getY();

                // Annuler l'animation
                enemyInfo.animator.cancel();
                enemyInfo.isPaused = true;

                // Arrêter le vérificateur de collision
                if (enemyInfo.collisionChecker != null) {
                    handler.removeCallbacks(enemyInfo.collisionChecker);
                }

                Log.d("EnemyPause", "Ennemi pausé à Y=" + enemyInfo.currentY +
                        ", temps restant=" + enemyInfo.remainingDuration);
            }
        }
    }

    private void resumeAllEnemyAnimations() {
        for (EnemyInfo enemyInfo : activeEnemies) {
            if (enemyInfo.isPaused && enemyInfo.remainingDuration > 0) {
                // Reprendre l'animation depuis la position actuelle
                enemyInfo.animator = enemyInfo.enemyView.animate()
                        .translationY(enemyInfo.targetY)
                        .setDuration(enemyInfo.remainingDuration)
                        .setInterpolator(new LinearInterpolator())
                        .withEndAction(() -> {
                            if (gameLayout != null) {
                                removeEnemyFromList(enemyInfo.enemyView);
                                gameLayout.removeView(enemyInfo.enemyView);
                                // Recréer un ennemi seulement si le jeu continue
                                if (gameStarted && !isPaused) {
                                    handler.postDelayed(this::createEnemy, SPAWN_DELAY);
                                }
                            }
                        });

                enemyInfo.animator.start();
                enemyInfo.isPaused = false;
                enemyInfo.startTime = System.currentTimeMillis();

                // Reprendre la vérification des collisions
                checkCollisionRepeatedly(enemyInfo.enemyView);

                Log.d("EnemyResume", "Ennemi repris depuis Y=" + enemyInfo.currentY +
                        ", temps restant=" + enemyInfo.remainingDuration);
            }
        }
    }

    private void removeEnemyFromList(ImageView enemy) {
        activeEnemies.removeIf(enemyInfo -> enemyInfo.enemyView == enemy);
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

        private void updateTimerDisplay(long elapsedTime) {
            int minutes = (int) (elapsedTime / 60000);
            int seconds = (int) ((elapsedTime % 60000) / 1000);
            int milliseconds = (int) ((elapsedTime % 1000) / 10);

            String timeText = String.format("%02d:%02d:%02d", minutes, seconds, milliseconds);
            timerText.setText(timeText);
        }

        private void startTimer() {
            if (!isTimerRunning) {
                startTime = System.currentTimeMillis();
                isTimerRunning = true;
                timerHandler.post(timerRunnable);
            }
        }

    private void resumeGame() {
        isManuallyPaused = false;
        isPaused = false;

        // Masquer le menu de pause
        pauseMenu.setVisibility(View.INVISIBLE);
        pauseButton.setVisibility(VISIBLE);

        // Réactiver le capteur
        if (accelerometer != null && gameStarted && sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        // Reprendre les animations
        resumeAllEnemyAnimations();
        resumeBackgroundMovement();
        resumePlatMovement();
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
    private void initializeViews() {
        gameLayout = findViewById(R.id.main);
        player = findViewById(R.id.imageView_perso);
        backgroundImage = findViewById(R.id.imageView_background);
        backgroundFrame = findViewById(R.id.backgroundFrameLayout);
        introTextView = findViewById(R.id.introTextView);

        // Vérifier que toutes les vues existent
        if (gameLayout == null) {
            throw new RuntimeException("gameLayout (R.id.main) not found in layout");
        }
        if (player == null) {
            throw new RuntimeException("player (R.id.imageView_perso) not found in layout");
        }
        if (backgroundImage == null) {
            throw new RuntimeException("backgroundImage (R.id.imageView_background) not found in layout");
        }
        if (backgroundFrame == null) {
            throw new RuntimeException("backgroundFrame (R.id.frame_background) not found in layout");
        }
        if (introTextView == null) {
            throw new RuntimeException("introTextView (R.id.introTextView) not found in layout");
        }
        player.setY(player.getY() - 25);
    }

    private void initializeScreenDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
    }

    private void showIntroText() {
        // Rendre le texte visible
        introTextView.setVisibility(VISIBLE);

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
        }, 4000);
    }

    private void startGame() {
        gameStarted = true;

        // === DÉMARRAGE DE L'ARRIÈRE-PLAN MOBILE ===
        startBackgroundMovement();

        startPlatMovement();

        // === INITIALISATION DES PLATEFORMES ET PHYSIQUE ===
        initializeGamePhysics();

        // Délai de 2 secondes avant de commencer à faire apparaître les ennemis
        handler.postDelayed(() -> spawnEnemies(), 2000);
    }

    private void initializeGamePhysics() {
        // Créer la plateforme de départ
        startPlatform = createStartPlatform();

        // Attendre que les vues soient mesurées avant de positionner le personnage
        player.post(() -> {
            // Positionner le personnage sur la plateforme de départ
            characterX = startPlatform.getX() + (startPlatform.getWidth() - player.getWidth()) / 2;
            characterY = startPlatform.getY() - player.getHeight();

            player.setX(characterX);
            player.setY(characterY);

            isOnGround = true; // Le personnage commence sur la plateforme

            Log.d("Level4", "Position initiale - Character: (" + characterX + ", " + characterY +
                    "), Platform: (" + startPlatform.getX() + ", " + startPlatform.getY() + ")");
        });

        // Initialiser les plateformes après avoir positionné le personnage
        platformCreator = new PlatformCreationHelper(Level4Activity.this, gameLayout, player, startPlatform);
        activePlatforms = platformCreator.creerPlateformes(false);

        // Démarrer la boucle de jeu
        startGameLoop();
    }
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

    private ImageView createStartPlatform() {
        // Créer une plateforme de départ
        ImageView startPlatform = new ImageView(this);
        startPlatform.setImageResource(R.drawable.plateforme_v1); // Assurez-vous que cette ressource existe

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                400, // largeur
                100  // hauteur
        );

        // Positionner la plateforme au bas de l'écran, centrée
        startPlatform.setLayoutParams(params);
        startPlatform.setX((screenWidth - 400) / 2f);
        startPlatform.setY(screenHeight - 200);

        gameLayout.addView(startPlatform);
        return startPlatform;
    }

    private void startGameLoop() {
        gameRunnable = new Runnable() {
            @Override
            public void run() {
                if (gameStarted && !isPaused) {
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
            if(!isPaused){
                player.setX(characterX);
                player.setY(characterY);
            }

            if(characterY <= player.getHeight()+100) {
                Intent homeIntent = new Intent(Level4Activity.this, GameEndActivity.class);
                homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
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
        } else if (characterX + player.getWidth() > screenWidth) {
            characterX = screenWidth - player.getWidth();
        }
    }

    private void handleAutoJump() {
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

        // Vérifier collision avec la plateforme de départ
        if (isCollidingWithPlatform(startPlatform)) {
            if (velocityY > 0 &&
                    characterY + player.getHeight() >= startPlatform.getY() &&
                    characterY + player.getHeight() <= startPlatform.getY() + startPlatform.getHeight()) {

                characterY = startPlatform.getY() - player.getHeight();
                velocityY = 0;
                isOnGround = true;
                isJumping = false;
                firstJump = false;
                return;
            }
        }

        // Vérifier collisions avec les autres plateformes
        if (activePlatforms != null) {
            for (ImageView platform : activePlatforms) {
                if (isCollidingWithPlatform(platform)) {
                    if (velocityY > 0 &&
                            characterY + player.getHeight() >= platform.getY() &&
                            characterY + player.getHeight() <= platform.getY() + platform.getHeight()) {

                        characterY = platform.getY() - player.getHeight();
                        velocityY = 0;
                        isOnGround = true;
                        isJumping = false;
                        firstJump = false;
                        break;
                    }
                }
            }
        }
    }

    private boolean isCollidingWithPlatform(ImageView platform) {
        // Récupérer les dimensions et positions
        float charLeft = characterX;
        float charRight = characterX + player.getWidth();
        float charTop = characterY;
        float charBottom = characterY + player.getHeight();

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
        Log.d("Level4", "Le personnage est tombé dans le vide !");
        respawnCharacter();
    }

    private void respawnCharacter() {
        // Remettre le personnage sur la plateforme de départ
        characterX = startPlatform.getX() + (startPlatform.getWidth() - player.getWidth()) / 2;
        characterY = startPlatform.getY() - player.getHeight();
        velocityY = 0;
        isOnGround = true;
        isJumping = false;
        lastJumpTime = 0;

        // Appliquer immédiatement la position
        player.setX(characterX);
        player.setY(characterY);
    }

    private void startBackgroundMovement() {
        if (!isBackgroundMoving && backgroundImage != null) {
            isBackgroundMoving = true;
            backgroundRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isBackgroundMoving && !isPaused) {
                        // Hauteur de l'écran et de l'image de fond
                        int screenHeight = gameLayout.getHeight();
                        int backgroundImageHeight = backgroundImage.getHeight();

                        // Si la hauteur n'est pas encore disponible, utiliser la hauteur de l'écran
                        if (backgroundImageHeight == 0) {
                            DisplayMetrics displayMetrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                            backgroundImageHeight = displayMetrics.heightPixels * 2; // Estimation
                        }

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
                        backgroundHandler.postDelayed(this, 30);
                    }
                }
            };
            backgroundHandler.post(backgroundRunnable);
        }
    }

    private void startPlatMovement() {
        if (!isPlatMoving && backgroundFrame != null) {
            isPlatMoving = true;
            platRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isPlatMoving && !isPaused) {
                        // Hauteur de l'écran et de l'image de fond
                        int screenHeight = gameLayout.getHeight();
                        int platImageHeight = backgroundFrame.getHeight();

                        // Si la hauteur n'est pas encore disponible, utiliser la hauteur de l'écran
                        if (platImageHeight == 0) {
                            DisplayMetrics displayMetrics = new DisplayMetrics();
                            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                            platImageHeight = displayMetrics.heightPixels * 2; // Estimation
                        }

                        // Calcul du déplacement maximum possible vers le bas
                        float maxDownMovement = platImageHeight - screenHeight;

                        // Déplacer l'arrière-plan vers le BAS
                        backgroundY += BACKGROUND_SPEED;

                        // Vérifier si on a atteint la fin du défilement
                        if (backgroundY >= maxDownMovement) {
                            backgroundY = maxDownMovement;
                            backgroundFrame.setTranslationY(backgroundY);
                            isPlatMoving = false;
                            onPlatMovementFinished();
                            return;
                        }

                        // Appliquer le mouvement
                        backgroundFrame.setTranslationY(backgroundY);

                        // Continuer l'animation
                        platHandler.postDelayed(this, 30);
                    }
                }
            };
            platHandler.post(platRunnable);
        }
    }

    private void onBackgroundMovementFinished() {
        System.out.println("Le background a atteint la fin !");
    }

    private void onPlatMovementFinished() {
        System.out.println("La plateforme a atteint la fin !");
    }

    private void resetBackgroundPosition() {
        backgroundY = 0f;
        if (backgroundImage != null) {
            backgroundImage.setTranslationY(backgroundY);
        }
        isBackgroundMoving = false;
    }
    private void resetPlatPosition() {
        backgroundY = 0f;
        if (backgroundFrame != null) {
            backgroundFrame.setTranslationY(backgroundY);
        }
        isPlatMoving = false;
    }

    private void stopBackgroundMovement() {
        isBackgroundMoving = false;
        if (backgroundHandler != null && backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
    }

    private void stopPlatMovement() {
        isPlatMoving = false;
        if (platHandler != null && platRunnable != null) {
            platHandler.removeCallbacks(platRunnable);
        }
    }

    private void pauseBackgroundMovement() {
        isBackgroundMoving = false;
        if (backgroundHandler != null && backgroundRunnable != null) {
            backgroundHandler.removeCallbacks(backgroundRunnable);
        }
    }

    private void pausePlatMovement() {
        isPlatMoving = false;
        if (platHandler != null && platRunnable != null) {
            platHandler.removeCallbacks(platRunnable);
        }
    }

    private void resumeBackgroundMovement() {
        if (!isBackgroundMoving && gameStarted && !isPaused) {
            startBackgroundMovement();
        }
    }

    private void resumePlatMovement() {
        if (!isPlatMoving && gameStarted && !isPaused) {
            startPlatMovement();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isManuallyPaused) {
            resumeGame();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isManuallyPaused) {
            pauseGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // === ARRÊT COMPLET DE TOUS LES ÉLÉMENTS ===
        stopBackgroundMovement();
        stopPlatMovement();

        // Arrêter toutes les animations d'ennemis
        for (EnemyInfo enemyInfo : activeEnemies) {
            if (enemyInfo.animator != null) {
                enemyInfo.animator.cancel();
            }
            if (enemyInfo.collisionChecker != null) {
                handler.removeCallbacks(enemyInfo.collisionChecker);
            }
        }
        activeEnemies.clear();

        // Nettoyer tous les handlers
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
        if (jumpHandler != null) {
            jumpHandler.removeCallbacksAndMessages(null);
        }
        if (backgroundHandler != null) {
            backgroundHandler.removeCallbacksAndMessages(null);
        }
        if (platHandler != null) {
            platHandler.removeCallbacksAndMessages(null);
        }
        if (gameHandler != null) {
            gameHandler.removeCallbacksAndMessages(null);
        }
        if (timerHandler != null) {
            timerHandler.removeCallbacksAndMessages(null);
        }

        // Désinscrire le capteur
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // Arrêter le chronomètre
        stopTimer();
    }

    private void spawnEnemies() {
        if (!gameStarted || isPaused) return;

        // Créer les ennemis initiaux
        for (int i = 0; i < ENEMY_COUNT; i++) {
            handler.postDelayed(this::createEnemy, i * SPAWN_DELAY);
        }
    }

    private void createEnemy() {
        if (!gameStarted || isPaused) return;

        ImageView enemy = new ImageView(this);
        enemy.setImageResource(R.drawable.evil); // Assurez-vous que cette ressource existe

        ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                ENEMY_SIZE, ENEMY_SIZE
        );
        enemy.setLayoutParams(params);

        // Position aléatoire sur l'axe X
        int randomX = random.nextInt(screenWidth - ENEMY_SIZE);
        enemy.setX(randomX);
        enemy.setY(-ENEMY_SIZE); // Commencer au-dessus de l'écran

        gameLayout.addView(enemy);

        // Calculer la position cible (bas de l'écran)
        float targetY = screenHeight + ENEMY_SIZE;

        // Calculer la durée basée sur la distance
        long duration = (long) ((targetY + ENEMY_SIZE) / BACKGROUND_SPEED * 30);

        // Créer l'animation
        ViewPropertyAnimator animator = enemy.animate()
                .translationY(targetY)
                .setDuration(duration)
                .setInterpolator(new LinearInterpolator())
                .withEndAction(() -> {
                    if (gameLayout != null) {
                        removeEnemyFromList(enemy);
                        gameLayout.removeView(enemy);
                        // Recréer un ennemi après un délai si le jeu continue
                        if (gameStarted && !isPaused) {
                            handler.postDelayed(this::createEnemy, SPAWN_DELAY);
                        }
                    }
                });

        animator.start();

        // Ajouter à la liste des ennemis actifs
        EnemyInfo enemyInfo = new EnemyInfo(enemy, animator, targetY, duration);
        activeEnemies.add(enemyInfo);

        // Commencer la vérification des collisions
        checkCollisionRepeatedly(enemy);
    }

    private void checkCollisionRepeatedly(ImageView enemy) {
        Runnable collisionChecker = new Runnable() {
            @Override
            public void run() {
                if (!isPaused && gameStarted && enemy.getParent() != null) {
                    if (isColliding(player, enemy)) {
                        handleCollision();
                        return;
                    }
                    // Vérifier à nouveau dans 50ms
                    handler.postDelayed(this, 50);
                }
            }
        };

        // Stocker le checker dans l'EnemyInfo correspondant
        for (EnemyInfo enemyInfo : activeEnemies) {
            if (enemyInfo.enemyView == enemy) {
                enemyInfo.collisionChecker = collisionChecker;
                break;
            }
        }

        handler.post(collisionChecker);
    }

    private boolean isColliding(ImageView view1, ImageView view2) {
        Rect rect1 = new Rect();
        Rect rect2 = new Rect();

        view1.getHitRect(rect1);
        view2.getHitRect(rect2);

        return Rect.intersects(rect1, rect2);
    }

    private void handleCollision() {
        Log.d("Level4", "Collision détectée !");

        // Arrêter le jeu et passer à l'écran Game Over
        gameStarted = false;
        isPaused = true;

        Intent gameOverIntent = new Intent(Level4Activity.this, GameOverActivity.class);
        startActivity(gameOverIntent);
        finish();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && gameStarted && !isPaused) {
            // Récupérer l'inclinaison sur l'axe X
            currentTilt = event.values[0];
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Non utilisé dans ce cas
    }
}