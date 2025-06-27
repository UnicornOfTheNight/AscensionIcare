package com.example.superjump;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;

public class MainActivity extends AppCompatActivity {

    private TabLayout tabLayoutMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Au démarrage : afficher loading, masquer le menu principal
        findViewById(R.id.loading_container).setVisibility(View.VISIBLE);
        findViewById(R.id.main_container).setVisibility(View.GONE);

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

        // Actions pour les boutons des niveaux
        findViewById(R.id.bt_nvx1).setOnClickListener(v -> {
            // Lancer l'activité pour le Niveau 1
        });

        findViewById(R.id.bt_nvx2).setOnClickListener(v -> {
            // Lancer l'activité pour le Niveau 2
        });

        findViewById(R.id.bt_nvx3).setOnClickListener(v -> {
            // Lancer l'activité pour le Niveau 3
        });
    }
}