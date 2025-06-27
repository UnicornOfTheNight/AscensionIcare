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
import android.view.animation.AlphaAnimation;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.List;
import java.util.Random;

public class Level1Activity extends AppCompatActivity implements SensorEventListener {

    private ConstraintLayout gameLayout;
    private ImageView player;
    private ImageView backgroundImage;
    private FrameLayout backgroundFrame;
    private TextView introTextView;
    private Handler handler = new Handler();
    private Random random = new Random();



    // === VARIABLES POUR L'ARRIÈRE-PLAN MOBILE ===
    private Handler backgroundHandler = new Handler();
    private Runnable backgroundRunnable;
    private boolean isBackgroundMoving = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level1);

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

            Log.d("Level1", "Position initiale - Character: (" + characterX + ", " + characterY +
                    "), Platform: (" + startPlatform.getX() + ", " + startPlatform.getY() + ")");
        });

        // Initialiser les plateformes après avoir positionné le personnage
        platformCreator = new PlatformCreationHelper(Level1Activity.this, gameLayout, player, startPlatform);
        activePlatforms = platformCreator.creerPlateformes(false);

        // Démarrer la boucle de jeu
        startGameLoop();
    }

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
                if (gameStarted) {
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
            player.setX(characterX);
            player.setY(characterY);
        }

        if(characterY <= player.getHeight()+100) {
            Intent homeIntent = new Intent(Level1Activity.this, Level2Activity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            finish();
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
        Log.d("Level1", "Le personnage est tombé dans le vide !");
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
                    if (isBackgroundMoving) {
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
                    if (isPlatMoving) {
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
        if (!isBackgroundMoving && gameStarted) {
            startBackgroundMovement();
        }
    }

    private void resumePlatMovement() {
        if (!isPlatMoving && gameStarted) {
            startPlatMovement();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // === ACTIVATION DU CAPTEUR ET REPRISE DE L'ARRIÈRE-PLAN ===
        if (accelerometer != null && gameStarted && sensorManager != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
        resumeBackgroundMovement();
        resumePlatMovement();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // === DÉSACTIVATION DU CAPTEUR ET ANIMATIONS ===
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // === PAUSE DE L'ARRIÈRE-PLAN ===
        pauseBackgroundMovement();
        pausePlatMovement();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // === ARRÊT COMPLET DE L'ARRIÈRE-PLAN ===
        stopBackgroundMovement();
        stopPlatMovement();

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
        if (gameHandler != null && gameRunnable != null) {
            gameHandler.removeCallbacks(gameRunnable);
        }
    }

    // === GESTION DU CAPTEUR POUR LE MOUVEMENT ===
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
        // Pas d'implémentation nécessaire
    }
    private boolean checkCollision(View v1, View v2) {
        if (v1 == null || v2 == null) return false;

        Rect r1 = new Rect();
        Rect r2 = new Rect();
        v1.getHitRect(r1);
        v2.getHitRect(r2);
        return Rect.intersects(r1, r2);
    }
}