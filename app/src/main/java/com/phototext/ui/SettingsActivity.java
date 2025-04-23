package com.phototext.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.lifecycle.ViewModelProvider;

import com.phototext.R;
import com.phototext.viewmodel.SettingsViewModel;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SettingsActivity extends AppCompatActivity {
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String DEFAULT_SERVER_IP = "10.0.2.2";

    private SettingsViewModel viewModel;
    private SharedPreferences preferences;
    private SeekBar pitchSeekBar, speedSeekBar;
    private TextView pitchValue, speedValue;
    private RadioGroup voiceGenderGroup, themeRadioGroup, ttsEngineGroup;
    private EditText serverIpEditText;

    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        viewModel = new ViewModelProvider(this).get(SettingsViewModel.class);
        preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);

        initViews();
        loadSettings();
        setupObservers();
    }

    private void initViews() {
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchValue = findViewById(R.id.pitchValue);
        speedValue = findViewById(R.id.speedValue);
        voiceGenderGroup = findViewById(R.id.voiceGenderGroup);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        ttsEngineGroup = findViewById(R.id.ttsEngineGroup);
        serverIpEditText = findViewById(R.id.serverIpEditText);
        Button testConnectionButton = findViewById(R.id.testConnectionButton);

        pitchSeekBar.setOnSeekBarChangeListener(createSeekBarListener("pitch", pitchValue));
        speedSeekBar.setOnSeekBarChangeListener(createSeekBarListener("speed", speedValue));

        testConnectionButton.setOnClickListener(v -> testServerConnection());

        findViewById(R.id.btnApplySettings).setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(this, "Impostazioni aggiornate!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void setupObservers() {
        viewModel.getThemeLiveData().observe(this, isDark -> {
            AppCompatDelegate.setDefaultNightMode(
                    isDark ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });
    }

    private void loadSettings() {
        // Tonalità e velocità
        float savedPitch = preferences.getFloat("pitch", 1.0f);
        float savedSpeed = preferences.getFloat("speed", 1.0f);
        pitchSeekBar.setProgress((int) (savedPitch * 50));
        speedSeekBar.setProgress((int) (savedSpeed * 50));
        pitchValue.setText(String.format(Locale.US, "Tonalità: %.1f", savedPitch));
        speedValue.setText(String.format(Locale.US, "Velocità: %.1f", savedSpeed));

        // Tema
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);
        ((RadioButton) findViewById(isDarkTheme ? R.id.radioDark : R.id.radioLight)).setChecked(true);

        // Voce
        String voice = preferences.getString("voiceGender", "male");
        ((RadioButton) findViewById("male".equals(voice) ? R.id.voiceMale : R.id.voiceFemale)).setChecked(true);

        // TTS engine
        String engine = preferences.getString("ttsEngine", "google");
        ((RadioButton) findViewById("google".equals(engine) ? R.id.ttsGoogle : R.id.ttsKokoro)).setChecked(true);

        // Server IP
        String serverIp = preferences.getString(KEY_SERVER_IP, DEFAULT_SERVER_IP);
        serverIpEditText.setText(serverIp);
    }

    private void saveSettings() {
        viewModel.setTheme(themeRadioGroup.getCheckedRadioButtonId() == R.id.radioDark);
        viewModel.setPitch(pitchSeekBar.getProgress() / 50.0f);
        viewModel.setSpeed(speedSeekBar.getProgress() / 50.0f);
        viewModel.setVoiceGender(
                (voiceGenderGroup.getCheckedRadioButtonId() == R.id.voiceMale) ? "male" : "female");
        viewModel.setTtsEngine(
                (ttsEngineGroup.getCheckedRadioButtonId() == R.id.ttsGoogle) ? "google" : "kokoro");
        viewModel.setServerIp(serverIpEditText.getText().toString());

        viewModel.saveSettings();
        finish();
    }

    private void testServerConnection() {
        String ip = serverIpEditText.getText().toString();
        executor.execute(() -> {
            try {
                URL url = new URL("http://" + ip + ":5000/ping");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.connect();
                int responseCode = connection.getResponseCode();
                connection.disconnect();

                runOnUiThread(() -> {
                    String message = responseCode == 200 ?
                            "Connessione al server riuscita!" :
                            "Connessione fallita. Verifica IP e che il server sia attivo";
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, "Errore di connessione: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e("ConnectionTest", "Error testing connection", e);
                });
            }
        });
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarListener(String key, TextView valueText) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 50.0f;
                valueText.setText(String.format(Locale.US, "%s: %.1f",
                        key.equals("pitch") ? "Tonalità" : "Velocità", value));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }
}