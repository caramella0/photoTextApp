package com.phototext.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.phototext.R;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private SeekBar pitchSeekBar, speedSeekBar;
    private TextView pitchValue, speedValue;
    private RadioGroup voiceGenderGroup, themeRadioGroup;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        applySavedTheme();
        setContentView(R.layout.activity_settings);

        initViews();
        loadSettings();
        setupListeners();
    }

    /** Inizializza gli elementi UI */
    private void initViews() {
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchValue = findViewById(R.id.pitchValue);
        speedValue = findViewById(R.id.speedValue);
        voiceGenderGroup = findViewById(R.id.voiceGenderGroup);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
    }

    /** Carica le impostazioni salvate e aggiorna la UI */
    private void loadSettings() {
        preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Carica il tema selezionato
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);
        ((RadioButton) findViewById(isDarkTheme ? R.id.radioDark : R.id.radioLight)).setChecked(true);
        Log.d("SettingsActivity", "Tema caricato: " + (isDarkTheme ? "Scuro" : "Chiaro"));

        // Carica il genere della voce
        String savedVoice = preferences.getString("voiceGender", "male");
        ((RadioButton) findViewById("male".equals(savedVoice) ? R.id.voiceMale : R.id.voiceFemale)).setChecked(true);
        Log.d("SettingsActivity", "Voce caricata: " + savedVoice);

        // Carica tonalità e velocità della voce
        float savedPitch = preferences.getFloat("pitch", 1.0f);
        float savedSpeed = preferences.getFloat("speed", 1.0f);
        pitchSeekBar.setProgress((int) (savedPitch * 50));
        speedSeekBar.setProgress((int) (savedSpeed * 50));
        pitchValue.setText(String.format(Locale.US, "Tonalità: %.1f", savedPitch));
        speedValue.setText(String.format(Locale.US, "Velocità: %.1f", savedSpeed));

        Log.d("SettingsActivity", "Pitch caricato: " + savedPitch + ", Speed caricata: " + savedSpeed);
    }


    /** Imposta i listener per i controlli */
    private void setupListeners() {
        pitchSeekBar.setOnSeekBarChangeListener(createSeekBarListener("pitch", pitchValue));
        speedSeekBar.setOnSeekBarChangeListener(createSeekBarListener("speed", speedValue));

        voiceGenderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String selectedVoice = (checkedId == R.id.voiceMale) ? "male" : "female";
            preferences.edit().putString("voiceGender", selectedVoice).apply();
            Log.d("SettingsActivity", "Voce selezionata: " + selectedVoice);
        });

        findViewById(R.id.btnApplySettings).setOnClickListener(v -> applySettings());
    }

    /** Crea un listener per aggiornare in tempo reale i valori */
    private SeekBar.OnSeekBarChangeListener createSeekBarListener(String key, TextView valueText) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newValue = progress / 50.0f;
                valueText.setText(String.format(Locale.US, "%s: %.1f", key.equals("pitch") ? "Tonalità" : "Velocità", newValue));
                preferences.edit().putFloat(key, newValue).apply();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

    /** Applica il tema salvato all'avvio */
    private void applySavedTheme() {
        preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);

        Log.d("SettingsActivity", "Applying Theme: " + (isDarkTheme ? "Scuro" : "Chiaro"));

        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }


    /** Salva le impostazioni e riavvia l'activity per applicare le modifiche */
    private void applySettings() {
        SharedPreferences.Editor editor = preferences.edit();

        // Salva il tema selezionato
        boolean isDarkTheme = themeRadioGroup.getCheckedRadioButtonId() == R.id.radioDark;
        editor.putBoolean("isDarkTheme", isDarkTheme);
        Log.d("SettingsActivity", "Tema salvato: " + (isDarkTheme ? "Scuro" : "Chiaro"));

        // Salva la voce selezionata
        String selectedVoice = (voiceGenderGroup.getCheckedRadioButtonId() == R.id.voiceMale) ? "male" : "female";
        editor.putString("voiceGender", selectedVoice);
        Log.d("SettingsActivity", "Voce salvata: " + selectedVoice);

        // Salva tonalità e velocità della voce
        float pitch = pitchSeekBar.getProgress() / 50.0f;
        float speed = speedSeekBar.getProgress() / 50.0f;
        editor.putFloat("pitch", pitch);
        editor.putFloat("speed", speed);
        Log.d("SettingsActivity", "Pitch salvato: " + pitch + ", Speed salvata: " + speed);

        editor.apply();

        Toast.makeText(this, "Impostazioni aggiornate!", Toast.LENGTH_SHORT).show();

        restartActivity();
    }



    /** Riavvia l'activity per applicare il nuovo tema */
    private void restartActivity() {
        recreate(); // Riavvia l'activity senza bisogno di un nuovo Intent
    }



}
