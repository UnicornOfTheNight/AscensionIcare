package com.example.superjump;

import android.content.Context;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.ArrayList;
import java.util.List;

public class Level1Activity extends AppCompatActivity implements SensorEventListener {
    /// screen elements
    private ConstraintLayout gameAreaLayout;
    private ImageView characterImageView;

    /// sensor elements to move character
    private SensorManager sensorManager;
    private Sensor accelerometer;

    /// classes helpers
    private PlatformCreationHelper platformCreator;
    private CharacterMovementHelper characterMovementHelper;
    private final Handler jumpHandler = new Handler();
    private Runnable repetitiveJumpRunnable;


    private List<ImageView> activePlatforms = new ArrayList<>();
    private float previousCharacterBottomY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level1);

        characterImageView = findViewById(R.id.imageView_perso);
        gameAreaLayout = findViewById(R.id.main);
        previousCharacterBottomY = characterImageView.getY();

        gameAreaLayout.post(() -> {
            characterMovementHelper = new CharacterMovementHelper(characterImageView, gameAreaLayout);
            characterMovementHelper.updateGroundY();

            // Initialize and use PlatformCreationHelper
            platformCreator = new PlatformCreationHelper(Level1Activity.this, gameAreaLayout, characterImageView, findViewById(R.id.imageView_plateforme));

            activePlatforms = platformCreator.creerPlateformes();

            // repeat jump
            repetitiveJumpRunnable = new Runnable() {
                @Override
                public void run() {
                    if (characterMovementHelper != null && !characterMovementHelper.getIsJumping()) {
                        characterMovementHelper.performJump();
                    }
                    checkCollisions();
                    jumpHandler.postDelayed(this, 0);
                }
            };
            jumpHandler.post(repetitiveJumpRunnable);
        });

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }
    boolean beginning = true;
    private void checkCollisions() {
        if (characterImageView == null || activePlatforms == null || activePlatforms.isEmpty() || characterMovementHelper == null) {
            return;
        }

        Rect characterRect = new Rect(
                (int) characterImageView.getX(),
                (int) characterImageView.getY(),
                (int) (characterImageView.getX() + characterImageView.getWidth()),
                (int) (characterImageView.getY() + characterImageView.getHeight())
        );

        float currentCharacterBottomY = characterRect.bottom;

        for (ImageView platform : activePlatforms) {
            if (platform == null || platform.getVisibility() != ImageView.VISIBLE) continue;

            Rect platformRect = new Rect(
                    (int) platform.getX(),
                    (int) platform.getY(),
                    (int) (platform.getX() + platform.getWidth()),
                    (int) (platform.getY() + platform.getHeight())
            );

            boolean horizontalOverlap = characterRect.left < platformRect.right &&
                    characterRect.right > platformRect.left;

            if (horizontalOverlap) {

                boolean isFallingOnPlatform =
                                        beginning ||
                                                (previousCharacterBottomY <= platformRect.top &&
                                currentCharacterBottomY >= platformRect.top &&
                                currentCharacterBottomY <= platformRect.top + (platform.getHeight() / 2.0f));

                if (isFallingOnPlatform) {
                    beginning =false;
                    characterMovementHelper.landOnPlatform(platformRect.top);
                    break;
                }
            }
        }
        previousCharacterBottomY = currentCharacterBottomY;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }

        // Reprendre la boucle de jeu si elle avait été arrêtée
        if (repetitiveJumpRunnable != null && gameAreaLayout.getWidth() > 0) { // S'assurer que le layout est prêt
            // Vider les anciens messages au cas où onPause puis onResume rapidement
            jumpHandler.removeCallbacks(repetitiveJumpRunnable);
            jumpHandler.post(repetitiveJumpRunnable);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // stop listening to sensor when activity is paused
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }

        // cancel animation when activity is paused
        if (characterMovementHelper != null) {
            characterMovementHelper.cancelAnimations();
        }
        // stop jumps when activity is paused
        if (jumpHandler != null && repetitiveJumpRunnable != null) {
            jumpHandler.removeCallbacks(repetitiveJumpRunnable);
        }

        previousCharacterBottomY = -1;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (characterMovementHelper != null) {
            int rotation = getWindowManager().getDefaultDisplay().getRotation();
            characterMovementHelper.handleSensorEvent(event, rotation); // call character class method
        }
    }

    /// @summary method not used in this version, present for the SensorEventListener interface
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }
}