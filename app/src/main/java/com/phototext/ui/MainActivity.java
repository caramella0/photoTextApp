package com.phototext.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.phototext.R;
import com.phototext.camera.CameraManager;
import com.phototext.ocr.OCRManager;
import com.phototext.tts.BaseTTSManager;
import com.phototext.tts.GoogleTTSManager;
import com.phototext.tts.KokoroTTSManager;
import com.phototext.tts.TextToSpeechManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements OCRManager.OCRCallback {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_EDIT_TEXT = 1;
    private static final String AUDIO_DIR = "PhotoText_Audios";
    private ImageView imagePreview;
    private TextView textOutput;
    private ProgressBar progressBar;
    private CameraManager cameraManager;
    public OCRManager ocrManager;
    private TextToSpeechManager ttsManager;
    private BaseTTSManager ttsManagerbase;


    // Executor per operazioni I/O
    private final Executor executor = Executors.newSingleThreadExecutor();

    // Activity launchers
    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> showMessage(isGranted ? "Permesso concesso" : "Permesso negato"));

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri selectedImageUri = data.getData();
                        if (selectedImageUri != null) {
                            processSelectedImage(selectedImageUri);
                        } else {
                            showMessage("Nessuna immagine selezionata");
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> editTextLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String modifiedText = result.getData().getStringExtra(TextEditActivity.RESULT_TEXT);
                    if (modifiedText != null) {
                        textOutput.setText(modifiedText);
                        saveModifiedText(modifiedText);
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(TAG, "Activity created");
        initViews();
        initManagers();

        // Verifica periodicamente lo stato del TTS
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!ttsManager.isReady()) {
                Log.w(TAG, "TTS engine still initializing");
                Toast.makeText(this, "TTS engine initializing...", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "TTS engine is ready");
            }
        }, 1000);

        setupListeners();
        checkPermissions();

    }

    @Override
    protected void onResume() {
        super.onResume();
        initializeTTSManager();
        loadSettings();
        initializeTTSManager();
    }

    private void initializeTTSManager() {
        SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String engine = prefs.getString("ttsEngine", "google");

        if ("kokoro".equals(engine)) {
            ttsManagerbase = new KokoroTTSManager(this);
            Log.d("MainActivity", "Inizializzato KokoroTTSManager");
        } else {
            ttsManagerbase = new GoogleTTSManager(this);
            Log.d("MainActivity", "Inizializzato GoogleTTSManager");
        }
    }

    private void loadSettings() {
        SharedPreferences preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        String serverIp = preferences.getString("server_ip", "10.0.2.2");
        // Qui puoi loggare o usare il valore
        Log.d("MainActivity", "Server IP caricato: " + serverIp);
        // Altri parametri come pitch, speed, voice, engine ecc. possono essere caricati se servono
    }

    private void initViews() {
        imagePreview = findViewById(R.id.imagePreview);
        textOutput = findViewById(R.id.textOutput);
        progressBar = findViewById(R.id.progressBar);
    }

    private void initManagers() {
        cameraManager = new CameraManager(this, imagePreview);
        ocrManager = new OCRManager(this, this);
        ttsManager = new TextToSpeechManager(this);
    }

    private void setupListeners() {
        findViewById(R.id.btnCapture).setOnClickListener(v -> cameraManager.openCamera());
        findViewById(R.id.btnPickImage).setOnClickListener(v -> pickImageFromGallery());
        findViewById(R.id.btnExtractText).setOnClickListener(v -> processCapturedImage());
        findViewById(R.id.btnPlay).setOnClickListener(v -> speakText());
        findViewById(R.id.btnPause).setOnClickListener(v -> ttsManager.pause());
        findViewById(R.id.btnStop).setOnClickListener(v -> ttsManager.stop());
        findViewById(R.id.btnModText).setOnClickListener(v -> openTextEditor());
        findViewById(R.id.btnDownloadAudio).setOnClickListener(v -> saveAudioFile());
        findViewById(R.id.btnAudioLibrary).setOnClickListener(v ->
                startActivity(new Intent(this, AudioLibraryActivity.class)));
        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            String text = textOutput.getText().toString();
            if (!text.isEmpty()) {
                if (ttsManager.isReady()) {
                    ttsManager.speak(text);
                } else {
                    Toast.makeText(this, "Motore vocale non pronto, attendere...", Toast.LENGTH_SHORT).show();
                }
            } else {
                showMessage("Nessun testo da riprodurre");
            }
        });

        findViewById(R.id.btnDownloadAudio).setOnClickListener(v -> {
            String text = textOutput.getText().toString();
            if (!text.isEmpty()) {
                if (ttsManager.isReady()) {
                    String filename = "audio_" + System.currentTimeMillis() + ".wav";
                    ttsManager.saveAudioToFile(text, filename);
                    Toast.makeText(this, "Salvataggio audio iniziato: " + filename, Toast.LENGTH_SHORT).show();

                    // Verifica dopo un ritardo se il file è stato creato
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        File file = new File(getExternalFilesDir(null), filename);
                        if (file.exists()) {
                            Toast.makeText(this, "Audio salvato in: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Errore nel salvataggio dell'audio", Toast.LENGTH_SHORT).show();
                        }
                    }, 2000);
                } else {
                    Toast.makeText(this, "Motore vocale non pronto", Toast.LENGTH_SHORT).show();
                }
            } else {
                showMessage("Nessun testo da convertire");
            }
        });
    }

    private void processCapturedImage() {
        Bitmap image = cameraManager.getCapturedImage();
        if (image != null) {
            recognizeTextFromImage(image);
        } else {
            showMessage("Nessuna immagine da elaborare");
        }
    }

    private void speakText() {
        String text = textOutput.getText().toString();
        if (!text.isEmpty()) {
            ttsManager.speak(text);
        } else {
            showMessage("Nessun testo da riprodurre");
        }
    }

    private void openTextEditor() {
        String currentText = textOutput.getText().toString();
        if (!currentText.isEmpty()) {
            Intent intent = new Intent(this, TextEditActivity.class);
            intent.putExtra(TextEditActivity.EXTRA_TEXT, currentText);
            editTextLauncher.launch(intent);
        } else {
            showMessage("Nessun testo da modificare");
        }
    }

    private void saveAudioFile() {
        String text = textOutput.getText().toString();
        if (!text.isEmpty()) {
            if (ttsManager.isReady()) {
                // Crea una directory dedicata se non esiste
                File audioDir = new File(getExternalFilesDir(null), AUDIO_DIR);
                if (!audioDir.exists()) {
                    if (!audioDir.mkdirs()) {
                        Log.e(TAG, "Failed to create audio directory");
                        showMessage("Failed to create audio directory");
                        return;
                    }
                }

                String filename = "audio_" + System.currentTimeMillis() + ".wav";
                File outputFile = new File(audioDir, filename);

                Log.d(TAG, "Attempting to save audio to: " + outputFile.getAbsolutePath());
                ttsManager.saveAudioToFile(text, outputFile.getAbsolutePath());
                showMessage("Saving audio...");

                // Verifica dopo un ritardo
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (outputFile.exists()) {
                        Log.d(TAG, "Audio file created successfully");
                        showMessage("Audio saved successfully");
                        // Aggiorna la libreria audio
                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                Uri.fromFile(outputFile)));
                    } else {
                        Log.e(TAG, "Audio file not created");
                        showMessage("Failed to save audio");
                    }
                }, 3000);
            } else {
                Log.e(TAG, "TTS engine not ready");
                showMessage("TTS engine not ready");
            }
        } else {
            Log.w(TAG, "No text to convert");
            showMessage("No text to convert");
        }
    }

    @Override
    public void onOCRComplete(@NonNull String recognizedText) {
        runOnUiThread(() -> {
            textOutput.setText(recognizedText);
            showProgress(false);
        });
    }

    @Override
    public void onOCRError(@NonNull Exception e) {
        runOnUiThread(() -> {
            showProgress(false);
            showMessage("Errore nel riconoscimento del testo");
            Log.e(TAG, "OCR Error", e);
        });
    }

    private void recognizeTextFromImage(Bitmap bitmap) {
        showProgress(true);
        executor.execute(() -> {
            try {
                TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
                InputImage image = InputImage.fromBitmap(bitmap, 0);

                recognizer.process(image)
                        .addOnSuccessListener(visionText -> runOnUiThread(() -> {
                            textOutput.setText(visionText.getText());
                            showProgress(false);
                        }))
                        .addOnFailureListener(e -> runOnUiThread(() -> {
                            showProgress(false);
                            showMessage("Errore riconoscimento testo");
                            Log.e(TAG, "OCR Error", e);
                        }));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showMessage("Errore nell'elaborazione");
                    Log.e(TAG, "Image processing error", e);
                });
            }
        });
    }

    private void processSelectedImage(Uri imageUri) {
        showProgress(true);
        executor.execute(() -> {
            try (InputStream inputStream = getContentResolver().openInputStream(imageUri)) {
                if (inputStream == null) throw new IOException("Impossibile aprire lo stream");

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                if (bitmap == null) throw new IOException("Bitmap non decodificato");

                runOnUiThread(() -> {
                    imagePreview.setImageURI(imageUri);
                    recognizeTextFromImage(bitmap);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showMessage("Errore: " + e.getMessage());
                    Log.e(TAG, "Image processing error", e);
                });
            }
        });
    }

    private void pickImageFromGallery() {
        if (hasStoragePermission()) {
            launchImagePicker();
        } else {
            requestStoragePermission();
        }
    }

    private boolean hasStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;
        requestPermissionLauncher.launch(permission);
    }

    private void launchImagePicker() {
        showProgress(true);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    private void saveModifiedText(String text) {
        // Implementa il salvataggio permanente se necessario
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        setButtonsEnabled(!show);
    }

    private void setButtonsEnabled(boolean enabled) {
        findViewById(R.id.btnExtractText).setEnabled(enabled);
        findViewById(R.id.btnPickImage).setEnabled(enabled);
        findViewById(R.id.btnCapture).setEnabled(enabled);
        findViewById(R.id.btnPlay).setEnabled(enabled);
        findViewById(R.id.btnModText).setEnabled(enabled);
        findViewById(R.id.btnDownloadAudio).setEnabled(enabled);
    }

    private void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }

        if (!hasStoragePermission()) {
            requestStoragePermission();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}