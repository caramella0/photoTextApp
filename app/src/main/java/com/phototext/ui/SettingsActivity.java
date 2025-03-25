package com.phototext.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.phototext.R;
import com.phototext.tts.TextToSpeechManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private SeekBar pitchSeekBar, speedSeekBar;
    private TextView pitchValue, speedValue;
    private RadioGroup voiceGenderGroup, themeRadioGroup;
    private SharedPreferences preferences;
    private TextToSpeechManager ttsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inizializza SharedPreferences e applica il tema prima di caricare il layout
        preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        applySavedTheme();

        setContentView(R.layout.activity_settings);
        ttsManager = new TextToSpeechManager(this);

        // Inizializza componenti UI
        initViews();
        loadSettings();

        // Gestisce il pulsante "Applica Modifiche"
        Button btnApplySettings = findViewById(R.id.btnApplySettings);
        btnApplySettings.setOnClickListener(v -> applySettings());
    }

    /** Inizializza i componenti UI */
    private void initViews() {
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchValue = findViewById(R.id.pitchValue);
        speedValue = findViewById(R.id.speedValue);
        voiceGenderGroup = findViewById(R.id.voiceGenderGroup);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);

        // Listener per aggiornare i valori delle SeekBar in tempo reale
        pitchSeekBar.setOnSeekBarChangeListener(createSeekBarListener("pitch", pitchValue));
        speedSeekBar.setOnSeekBarChangeListener(createSeekBarListener("speed", speedValue));
    }

    /** Carica le impostazioni salvate e aggiorna la UI */
    private void loadSettings() {
        // Carica tema
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);
        ((RadioButton) findViewById(isDarkTheme ? R.id.radioDark : R.id.radioLight)).setChecked(true);

        // Carica genere della voce
        String savedVoice = preferences.getString("voiceGender", "male");
        ((RadioButton) findViewById("male".equals(savedVoice) ? R.id.voiceMale : R.id.voiceFemale)).setChecked(true);

        // Carica tonalità e velocità
        float savedPitch = preferences.getFloat("pitch", 1.0f);
        float savedSpeed = preferences.getFloat("speed", 1.0f);
        pitchSeekBar.setProgress((int) (savedPitch * 50));
        speedSeekBar.setProgress((int) (savedSpeed * 50));
        pitchValue.setText(String.format(Locale.US, "Tonalità: %.1f", savedPitch));
        speedValue.setText(String.format(Locale.US, "Velocità: %.1f", savedSpeed));
    }

    /** Salva le impostazioni e riavvia l'activity */
    private void applySettings() {
        SharedPreferences.Editor editor = preferences.edit();

        // Salva tema
        boolean isDarkTheme = themeRadioGroup.getCheckedRadioButtonId() == R.id.radioDark;
        editor.putBoolean("isDarkTheme", isDarkTheme);

        // Salva voce selezionata
        String selectedVoice = (voiceGenderGroup.getCheckedRadioButtonId() == R.id.voiceMale) ? "male" : "female";
        editor.putString("voiceGender", selectedVoice);

        // Salva tonalità e velocità
        float pitch = pitchSeekBar.getProgress() / 50.0f;
        float speed = speedSeekBar.getProgress() / 50.0f;
        editor.putFloat("pitch", pitch);
        editor.putFloat("speed", speed);
        editor.apply();

        // Applica modifiche al TextToSpeechManager
        ttsManager.saveSettings(pitch, speed, selectedVoice);

        Toast.makeText(this, "Impostazioni aggiornate!", Toast.LENGTH_SHORT).show();

        // Riavvia l'activity per applicare il nuovo tema
        recreate();
    }

    /** Applica il tema salvato */
    private void applySavedTheme() {
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    /** Listener per aggiornare le SeekBar */
    private SeekBar.OnSeekBarChangeListener createSeekBarListener(String key, TextView valueText) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newValue = progress / 50.0f;
                valueText.setText(String.format(Locale.US, "%s: %.1f", key.equals("pitch") ? "Tonalità" : "Velocità", newValue));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }
}
