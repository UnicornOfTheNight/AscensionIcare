package com.example.superjump;

import android.content.Context;
import android.graphics.Rect;
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
    private static final int NOMBRE_PLATEFORMES_DEFAUT = 2;
    private static final int LARGEUR_PLATEFORME_DP_DEFAUT = 80; //obtenir les dp crees par gabin
    private static final int HAUTEUR_PLATEFORME_DP_DEFAUT = 25; //obtenir les dp crees par gabin
    private static final int MAX_PLACEMENT_ATTEMPTS_DEFAUT = 50;

    private static final int HAUTEUR_MAX_SAUT_PERSONNAGE_DP = 150;
    private static final int PORTEE_HORIZONTALE_MAX_SAUT_PERSONNAGE_DP = 120;
    private static final int OFFSET_Y_PREMIERE_PLATEFORME_DP = 50;
    private static final int ESPACEMENT_VERTICAL_MIN_DP = 80; // Nouvel espacement minimum

    private List<ImageView> existingPlatforms;
    private Random random;

    /// @summary Class constructor
    /// @param context Context of the activity
    /// @param gameAreaLayout ConstraintLayout of the game area
    /// @param characterImageView ImageView of the character
    public PlatformCreationHelper(Context context, ConstraintLayout gameAreaLayout, ImageView characterImageView, ImageView startPlatform) {
        this.context = context;
        this.gameAreaLayout = gameAreaLayout;
        this.characterImageView = characterImageView;
        this.existingPlatforms = new ArrayList<>();
        this.random = new Random();
        this.existingPlatforms.add(startPlatform);
    }

    /// @summary create platforms function
    /// @return List of created platforms
    public List<ImageView> creerPlateformes() {
        do{
            existingPlatforms = creerPlateformes(NOMBRE_PLATEFORMES_DEFAUT, LARGEUR_PLATEFORME_DP_DEFAUT, HAUTEUR_PLATEFORME_DP_DEFAUT, MAX_PLACEMENT_ATTEMPTS_DEFAUT);
        }while(existingPlatforms.isEmpty());
        return existingPlatforms;
    }

    /// @summary create platforms function
    /// @param nombrePlateformes number of platforms to create
    /// @param largeurPlateformeDp width of platforms in dp
    /// @param hauteurPlateformeDp height of platforms in dp
    /// @param maxAttempts maximum number of attempts to place a platform
    /// @return List of created platforms
    // change to public to overload creerPlateformes function if parameters can be changed
    private List<ImageView> creerPlateformes(int nombrePlateformes, int largeurPlateformeDp, int hauteurPlateformeDp, int maxAttempts) {
        if (gameAreaLayout == null || context == null) {
            return existingPlatforms;
        }

        int gameAreaWidth = gameAreaLayout.getWidth();
        int gameAreaHeight = gameAreaLayout.getHeight();

        if (gameAreaWidth == 0 || gameAreaHeight == 0) { //check that gamearea layouts are available
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
        // loop for platforms creation
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

                if (i == 0) { // center first plateform on screen
                     if (characterImageView != null && characterImageView.getY() > 0) {
                         // take back jump height from character position so the platform generated with random position will be accessible from the game start point
                        randomY = (int) (characterImageView.getY() - hauteurMaxSautPx + random.nextInt(hauteurMaxSautPx / 2));
                        // make sure the platform is not offscreen (height)
                        randomY = Math.max(0, Math.min(randomY, gameAreaHeight - hauteurPlateformePx));
                     } else {
                        randomY = gameAreaHeight - hauteurPlateformePx - offsetYPremierePlateformePx;
                     }
                     randomX = random.nextInt(Math.max(1, gameAreaWidth - largeurPlateformeDp));
                } else {
                    if (existingPlatforms.isEmpty()) {
                        // log erreur
                        break;
                    }
                    // get last added platform as reference
                    ImageView refPlatform = existingPlatforms.get(existingPlatforms.size() - 1);
                    int refX = (int) refPlatform.getX();
                    int refY = (int) refPlatform.getY();

                    // calculate x position reachable
                    int minX = Math.max(0, refX + (refPlatform.getWidth()/2) - porteeHorizontaleMaxSautPx - (largeurPlateformeDp / 2));
                    int maxX = Math.min(gameAreaWidth - largeurPlateformeDp, refX + (refPlatform.getWidth()/2) + porteeHorizontaleMaxSautPx - (largeurPlateformeDp / 2));

                    if (maxX <= minX) {
                        randomX = random.nextInt(Math.max(1, gameAreaWidth - largeurPlateformeDp));
                    } else {
                        randomX = random.nextInt(maxX - minX + 1) + minX;
                    }
                    // calculate y position reachable and higher than previous platform
                    int minY_accessible = refY - hauteurMaxSautPx;
                    int maxY_accessible = refY - hauteurPlateformePx - espacementVerticalMinPx;
                    if (maxY_accessible < minY_accessible) {
                        maxY_accessible = minY_accessible;
                    }
                    randomY = random.nextInt(Math.max(1, maxY_accessible - minY_accessible + 1)) + minY_accessible;
                }

                // Final values to avoid offscreen coordinates
                randomX = Math.max(0, Math.min(randomX, gameAreaWidth - largeurPlateformeDp));
                randomY = Math.max(0, Math.min(randomY, gameAreaHeight - hauteurPlateformePx));

                newPlatformRect.set(randomX, randomY, randomX + largeurPlateformeDp, randomY + hauteurPlateformePx);

                // check for overlays with existing platforms
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
            }
        }
        return existingPlatforms;
    }

    /// @summary getter of existing platforms list
    /// @return List of existing platforms
    public List<ImageView> getExistingPlatforms() {
        return existingPlatforms;
    }
}