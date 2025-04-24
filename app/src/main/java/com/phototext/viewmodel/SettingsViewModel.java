package com.phototext.viewmodel;

import static android.content.Context.MODE_PRIVATE;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

public class SettingsViewModel extends AndroidViewModel {
    private static final String TAG = "SettingsViewModel";
    private final SharedPreferences preferences;

    private final MutableLiveData<Boolean> isDarkTheme = new MutableLiveData<>();
    private final MutableLiveData<Float> pitch = new MutableLiveData<>();
    private final MutableLiveData<Float> speed = new MutableLiveData<>();
    private final MutableLiveData<String> voiceGender = new MutableLiveData<>();
    private final MutableLiveData<String> ttsEngine = new MutableLiveData<>();
    private final MutableLiveData<String> serverIp = new MutableLiveData<>();

    public SettingsViewModel(Application application) {
        super(application);
        preferences = application.getSharedPreferences("AppSettings", MODE_PRIVATE);
        loadSettings();
    }

    public void loadSettings() {
        isDarkTheme.setValue(preferences.getBoolean("isDarkTheme", false));
        pitch.setValue(preferences.getFloat("pitch", 1.0f));
        speed.setValue(preferences.getFloat("speed", 1.0f));
        voiceGender.setValue(preferences.getString("voiceGender", "male"));
        ttsEngine.setValue(preferences.getString("ttsEngine", "google"));
        serverIp.setValue(preferences.getString("server_ip", "10.0.2.2"));
    }

    public void saveSettings() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isDarkTheme", isDarkTheme.getValue());
        editor.putFloat("pitch", pitch.getValue());
        editor.putFloat("speed", speed.getValue());
        editor.putString("voiceGender", voiceGender.getValue());
        editor.putString("ttsEngine", ttsEngine.getValue());
        editor.putString("server_ip", serverIp.getValue());
        editor.apply();
        Log.d(TAG, "Settings saved successfully");
    }

    // Getter e Setter per tutte le LiveData
    public MutableLiveData<Boolean> getThemeLiveData() { return isDarkTheme; }
    public void setTheme(boolean isDark) { isDarkTheme.setValue(isDark); }

    public MutableLiveData<Float> getPitchLiveData() { return pitch; }
    public void setPitch(float value) { pitch.setValue(value); }

    public MutableLiveData<Float> getSpeedLiveData() { return speed; }
    public void setSpeed(float value) { speed.setValue(value); }

    public MutableLiveData<String> getVoiceGenderLiveData() { return voiceGender; }
    public void setVoiceGender(String gender) { voiceGender.setValue(gender); }

    public MutableLiveData<String> getTtsEngineLiveData() { return ttsEngine; }
    public void setTtsEngine(String engine) { ttsEngine.setValue(engine); }

    public MutableLiveData<String> getServerIpLiveData() { return serverIp; }
    public void setServerIp(String ip) { serverIp.setValue(ip); }
}