package com.phototext.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

public class SettingsViewModel extends AndroidViewModel {
    private final MutableLiveData<Boolean> isDarkTheme = new MutableLiveData<>();
    private final MutableLiveData<Float> pitch = new MutableLiveData<>();
    private final MutableLiveData<Float> speed = new MutableLiveData<>();
    private final MutableLiveData<String> voiceGender = new MutableLiveData<>();
    private final MutableLiveData<String> ttsEngine = new MutableLiveData<>();
    private final MutableLiveData<String> serverIp = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        loadSettings();
    }

    public LiveData<Boolean> getThemeLiveData() {
        return isDarkTheme;
    }

    public LiveData<Float> getPitchLiveData() {
        return pitch;
    }

    public LiveData<Float> getSpeedLiveData() {
        return speed;
    }

    public LiveData<String> getVoiceGenderLiveData() {
        return voiceGender;
    }

    public LiveData<String> getTtsEngineLiveData() {
        return ttsEngine;
    }

    public LiveData<String> getServerIpLiveData() {
        return serverIp;
    }

    public void setTheme(boolean isDark) {
        isDarkTheme.setValue(isDark);
    }

    public void setPitch(float pitchValue) {
        pitch.setValue(pitchValue);
    }

    public void setSpeed(float speedValue) {
        speed.setValue(speedValue);
    }

    public void setVoiceGender(String gender) {
        voiceGender.setValue(gender);
    }

    public void setTtsEngine(String engine) {
        ttsEngine.setValue(engine);
    }

    public void setServerIp(String ip) {
        serverIp.setValue(ip);
    }

    private void loadSettings() {
        // Carica le impostazioni dalle SharedPreferences
        var prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());

        isDarkTheme.setValue(prefs.getBoolean("isDarkTheme", false));
        pitch.setValue(prefs.getFloat("pitch", 1.0f));
        speed.setValue(prefs.getFloat("speed", 1.0f));
        voiceGender.setValue(prefs.getString("voiceGender", "male"));
        ttsEngine.setValue(prefs.getString("ttsEngine", "google"));
        serverIp.setValue(prefs.getString("server_ip", "10.0.2.2"));
    }

    public void saveSettings() {
        var prefs = PreferenceManager.getDefaultSharedPreferences(getApplication());
        var editor = prefs.edit();

        if (isDarkTheme.getValue() != null) {
            editor.putBoolean("isDarkTheme", isDarkTheme.getValue());
        }
        if (pitch.getValue() != null) {
            editor.putFloat("pitch", pitch.getValue());
        }
        if (speed.getValue() != null) {
            editor.putFloat("speed", speed.getValue());
        }
        if (voiceGender.getValue() != null) {
            editor.putString("voiceGender", voiceGender.getValue());
        }
        if (ttsEngine.getValue() != null) {
            editor.putString("ttsEngine", ttsEngine.getValue());
        }
        if (serverIp.getValue() != null) {
            editor.putString("server_ip", serverIp.getValue());
        }

        editor.apply();
    }
}