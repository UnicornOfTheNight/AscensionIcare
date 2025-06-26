package com.example.superjump;

import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class PlatformCreationHelper {
    private Context context;
    private ConstraintLayout gameAreaLayout;
    private ImageView characterImageView;

    // platforms constants
    private static final int NOMBRE_PLATEFORMES_DEFAUT = 6;
    private static final int LARGEUR_PLATEFORME_DP_DEFAUT = 80;
    private static final int HAUTEUR_PLATEFORME_DP_DEFAUT = 25;
    private static final int MAX_PLACEMENT_ATTEMPTS_DEFAUT = 50;

    private static final int HAUTEUR_MAX_SAUT_PERSONNAGE_DP = 150;
    private static final int PORTEE_HORIZONTALE_MAX_SAUT_PERSONNAGE_DP = 120;
    private static final int OFFSET_Y_PREMIERE_PLATEFORME_DP = 50;
    private static final int ESPACEMENT_VERTICAL_MIN_DP = 80;

    // Nouvelles constantes pour les plateformes qui disparaissent
    private static final int PLATFORM_DISAPPEAR_PROBABILITY = 33; // 33% de chance (1 sur 3)
    private static final long PLATFORM_DISAPPEAR_DELAY_MIN = 2000; // 2 secondes minimum
    private static final long PLATFORM_DISAPPEAR_DELAY_MAX = 5000; // 5 secondes maximum
    private static final long PLATFORM_WARNING_TIME = 1000; // 1 seconde d'avertissement avant disparition

    private List<ImageView> existingPlatforms;
    private List<Boolean> platformDisappearFlags; // Track quelles plateformes peuvent disparaître
    private Random random;
    private Handler disappearHandler;

    public PlatformCreationHelper(Context context, ConstraintLayout gameAreaLayout, ImageView characterImageView, ImageView startPlatform) {
        this.context = context;
        this.gameAreaLayout = gameAreaLayout;
        this.characterImageView = characterImageView;
        this.existingPlatforms = new ArrayList<>();
        this.platformDisappearFlags = new ArrayList<>();
        this.random = new Random();
        this.disappearHandler = new Handler();
        this.existingPlatforms.add(startPlatform);
        this.platformDisappearFlags.add(false); // La plateforme de départ ne disparaît jamais
    }

    public List<ImageView> creerPlateformes() {
        do {
            existingPlatforms = creerPlateformes(NOMBRE_PLATEFORMES_DEFAUT, LARGEUR_PLATEFORME_DP_DEFAUT, HAUTEUR_PLATEFORME_DP_DEFAUT, MAX_PLACEMENT_ATTEMPTS_DEFAUT);
        } while (existingPlatforms.isEmpty());
        return existingPlatforms;
    }

    private List<ImageView> creerPlateformes(int nombrePlateformes, int largeurPlateformeDp, int hauteurPlateformeDp, int maxAttempts) {
        if (gameAreaLayout == null || context == null) {
            return existingPlatforms;
        }

        int gameAreaWidth = gameAreaLayout.getWidth();
        int gameAreaHeight = gameAreaLayout.getHeight();

        if (gameAreaWidth == 0 || gameAreaHeight == 0) {
            gameAreaLayout.post(() -> creerPlateformes(nombrePlateformes, largeurPlateformeDp, hauteurPlateformeDp, maxAttempts));
            return existingPlatforms;
        }

        final float scale = context.getResources().getDisplayMetrics().density;
        int largeurPlateformePx = (int) (largeurPlateformeDp * scale + 0.5f);
        int hauteurPlateformePx = (int) (hauteurPlateformeDp * scale + 0.5f);
        int hauteurMaxSautPx = (int) (HAUTEUR_MAX_SAUT_PERSONNAGE_DP * scale + 0.5f);
        int porteeHorizontaleMaxSautPx = (int) (PORTEE_HORIZONTALE_MAX_SAUT_PERSONNAGE_DP * scale + 0.5f);
        int offsetYPremierePlateformePx = (int) (OFFSET_Y_PREMIERE_PLATEFORME_DP * scale + 0.5f);
        int espacementVerticalMinPx = (int) (ESPACEMENT_VERTICAL_MIN_DP * scale + 0.5f);

        Rect newPlatformRect = new Rect();

        for (int i = 0; i < nombrePlateformes && (i == 0 || newPlatformRect.top >= 600); i++) {
            ImageView platformImageView = new ImageView(context);
            platformImageView.setImageResource(R.drawable.plateforme_v1);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    largeurPlateformePx,
                    hauteurPlateformePx
            );
            platformImageView.setLayoutParams(params);

            int randomX = 0, randomY = 0;
            boolean isOverlapping;
            int placementAttempts = 0;

            do {
                isOverlapping = false;
                placementAttempts++;

                if (i == 0) {
                    if (characterImageView != null && characterImageView.getY() > 0) {
                        randomY = (int) (characterImageView.getY() - hauteurMaxSautPx + random.nextInt(hauteurMaxSautPx / 2));
                        randomY = Math.max(0, Math.min(randomY, gameAreaHeight - hauteurPlateformePx));
                    } else {
                        randomY = gameAreaHeight - hauteurPlateformePx - offsetYPremierePlateformePx;
                    }
                    randomX = random.nextInt(Math.max(1, gameAreaWidth - largeurPlateformeDp));
                } else {
                    if (existingPlatforms.isEmpty()) {
                        break;
                    }
                    ImageView refPlatform = existingPlatforms.get(existingPlatforms.size() - 1);
                    int refX = (int) refPlatform.getX();
                    int refY = (int) refPlatform.getY();

                    int minX = Math.max(0, refX + (refPlatform.getWidth()/2) - porteeHorizontaleMaxSautPx - (largeurPlateformeDp / 2));
                    int maxX = Math.min(gameAreaWidth - largeurPlateformeDp, refX + (refPlatform.getWidth()/2) + porteeHorizontaleMaxSautPx - (largeurPlateformeDp / 2));

                    if (maxX <= minX) {
                        randomX = random.nextInt(Math.max(1, gameAreaWidth - largeurPlateformeDp));
                    } else {
                        randomX = random.nextInt(maxX - minX + 1) + minX;
                    }

                    int minY_accessible = refY - hauteurMaxSautPx;
                    int maxY_accessible = refY - hauteurPlateformePx - espacementVerticalMinPx;
                    if (maxY_accessible < minY_accessible) {
                        maxY_accessible = minY_accessible;
                    }
                    randomY = random.nextInt(Math.max(1, maxY_accessible - minY_accessible + 1)) + minY_accessible;
                }

                randomX = Math.max(0, Math.min(randomX, gameAreaWidth - largeurPlateformeDp));
                randomY = Math.max(0, Math.min(randomY, gameAreaHeight - hauteurPlateformePx));

                newPlatformRect.set(randomX, randomY, randomX + largeurPlateformeDp, randomY + hauteurPlateformePx);

                for (ImageView existingPlatform : existingPlatforms) {
                    Rect existingRect = new Rect(
                            (int) existingPlatform.getX(),
                            (int) existingPlatform.getY(),
                            (int) existingPlatform.getX() + existingPlatform.getWidth(),
                            (int) existingPlatform.getY() + existingPlatform.getHeight()
                    );
                    if (Rect.intersects(newPlatformRect, existingRect)) {
                        isOverlapping = true;
                        break;
                    }
                }
            } while (isOverlapping && placementAttempts < maxAttempts);

            if (!isOverlapping) {
                platformImageView.setX(randomX);
                platformImageView.setY(randomY);
                Log.d("landOnPlatform", "position " + randomY);


                // NOUVEAU : Déterminer si cette plateforme peut disparaître (1 chance sur 3)
                boolean canDisappear = (i > 0) && (random.nextInt(100) < PLATFORM_DISAPPEAR_PROBABILITY);
                platformDisappearFlags.add(canDisappear);

                // Si la plateforme peut disparaître, programmer sa disparition
                if (i == 2) {
                    i++;
                    platformImageView.setImageResource(R.drawable.plateforme2);
                    TimerTask task = new TimerTask() {
                        public void run() {
                            platformImageView.setImageResource(R.drawable.plateforme3);
                        }
                    };
                    Timer timer = new Timer("Timer");

                    long delay = 1000L;
                    timer.schedule(task, delay);
                    //schedulePlatformDisappearance(platformImageView, existingPlatforms.size() - 1);
                }else{
                    platformImageView.setImageResource(R.drawable.plateforme1);

                }

                existingPlatforms.add(platformImageView);
                gameAreaLayout.addView(platformImageView);
            }
        }
        return existingPlatforms;
    }
    int i = 2;

    /**
     * Programme la disparition d'une plateforme après un délai aléatoire
     */
    private void schedulePlatformDisappearance(ImageView platform, int platformIndex) {
        // Délai aléatoire entre min et max
        long delay = PLATFORM_DISAPPEAR_DELAY_MIN +
                random.nextInt((int)(PLATFORM_DISAPPEAR_DELAY_MAX - PLATFORM_DISAPPEAR_DELAY_MIN));

        // Programmer l'avertissement (clignotement)
        disappearHandler.postDelayed(() -> {
            if (platform.getParent() != null) { // Vérifier que la plateforme existe encore
                startPlatformWarning(platform, platformIndex);
            }
        }, delay - PLATFORM_WARNING_TIME);
    }

    /**
     * Fait clignoter la plateforme pour avertir qu'elle va disparaître
     */
    private void startPlatformWarning(ImageView platform, int platformIndex) {
        // Faire clignoter la plateforme 5 fois
        final int[] blinkCount = {0};
        final Runnable blinkRunnable = new Runnable() {
            @Override
            public void run() {
                if (platform.getParent() != null && blinkCount[0] < 10) { // 10 = 5 clignotements
                    platform.setAlpha(blinkCount[0] % 2 == 0 ? 0.3f : 1.0f);
                    blinkCount[0]++;
                    disappearHandler.postDelayed(this, 100); // Clignoter toutes les 100ms
                } else if (platform.getParent() != null) {
                    // Faire disparaître la plateforme après le clignotement
                    makePlatformDisappear(platform, platformIndex);
                }
            }
        };
        disappearHandler.post(blinkRunnable);
    }

    /**
     * Fait disparaître définitivement une plateforme
     */
    private void makePlatformDisappear(ImageView platform, int platformIndex) {
        if (platform.getParent() != null) {
            gameAreaLayout.removeView(platform);

            // Optionnel : marquer la plateforme comme disparue dans la liste
            // (garder l'index pour éviter les décalages)
            if (platformIndex < platformDisappearFlags.size()) {
                platformDisappearFlags.set(platformIndex, false); // Marquer comme disparue
            }

            Log.d("PlatformDisappear", "Plateforme disparue à l'index " + platformIndex);
        }
    }

    /**
     * Méthode pour vérifier si une plateforme a disparu (utile pour les collisions)
     */
    public boolean isPlatformDisappeared(int index) {
        if (index < 0 || index >= existingPlatforms.size()) return true;
        return existingPlatforms.get(index).getParent() == null;
    }

    /**
     * Nettoie les handlers lors de la destruction
     */
    public void cleanup() {
        if (disappearHandler != null) {
            disappearHandler.removeCallbacksAndMessages(null);
        }
    }

    public List<ImageView> getExistingPlatforms() {
        return existingPlatforms;
    }

    // Méthodes pour personnaliser les paramètres de disparition
    public void setPlatformDisappearProbability(int probability) {
        // Vous pouvez rendre PLATFORM_DISAPPEAR_PROBABILITY non-final pour permettre la modification
    }

    public void setPlatformDisappearDelayRange(long minDelay, long maxDelay) {
        // Vous pouvez rendre les constantes de délai non-finales pour permettre la modification
    }
}