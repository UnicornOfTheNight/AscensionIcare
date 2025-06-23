package com.example.superjump;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout; // Importez LinearLayout
import android.widget.TextView;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
// Assurez-vous d'avoir les imports pour EdgeToEdge si vous l'utilisez encore
// import androidx.activity.EdgeToEdge;
// import androidx.core.graphics.Insets;
// import androidx.core.view.ViewCompat;
// import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    // Layouts elements
    private TabLayout tabLayoutMenu;
    private LinearLayout layoutLevelsButtons;
    private LinearLayout layoutScoresButtons;
    private LinearLayout layoutParametersButtons;

    // Level buttons
    private ArrayList<Button> lst_btLevels;;

    // Scores text
    private TextView textArea_scores;

    // Parameters buttons
    private Button bt_effacer_donnees;
    private SwitchMaterial switch_son;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Manage insets for EdgeToEdge
         ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
         });

        // initialize elements
        tabLayoutMenu = findViewById(R.id.tabLayout_menu);
        layoutLevelsButtons = findViewById(R.id.layout_levels_buttons);
        layoutScoresButtons = findViewById(R.id.layout_scores_buttons);
        layoutParametersButtons = findViewById(R.id.layout_parameters_buttons);
        textArea_scores = findViewById(R.id.txt_scores);
        bt_effacer_donnees = findViewById(R.id.bt_effacerDonnees);
        switch_son = findViewById(R.id.switch_parametres_son);

        boolean sonActive = loadSoundPreference();
        switch_son.setChecked(sonActive);

        // show play tab item elements by default
        updateButtonVisibility(0);

        // listeners for tab activity
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
    }

     /// @summary Load the sound preference from SharedPreferences
     /// @return true if sound is enabled, false otherwise
     private boolean loadSoundPreference() {
         SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
         return prefs.getBoolean("sound_enabled", true); // true par défaut
     }

     /// @summary Save the sound preference to SharedPreferences
     /// @param isEnabled true if sound should be enabled, false otherwise
     private void saveSoundPreference(boolean isEnabled) {
         SharedPreferences.Editor editor = getSharedPreferences("app_prefs", MODE_PRIVATE).edit();
         editor.putBoolean("sound_enabled", isEnabled);
         editor.apply();
     }

     /// @summary Update the visibility of the buttons based on the selected tab item
     /// @param tabPosition The position of the selected tab item
    private void updateButtonVisibility(int tabPosition) {
        // set all buttons visibility to gone
        layoutLevelsButtons.setVisibility(View.GONE);
        layoutScoresButtons.setVisibility(View.GONE);
        layoutParametersButtons.setVisibility(View.GONE);

        // show layout corresponding to the selected tab item
        switch (tabPosition) {
            case 0: // tab item "Jouer"
                layoutLevelsButtons.setVisibility(View.VISIBLE);
                break;
            case 1: // tab item "Scores"
                layoutScoresButtons.setVisibility(View.VISIBLE);
                break;
            case 2: // tab item "Paramètres"
                layoutParametersButtons.setVisibility(View.VISIBLE);
                break;
        }
    }
}