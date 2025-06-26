package com.example.superjump;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private static final int PLATFORM_DISAPPEAR_PROBABILITY = 50; // 50% de chance (1 sur 2)
    private static final long PLATFORM_DISAPPEAR_DELAY = 2000; // 2 secondes après avoir atterri
    private static final long PLATFORM_WARNING_TIME = 500; // 0.5 seconde d'avertissement avant disparition

    private List<ImageView> existingPlatforms;
    private List<Boolean> platformDisappearFlags; // Track quelles plateformes peuvent disparaître
    private List<Boolean> platformActivated; // Track quelles plateformes ont été activées par le joueur
    private Random random;
    private Handler disappearHandler;

    public PlatformCreationHelper(Context context, ConstraintLayout gameAreaLayout, ImageView characterImageView, ImageView startPlatform) {
        this.context = context;
        this.gameAreaLayout = gameAreaLayout;
        this.characterImageView = characterImageView;
        this.existingPlatforms = new ArrayList<>();
        this.platformDisappearFlags = new ArrayList<>();
        this.platformActivated = new ArrayList<>();
        this.random = new Random();
        this.disappearHandler = new Handler();
        this.existingPlatforms.add(startPlatform);
        this.platformDisappearFlags.add(false); // La plateforme de départ ne disparaît jamais
        this.platformActivated.add(false); // La plateforme de départ n'est pas activée
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
                existingPlatforms.add(platformImageView);
                gameAreaLayout.addView(platformImageView);

                // Déterminer si cette plateforme peut disparaître
                boolean canDisappear = (i > 0) && (random.nextInt(100) < PLATFORM_DISAPPEAR_PROBABILITY);
                platformDisappearFlags.add(canDisappear);
                platformActivated.add(false); // Pas encore activée par le joueur
                if(canDisappear) platformImageView.setImageResource(R.drawable.plateforme2);
                Log.d("PlatformCreation", "Plateforme " + i + " créée - Peut disparaître: " + canDisappear);
            }
        }
        return existingPlatforms;
    }

    /**
     * NOUVELLE MÉTHODE : À appeler depuis Level2Activity quand le joueur atterrit sur une plateforme
     */
    public void onPlayerLandedOnPlatform(ImageView platform) {
        int platformIndex = existingPlatforms.indexOf(platform);

        if (platformIndex != -1 && platformIndex < platformDisappearFlags.size()) {
            // Vérifier si cette plateforme peut disparaître et n'a pas encore été activée
            if (platformDisappearFlags.get(platformIndex) && !platformActivated.get(platformIndex)) {
                platformActivated.set(platformIndex, true);
                Log.d("PlatformActivation", "Joueur atterri sur plateforme " + platformIndex + " - Démarrage du timer de disparition");
                schedulePlatformDisappearance(platform, platformIndex);
            }
        }
    }

    /**
     * Programme la disparition d'une plateforme après que le joueur ait atterri dessus
     */
    private void schedulePlatformDisappearance(ImageView platform, int platformIndex) {
        // Programmer l'avertissement (clignotement) avant disparition
        disappearHandler.postDelayed(() -> {
            if (platform.getParent() != null) { // Vérifier que la plateforme existe encore
                startPlatformWarning(platform, platformIndex);
            }
        }, 250);
    }

    /**
     * Fait clignoter la plateforme pour avertir qu'elle va disparaître
     */
    private void startPlatformWarning(ImageView platform, int platformIndex) {
        Log.d("PlatformWarning", "Début du clignotement pour la plateforme " + platformIndex);

        // Changer la couleur/texture de la plateforme pour indiquer qu'elle va disparaître
        platform.setImageResource(R.drawable.plateforme3);

        // Programmer la disparition finale
        disappearHandler.postDelayed(() -> {
            if (platform.getParent() != null) {
                makePlatformDisappear(platform, platformIndex);
            }
        }, 250);
    }

    /**
     * Fait disparaître définitivement une plateforme
     */
    private void makePlatformDisappear(ImageView platform, int platformIndex) {
        if (platform.getParent() != null) {
            gameAreaLayout.removeView(platform);
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

    public void setPlatformDisappearDelay(long delay) {
        // Vous pouvez rendre PLATFORM_DISAPPEAR_DELAY non-final pour permettre la modification
    }
}