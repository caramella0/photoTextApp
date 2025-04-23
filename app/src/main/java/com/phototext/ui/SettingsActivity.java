package com.phototext.ui;

import android.content.SharedPreferences;
import android.os.AsyncTask;
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

import com.phototext.R;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String DEFAULT_SERVER_IP = "10.0.2.2";
    private SharedPreferences preferences;
    private SeekBar pitchSeekBar, speedSeekBar;
    private TextView pitchValue, speedValue;
    private RadioGroup voiceGenderGroup, themeRadioGroup, ttsEngineGroup;
    private EditText serverIpEditText;
    private Button testConnectionButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        applySavedTheme();
        setContentView(R.layout.activity_settings);

        initViews();
        loadSettings();

        Button btnApplySettings = findViewById(R.id.btnApplySettings);
        btnApplySettings.setOnClickListener(v -> {
            saveSettings();
            Toast.makeText(this, "Impostazioni aggiornate!", Toast.LENGTH_SHORT).show();
            finish();  // Torna alla MainActivity
        });
    }

    private void applySavedTheme() {
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void initViews() {
        pitchSeekBar = findViewById(R.id.pitchSeekBar);
        speedSeekBar = findViewById(R.id.speedSeekBar);
        pitchValue = findViewById(R.id.pitchValue);
        speedValue = findViewById(R.id.speedValue);
        voiceGenderGroup = findViewById(R.id.voiceGenderGroup);
        themeRadioGroup = findViewById(R.id.themeRadioGroup);
        ttsEngineGroup = findViewById(R.id.ttsEngineGroup);

        pitchSeekBar.setOnSeekBarChangeListener(createSeekBarListener("pitch", pitchValue));
        speedSeekBar.setOnSeekBarChangeListener(createSeekBarListener("speed", speedValue));
        serverIpEditText = findViewById(R.id.serverIpEditText);
        testConnectionButton = findViewById(R.id.testConnectionButton);

        testConnectionButton.setOnClickListener(v -> testServerConnection());
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
        SharedPreferences.Editor editor = preferences.edit();

        float pitch = pitchSeekBar.getProgress() / 50.0f;
        float speed = speedSeekBar.getProgress() / 50.0f;
        boolean isDarkTheme = themeRadioGroup.getCheckedRadioButtonId() == R.id.radioDark;
        String voice = (voiceGenderGroup.getCheckedRadioButtonId() == R.id.voiceMale) ? "male" : "female";
        String ttsEngine = (ttsEngineGroup.getCheckedRadioButtonId() == R.id.ttsGoogle) ? "google" : "kokoro";

        editor.putFloat("pitch", pitch);
        editor.putFloat("speed", speed);
        editor.putBoolean("isDarkTheme", isDarkTheme);
        editor.putString("voiceGender", voice);
        editor.putString("ttsEngine", ttsEngine);
        editor.apply();
        editor.putString(KEY_SERVER_IP, serverIpEditText.getText().toString());
        editor.apply();
        editor.putString("voiceGender",
                (voiceGenderGroup.getCheckedRadioButtonId() == R.id.voiceMale) ? "male" : "female");
        editor.apply();
    }

    private void testServerConnection() {
        String ip = serverIpEditText.getText().toString();
        new ServerConnectionTestTask().execute(ip);
    }

    private class ServerConnectionTestTask extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... ips) {
            try {
                URL url = new URL("http://" + ips[0] + ":5000/");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(3000);
                connection.connect();
                int responseCode = connection.getResponseCode();
                connection.disconnect();
                return responseCode == 200;
            } catch (Exception e) {
                Log.e("ConnectionTest", "Error testing connection", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean isSuccessful) {
            String message = isSuccessful ?
                    "Connessione al server riuscita!" :
                    "Connessione fallita. Verifica IP e che il server sia attivo";
            Toast.makeText(SettingsActivity.this, message, Toast.LENGTH_LONG).show();
        }
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarListener(String key, TextView valueText) {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 50.0f;
                valueText.setText(String.format(Locale.US, "%s: %.1f", key.equals("pitch") ? "Tonalità" : "Velocità", value));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        };
    }
}
