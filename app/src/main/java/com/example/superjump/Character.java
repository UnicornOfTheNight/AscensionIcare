package com.example.superjump;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.view.Surface;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import java.util.concurrent.atomic.AtomicReference;


public class Character extends AppCompatActivity implements SensorEventListener {
    private static final float JUMP_HEIGHT = 1000f;
    private static final long JUMP_DURATION = 400;
    private static final float MOVEMENT_SENSITIVITY = 10.0f;
    private static final long HORIZONTAL_ANIMATION_DURATION = 50;
    public ValueAnimator xPositionAnimator;
    public ConstraintLayout gameAreaLayout;
    public ImageView characterImageView;
    //private AppCompatActivity screen;

    private AtomicReference<AppCompatActivity> screen;
    // sensor elements to move character
    public SensorManager sensorManager;
    public Sensor accelerometer;
    public ValueAnimator jumpAnimator;
    public float groundY;

    public boolean isJumping = false;

    public Character(AtomicReference<AppCompatActivity> p_screen){
        screen = p_screen;

        characterImageView = screen.get().findViewById(R.id.imageView_perso);
        gameAreaLayout = screen.get().findViewById(R.id.main);

        sensorManager = (SensorManager) screen.get().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        // wait until layout is drawn to get correct dimensions and initialize character position and groundMore actions
        gameAreaLayout.post(() -> {
            // position character at the wanted position
            groundY = gameAreaLayout.getHeight() - characterImageView.getHeight() - 35f;
            characterImageView.setX(gameAreaLayout.getWidth() / 2f - characterImageView.getWidth() / 2f); // horizontal center
            characterImageView.setY(groundY);

            // repeat jump continuously
            Handler handler = new Handler();
            Runnable repetitiveJump = new Runnable() {
                @Override
                public void run() {
                    if (!isJumping) {
                        performJump();
                    }
                    handler.postDelayed(this, 0);
                }
            };
            handler.post(repetitiveJump);
        });
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float rawSensorValueX;
            float movement = 0;

            int rotation = screen.get().getWindowManager().getDefaultDisplay().getRotation();
            if (rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180) {
                rawSensorValueX = event.values[0];
                movement = -rawSensorValueX * MOVEMENT_SENSITIVITY;
            }
            float targetX = characterImageView.getX() + movement;

            // limit movements to the screen
            float minX = 0;
            float maxX = gameAreaLayout.getWidth() - characterImageView.getWidth();
            targetX = Math.max(minX, Math.min(targetX, maxX));

            animateCharacterToX(targetX);
        }
    }

    /// @summary method not used in this version, present for the SensorEventListener interface
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /// @summary animate the character to a new position
    /// @param targetX the new position of the character
    private void animateCharacterToX(float targetX) {
        if (xPositionAnimator != null && xPositionAnimator.isRunning()) {
            xPositionAnimator.cancel(); // cancel running animation to launch new one
        }
        xPositionAnimator = ValueAnimator.ofFloat(characterImageView.getX(), targetX);
        xPositionAnimator.setDuration(HORIZONTAL_ANIMATION_DURATION);
        xPositionAnimator.setInterpolator(new DecelerateInterpolator()); // use decelerateInterceptor to make it more natural
        xPositionAnimator.addUpdateListener(animation ->
                characterImageView.setX((Float) animation.getAnimatedValue())
        );
        xPositionAnimator.start();
    }

    /// @summary perform a jump
    private void performJump() {
        if (isJumping) {
            return;
        }
        isJumping = true;

        // high animation for jump
        ValueAnimator riseAnimator = ValueAnimator.ofFloat(characterImageView.getY(), groundY - JUMP_HEIGHT);
        riseAnimator.setDuration(JUMP_DURATION);
        riseAnimator.setInterpolator(new DecelerateInterpolator()); // Go up quick then slow down
        riseAnimator.addUpdateListener(animation -> {
            characterImageView.setY((Float) animation.getAnimatedValue());
        });

        // Animation for fall
        ValueAnimator fallAnimator = ValueAnimator.ofFloat(groundY - JUMP_HEIGHT, groundY);
        fallAnimator.setDuration(JUMP_DURATION);
        fallAnimator.setInterpolator(new AccelerateInterpolator()); // Go down slowly then more quickly
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
                characterImageView.setY(groundY); // make sure character is on the ground
            }
        });

        // start to jump
        riseAnimator.start();
        jumpAnimator = riseAnimator; // keep a reference to the jump animation for cancellation later
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

        // cancel animation when activity is paused
        if (jumpAnimator != null && jumpAnimator.isRunning()) {
            jumpAnimator.cancel();
            isJumping = false; // Reinitialized to false
        }
    }

}
