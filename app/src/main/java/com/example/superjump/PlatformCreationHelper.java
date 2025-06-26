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
    private static final int NOMBRE_PLATEFORMES_DEFAUT = 60;
    private static final int LARGEUR_PLATEFORME_DP_DEFAUT = 40;
    private static final int HAUTEUR_PLATEFORME_DP_DEFAUT = 10;
    private static final int MAX_PLACEMENT_ATTEMPTS_DEFAUT = 500;

    private static final int HAUTEUR_MAX_SAUT_PERSONNAGE_DP = 35;
    private static final int PORTEE_HORIZONTALE_MAX_SAUT_PERSONNAGE_DP = 120;
    private static final int OFFSET_Y_PREMIERE_PLATEFORME_DP = 0;
    private static final int ESPACEMENT_VERTICAL_MIN_DP = 80;

    private List<ImageView> existingPlatforms;
    private Random random;

    /// @summary Class constructor
    /// @param context Context of the activity
    /// @param gameAreaLayout ConstraintLayout of the game area
    /// @param characterImageView ImageView of the character
    /// @param startPlatform ImageView of the starting platform
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
        do {
            existingPlatforms = creerPlateformes(NOMBRE_PLATEFORMES_DEFAUT, LARGEUR_PLATEFORME_DP_DEFAUT, HAUTEUR_PLATEFORME_DP_DEFAUT, MAX_PLACEMENT_ATTEMPTS_DEFAUT);
        } while (existingPlatforms.isEmpty());
        return existingPlatforms;
    }

    /// @summary create platforms above a reference platform
    /// @param referencePlatform the platform to use as reference for positioning new platforms above
    /// @param numberOfPlatforms number of platforms to create
    /// @return List of newly created platforms
    public List<ImageView> createPlatformsAbove(ImageView referencePlatform, int numberOfPlatforms) {
        List<ImageView> newPlatforms = new ArrayList<>();

        if (gameAreaLayout == null || context == null || referencePlatform == null) {
            Log.w("PlatformCreation", "Impossible de créer des plateformes au-dessus - paramètres manquants");
            return newPlatforms;
        }

        int gameAreaWidth = gameAreaLayout.getWidth();
        int gameAreaHeight = gameAreaLayout.getHeight();

        if (gameAreaWidth == 0 || gameAreaHeight == 0) {
            gameAreaLayout.post(() -> createPlatformsAbove(referencePlatform, numberOfPlatforms));
            return newPlatforms;
        }

        final float scale = context.getResources().getDisplayMetrics().density;
        int largeurPlateformePx = (int) (LARGEUR_PLATEFORME_DP_DEFAUT * scale + 0.5f);
        int hauteurPlateformePx = (int) (HAUTEUR_PLATEFORME_DP_DEFAUT * scale + 0.5f);
        int hauteurMaxSautPx = (int) (HAUTEUR_MAX_SAUT_PERSONNAGE_DP * scale + 0.5f);
        int porteeHorizontaleMaxSautPx = (int) (PORTEE_HORIZONTALE_MAX_SAUT_PERSONNAGE_DP * scale + 0.5f);
        int espacementVerticalMinPx = (int) (ESPACEMENT_VERTICAL_MIN_DP * scale + 0.5f);

        ImageView currentReferencePlatform = referencePlatform;

        for (int i = 0; i < numberOfPlatforms; i++) {
            ImageView platformImageView = new ImageView(context);
            platformImageView.setElevation(0);
            platformImageView.setImageResource(R.drawable.plateforme_v1);
            ConstraintLayout.LayoutParams params = new ConstraintLayout.LayoutParams(
                    largeurPlateformePx,
                    hauteurPlateformePx
            );
            platformImageView.setLayoutParams(params);

            int randomX = 0, randomY = 0;
            boolean isOverlapping;
            int placementAttempts = 0;
            Rect newPlatformRect = new Rect();

            do {
                isOverlapping = false;
                placementAttempts++;

                // Obtenir la position de la plateforme de référence
                int refX = (int) currentReferencePlatform.getX();
                int refY = (int) currentReferencePlatform.getY();

                // Calculer la position X accessible depuis la plateforme de référence
                int minX = Math.max(0, refX + (currentReferencePlatform.getWidth()/2) - porteeHorizontaleMaxSautPx - (largeurPlateformePx / 2));
                int maxX = Math.min(gameAreaWidth - largeurPlateformePx, refX + (currentReferencePlatform.getWidth()/2) + porteeHorizontaleMaxSautPx - (largeurPlateformePx / 2));

                if (maxX <= minX) {
                    randomX = random.nextInt(Math.max(1, gameAreaWidth - largeurPlateformePx));
                } else {
                    randomX = random.nextInt(maxX - minX + 1) + minX;
                }

                // Calculer la position Y au-dessus de la plateforme de référence
                int minY_accessible = refY - hauteurMaxSautPx;
                int maxY_accessible = refY - hauteurPlateformePx - espacementVerticalMinPx;

                if (maxY_accessible < minY_accessible) {
                    maxY_accessible = minY_accessible;
                }

                // S'assurer que la nouvelle plateforme ne sort pas du haut de l'écran
                minY_accessible = Math.max(0, minY_accessible);
                maxY_accessible = Math.max(0, maxY_accessible);

                if (maxY_accessible >= minY_accessible) {
                    randomY = random.nextInt(Math.max(1, maxY_accessible - minY_accessible + 1)) + minY_accessible;
                } else {
                    randomY = minY_accessible;
                }

                // Valeurs finales pour éviter les coordonnées hors écran
                randomX = Math.max(0, Math.min(randomX, gameAreaWidth - largeurPlateformePx));
                randomY = Math.max(0, Math.min(randomY, gameAreaHeight - hauteurPlateformePx));

                newPlatformRect.set(randomX, randomY, randomX + largeurPlateformePx, randomY + hauteurPlateformePx);

                // Vérifier les chevauchements avec les plateformes existantes
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

                // Vérifier aussi avec les nouvelles plateformes créées
                if (!isOverlapping) {
                    for (ImageView newPlatform : newPlatforms) {
                        Rect newRect = new Rect(
                                (int) newPlatform.getX(),
                                (int) newPlatform.getY(),
                                (int) newPlatform.getX() + newPlatform.getWidth(),
                                (int) newPlatform.getY() + newPlatform.getHeight()
                        );
                        if (Rect.intersects(newPlatformRect, newRect)) {
                            isOverlapping = true;
                            break;
                        }
                    }
                }

            } while (isOverlapping && placementAttempts < MAX_PLACEMENT_ATTEMPTS_DEFAUT);

            if (!isOverlapping) {
                platformImageView.setX(randomX);
                platformImageView.setY(randomY);
                Log.d("PlatformCreation", "Nouvelle plateforme créée au-dessus à la position Y: " + randomY);

                newPlatforms.add(platformImageView);
                existingPlatforms.add(platformImageView);
                gameAreaLayout.addView(platformImageView);

                // Utiliser cette nouvelle plateforme comme référence pour la suivante
                currentReferencePlatform = platformImageView;
            } else {
                Log.w("PlatformCreation", "Impossible de placer la plateforme " + i + " après " + placementAttempts + " tentatives");
            }
        }

        Log.d("PlatformCreation", "Créées " + newPlatforms.size() + " nouvelles plateformes au-dessus");
        return newPlatforms;
    }

    /// @summary create platforms function
    /// @param nombrePlateformes number of platforms to create
    /// @param largeurPlateformeDp width of platforms in dp
    /// @param hauteurPlateformeDp height of platforms in dp
    /// @param maxAttempts maximum number of attempts to place a platform
    /// @return List of created platforms
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

        // Boucle pour la création des plateformes
        for (int i = 0; i < nombrePlateformes && (i == 0 || newPlatformRect.top >= 600); i++) {
            ImageView platformImageView = new ImageView(context);
            platformImageView.setElevation(0);
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
                    // Centrer la première plateforme à l'écran
                    if (characterImageView != null && characterImageView.getY() > 0) {
                        // Retirer la hauteur de saut de la position du personnage pour que la plateforme générée soit accessible
                        randomY = (int) (characterImageView.getY() - hauteurMaxSautPx + random.nextInt(hauteurMaxSautPx / 2));
                        // S'assurer que la plateforme n'est pas hors écran (hauteur)
                        randomY = Math.max(0, Math.min(randomY, gameAreaHeight - hauteurPlateformePx));
                    } else {
                        randomY = gameAreaHeight - hauteurPlateformePx - offsetYPremierePlateformePx;
                    }
                    randomX = random.nextInt(Math.max(1, gameAreaWidth - largeurPlateformePx));
                } else {
                    if (existingPlatforms.isEmpty()) {
                        Log.e("PlatformCreation", "Aucune plateforme existante pour référence");
                        break;
                    }

                    // Obtenir la dernière plateforme ajoutée comme référence
                    ImageView refPlatform = existingPlatforms.get(existingPlatforms.size() - 1);
                    int refX = (int) refPlatform.getX();
                    int refY = (int) refPlatform.getY();

                    // Calculer la position X accessible
                    int minX = Math.max(0, refX + (refPlatform.getWidth()/2) - porteeHorizontaleMaxSautPx - (largeurPlateformePx / 2));
                    int maxX = Math.min(gameAreaWidth - largeurPlateformePx, refX + (refPlatform.getWidth()/2) + porteeHorizontaleMaxSautPx - (largeurPlateformePx / 2));

                    if (maxX <= minX) {
                        randomX = random.nextInt(Math.max(1, gameAreaWidth - largeurPlateformePx));
                    } else {
                        randomX = random.nextInt(maxX - minX + 1) + minX;
                    }

                    // Calculer la position Y accessible et plus haute que la plateforme précédente
                    int minY_accessible = refY - hauteurMaxSautPx;
                    int maxY_accessible = refY - hauteurPlateformePx - espacementVerticalMinPx;
                    if (maxY_accessible < minY_accessible) {
                        maxY_accessible = minY_accessible;
                    }
                    randomY = random.nextInt(Math.max(1, maxY_accessible - minY_accessible + 1)) + minY_accessible;
                }

                // Valeurs finales pour éviter les coordonnées hors écran
                randomX = Math.max(0, Math.min(randomX, gameAreaWidth - largeurPlateformePx));
                randomY = Math.max(0, Math.min(randomY, gameAreaHeight - hauteurPlateformePx));

                newPlatformRect.set(randomX, randomY, randomX + largeurPlateformePx, randomY + hauteurPlateformePx);

                // Vérifier les chevauchements avec les plateformes existantes
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
                Log.d("PlatformCreation", "Plateforme créée à la position Y: " + randomY);
                existingPlatforms.add(platformImageView);
                gameAreaLayout.addView(platformImageView);
            } else {
                Log.w("PlatformCreation", "Impossible de placer la plateforme " + i + " après " + placementAttempts + " tentatives");
            }
        }

        return existingPlatforms;
    }

    /// @summary getter of existing platforms list
    /// @return List of existing platforms
    public List<ImageView> getExistingPlatforms() {
        return existingPlatforms;
    }

    /// @summary Remove a platform from the existing platforms list and from the layout
    /// @param platform The platform to remove
    public void removePlatform(ImageView platform) {
        if (existingPlatforms.contains(platform)) {
            existingPlatforms.remove(platform);
            gameAreaLayout.removeView(platform);
            Log.d("PlatformCreation", "Plateforme supprimée");
        }
    }

    /// @summary Clear all platforms except the start platform
    public void clearPlatformsExceptStart() {
        ImageView startPlatform = null;
        if (!existingPlatforms.isEmpty()) {
            startPlatform = existingPlatforms.get(0); // La première plateforme est la plateforme de départ
        }

        // Supprimer toutes les plateformes du layout
        for (ImageView platform : existingPlatforms) {
            if (platform != startPlatform) {
                gameAreaLayout.removeView(platform);
            }
        }

        // Vider la liste et remettre seulement la plateforme de départ
        existingPlatforms.clear();
        if (startPlatform != null) {
            existingPlatforms.add(startPlatform);
        }

        Log.d("PlatformCreation", "Toutes les plateformes supprimées sauf la plateforme de départ");
    }
}