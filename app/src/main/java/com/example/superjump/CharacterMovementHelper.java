package com.example.superjump;

import androidx.core.animation.ValueAnimator;
import androidx.core.animation.Animator;
import androidx.core.animation.AnimatorListenerAdapter;
import androidx.core.animation.DecelerateInterpolator;
import androidx.core.animation.AccelerateInterpolator;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.view.Surface;
import android.widget.ImageView;
import androidx.constraintlayout.widget.ConstraintLayout;

public class CharacterMovementHelper {
    private ImageView characterImageView;
    private ConstraintLayout gameAreaLayout;

    private float groundY;
    private boolean isJumping = false;
    private ValueAnimator jumpAnimator;
    private ValueAnimator xPositionAnimator;

    private static final float JUMP_HEIGHT = 1000f;
    private static final long JUMP_DURATION = 400;
    private static final float MOVEMENT_SENSITIVITY = 5.0f;
    private static final long HORIZONTAL_ANIMATION_DURATION = 50;

    /// @summary Class constructor
    /// @param characterImageView ImageView of the character
    /// @param gameAreaLayout ConstraintLayout of the game area
    public CharacterMovementHelper(ImageView characterImageView, ConstraintLayout gameAreaLayout) {
        this.characterImageView = characterImageView;
        this.gameAreaLayout = gameAreaLayout;

        gameAreaLayout.post(() -> {
            groundY = gameAreaLayout.getHeight() - characterImageView.getHeight() - 135f;
        });
    }

    /// @summary perform a jump
    public void performJump() {
        if (isJumping) {
            return;
        }
        isJumping = true;

        // high animation for jump
        ValueAnimator riseAnimator = ValueAnimator.ofFloat(characterImageView.getY(), groundY - JUMP_HEIGHT);
        riseAnimator.setDuration(JUMP_DURATION);
        riseAnimator.setInterpolator(new DecelerateInterpolator()); // Go up quick then slow down
        riseAnimator.addUpdateListener(animation -> {
            characterImageView.setY((Float) ((ValueAnimator)animation).getAnimatedValue());
        });

        // Animation for fall
        ValueAnimator fallAnimator = ValueAnimator.ofFloat(groundY - JUMP_HEIGHT, groundY);
        fallAnimator.setDuration(JUMP_DURATION);
        fallAnimator.setInterpolator(new AccelerateInterpolator()); // Go down slowly then more quickly
        fallAnimator.addUpdateListener(animation -> {
            characterImageView.setY((Float) ((ValueAnimator)animation).getAnimatedValue());
        });

        // trigger all animations
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

        riseAnimator.start();  // start to jump
        jumpAnimator = riseAnimator; // keep a reference to the jump animation for cancellation later
    }

    /// @summary handle sensor events
    /// @param event SensorEvent concerned
    /// @param rotation rotation of the screen
    public void handleSensorEvent(SensorEvent event, int rotation) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float rawSensorValueX;
            float movement = 0;

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
                characterImageView.setX((Float) ((ValueAnimator)animation).getAnimatedValue())
        );
        xPositionAnimator.start();
    }

    /// @summary cancel all animations (currently animation on jump and horizontal movement)
    public void cancelAnimations() {
        if (xPositionAnimator != null && xPositionAnimator.isRunning()) {
            xPositionAnimator.cancel();
        }
        if (jumpAnimator != null && jumpAnimator.isRunning()) {
            jumpAnimator.cancel();
            isJumping = false;
        }
    }

    /// @summary get isJumping private variable
    /// @return true if the character is jumping
    public boolean getIsJumping() {
        return isJumping;
    }

    /// @summary update the vertical position of character when it hits the bottom
    public void updateGroundY() {
        if (gameAreaLayout != null && characterImageView != null) {
            groundY = gameAreaLayout.getHeight() - characterImageView.getHeight() - 135f;
        }
    }
}