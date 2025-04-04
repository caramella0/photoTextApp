package com.phototext.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
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
    private boolean isTtsReady = false;
    private static final String PREFS_NAME = "AppSettings";
    private static final String VOICE_PREFS = "VoiceSettings";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Applica il tema prima di impostare il layout
        preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        applySavedTheme();

        setContentView(R.layout.activity_settings);

        // Inizializza i componenti UI
        initViews();

        // Inizializza TTS Manager
        ttsManager = new TextToSpeechManager(this);

        // Configura listener per cambio genere voce
        voiceGenderGroup.setOnCheckedChangeListener((group, checkedId) -> {
            String gender = (checkedId == R.id.voiceMale) ? "male" : "female";
            ttsManager.saveSettings(pitchSeekBar.getProgress() / 50.0f,
                    speedSeekBar.getProgress() / 50.0f,
                    gender);
            ttsManager.speak("Questa è una prova vocale");
        });

        // Carica le impostazioni salvate
        loadSettings();

        // Configura pulsante applica modifiche
        Button btnApplySettings = findViewById(R.id.btnApplySettings);
        btnApplySettings.setOnClickListener(v -> applySettings());

        // Delay per inizializzazione TTS
        new Handler().postDelayed(() -> {
            isTtsReady = true;
        }, 1000);
    }

    /** Inizializza i componenti UI */
    private void initViews() {
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchValue = findViewById(R.id.pitchValue);
        speedValue = findViewById(R.id.speedValue);
        voiceGenderGroup = findViewById(R.id.voiceGenderGroup);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);

        pitchSeekBar.setOnSeekBarChangeListener(createSeekBarListener("pitch", pitchValue));
        speedSeekBar.setOnSeekBarChangeListener(createSeekBarListener("speed", speedValue));

    }



    /** Carica le impostazioni salvate */
    private void loadSettings() {
        // Carica tema
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);
        themeRadioGroup.check(isDarkTheme ? R.id.radioDark : R.id.radioLight);

        // Carica genere della voce
        String savedVoice = preferences.getString("voiceGender", "female");
        voiceGenderGroup.check("male".equals(savedVoice) ? R.id.voiceMale : R.id.voiceFemale);

        // Carica tonalità e velocità
        float savedPitch = preferences.getFloat("pitch", 1.0f);
        float savedSpeed = preferences.getFloat("speed", 1.0f);
        pitchSeekBar.setProgress((int) (savedPitch * 50));
        speedSeekBar.setProgress((int) (savedSpeed * 50));
        pitchValue.setText(String.format(Locale.US, "Tonalità: %.1f", savedPitch));
        speedValue.setText(String.format(Locale.US, "Velocità: %.1f", savedSpeed));

    }

    /** Applica le nuove impostazioni */
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

        // Applica impostazioni al TTS
        if (ttsManager != null) {
            ttsManager.saveSettings(pitch, speed, selectedVoice);
            ttsManager.speak("Test vocale");
        }

        Toast.makeText(this, "Impostazioni aggiornate!", Toast.LENGTH_SHORT).show();
    }

    /** Applica il tema salvato */
    private void applySavedTheme() {
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarListener(String key, TextView valueText) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newValue = progress / 50.0f;
                valueText.setText(String.format(Locale.US, "%s: %.1f",
                        key.equals("pitch") ? "Tonalità" : "Velocità", newValue));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }

}
