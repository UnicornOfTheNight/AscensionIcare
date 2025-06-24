package com.example.superjump;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

public class Level1Activity extends AppCompatActivity implements SensorEventListener {
    // screen elements
    private ConstraintLayout gameAreaLayout;
    private ImageView characterImageView;

    // sensor elements to move character
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private CharacterMovementHelper characterMovementHelper;
    private final Handler jumpHandler = new Handler();
    private Runnable repetitiveJumpRunnable;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level1);

        characterImageView = findViewById(R.id.imageView_perso);
        gameAreaLayout = findViewById(R.id.main);

        gameAreaLayout.post(() -> {
            characterMovementHelper = new CharacterMovementHelper(characterImageView, gameAreaLayout);

            characterMovementHelper.updateGroundY(); // update vertical position

            // repeat jump
            repetitiveJumpRunnable = new Runnable() {
                @Override
                public void run() {
                    if (characterMovementHelper != null && !characterMovementHelper.getIsJumping()) {
                        characterMovementHelper.performJump();
                    }
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