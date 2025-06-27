package com.example.superjump;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayoutMenu;

    public static String scoreNvx1 = "Niveau 1 : No score";
    public static String scoreNvx2 = "Niveau 2 : No score";
    public static String scoreNvx3 = "Niveau 3 : No score";

    public static TextView txtScores;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Au démarrage : afficher loading, masquer le menu principal
        findViewById(R.id.loading_container).setVisibility(View.VISIBLE);
        findViewById(R.id.main_container).setVisibility(View.GONE);
        findViewById(R.id.layout_scores_buttons).setVisibility(View.INVISIBLE);

        txtScores = findViewById(R.id.txt_scores);
        txtScores.setText("Mes scores : \n\n" + scoreNvx1 + "\n" + scoreNvx2 + "\n" + scoreNvx3);

        // Après 3 secondes : masquer loading, afficher le menu principal
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Masquer la page de loading
                findViewById(R.id.loading_container).setVisibility(View.GONE);

                // Afficher la page principale
                findViewById(R.id.main_container).setVisibility(View.VISIBLE);
            }
        }, 3000); // 3 secondes

        // Initialisation des éléments du layout
        tabLayoutMenu = findViewById(R.id.tabLayout_menu);

        tabLayoutMenu.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateButtonVisibility(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }

        });

        // Actions pour les boutons des niveaux
        findViewById(R.id.bt_nvx1).setOnClickListener(v -> {
            // Lancer l'activité pour le Niveau 1
            Intent homeIntent = new Intent(MainActivity.this, Level1Activity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            finish();
        });

        findViewById(R.id.bt_nvx2).setOnClickListener(v -> {
            // Lancer l'activité pour le Niveau 2
            Intent homeIntent = new Intent(MainActivity.this, Level2Activity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            finish();
        });

        findViewById(R.id.bt_nvx3).setOnClickListener(v -> {
            // Lancer l'activité pour le Niveau 3
            Intent homeIntent = new Intent(MainActivity.this, Level4Activity.class);
            homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(homeIntent);
            finish();
        });
    }

    /// @summary Update the visibility of the buttons based on the selected tab item
    /// @param tabPosition The position of the selected tab item
    private void updateButtonVisibility(int tabPosition) {
        // set all buttons visibility to gone
        View layoutLevelsButtons = findViewById(R.id.contenaire_boutons_niveaux);
        layoutLevelsButtons.setVisibility(View.GONE);
        View layoutScoresButtons = findViewById(R.id.layout_scores_buttons);
        layoutScoresButtons.setVisibility(View.GONE);

        View layout = findViewById(R.id.content_container);
        layout.setVisibility(View.GONE);

        // show layout corresponding to the selected tab item
        switch (tabPosition) {
            case 0: // tab item "Jouer"
                layoutLevelsButtons.setVisibility(View.VISIBLE);
                layout.setVisibility(View.VISIBLE);
                break;
            case 1: // tab item "Scores"
                layout.setVisibility(View.GONE);
                layoutScoresButtons.setVisibility(View.VISIBLE);
                findViewById(R.id.layout_scores_buttons).setVisibility(View.VISIBLE);
                break;
            default:
                layoutLevelsButtons.setVisibility(View.VISIBLE);
                layout.setVisibility(View.VISIBLE);
                break;
        }
    }
}