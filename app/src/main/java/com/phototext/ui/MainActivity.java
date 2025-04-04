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
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.phototext.R;
import com.phototext.camera.CameraManager;
import com.phototext.ocr.OCRManager;
import com.phototext.tts.TextToSpeechManager;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_EDIT_TEXT = 1;
    private TextView textOutput;
    private ImageView imagePreview;
    private CameraManager cameraManager;
    private OCRManager ocrManager;
    private TextToSpeechManager ttsManager;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;
    private SharedPreferences sharedPreferences;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onResume();

        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        setContentView(R.layout.activity_main);

        applyTheme();
        initPermissionLaunchers();
        initViews();
        initManagers();
        setupListeners();
        checkPermissions();
    }

    private void initPermissionLaunchers() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Toast.makeText(this, "Permesso concesso", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Permesso negato", Toast.LENGTH_SHORT).show();
                    }
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null) {
                            imagePreview.setImageURI(selectedImageUri);
                            processSelectedImage(selectedImageUri);
                        }
                    }
                });
    }

    private void applyTheme() {
        boolean isDarkTheme = sharedPreferences.getBoolean("isDarkTheme", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyTheme();

        if (ttsManager != null) {
            ttsManager.loadSettings();
        }
    }

    private void initViews() {
        imagePreview = findViewById(R.id.imagePreview);
        textOutput = findViewById(R.id.textOutput);
    }

    private void initManagers() {
        cameraManager = new CameraManager(this, imagePreview);
        ocrManager = new OCRManager(this, textOutput);
        ttsManager = new TextToSpeechManager(this);
    }

    private void setupListeners() {
        findViewById(R.id.btnCapture).setOnClickListener(v -> cameraManager.openCamera());
        findViewById(R.id.btnExtractText).setOnClickListener(v -> ocrManager.extractText(cameraManager.getCapturedImage()));
        findViewById(R.id.btnPlay).setOnClickListener(v -> ttsManager.speak(textOutput.getText().toString()));
        findViewById(R.id.btnPause).setOnClickListener(v -> ttsManager.pause());
        findViewById(R.id.btnStop).setOnClickListener(v -> ttsManager.stop());
        findViewById(R.id.btnPickImage).setOnClickListener(v -> pickImageFromGallery());

        findViewById(R.id.btnAudioLibrary).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, AudioLibraryActivity.class));
        });

        findViewById(R.id.btnExtractText).setOnClickListener(v -> {
            Bitmap image = cameraManager.getCapturedImage();
            if (image != null) {
                showProgress(true); // Mostra la barra di caricamento
                extractTextFromBitmap(image);
            } else {
                Toast.makeText(this, "Nessuna immagine da elaborare", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnModText).setOnClickListener(v -> {
            String currentText = textOutput.getText().toString();
            if (!currentText.isEmpty()) {
                Intent intent = new Intent(MainActivity.this, TextEditActivity.class);
                intent.putExtra(TextEditActivity.EXTRA_TEXT, currentText);
                startActivityForResult(intent, REQUEST_EDIT_TEXT);
            } else {
                Toast.makeText(this, "Nessun testo da modificare", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnDownloadAudio).setOnClickListener(v -> {
            String text = textOutput.getText().toString();
            if (!text.isEmpty()) {
                String filename = "audio_" + System.currentTimeMillis() + ".wav";
                ttsManager.saveAudioToFile(text, filename);
                Toast.makeText(this, "Audio salvato come " + filename, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Nessun testo da convertire", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, SettingsActivity.class));
        });
    }
    // Aggiungi questo metodo per gestire la visibilità della progress bar
    private void showProgress(boolean show) {
        runOnUiThread(() -> {
            ProgressBar progressBar = findViewById(R.id.progressBar);
            if (progressBar != null) {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }

            // Disabilita i pulsanti durante l'elaborazione
            setButtonsEnabled(!show);
        });
    }
    private void checkPermissions() {
        // Controlla permessi per l'audio
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }

        // Controlla permessi per lo storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO);
            }
        } else {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }
    }

    private void pickImageFromGallery() {
        showProgress(true); // Mostra subito la barra
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_EDIT_TEXT && resultCode == RESULT_OK) {
            String modifiedText = data.getStringExtra(TextEditActivity.RESULT_TEXT);
            textOutput.setText(modifiedText);

            // Opzionale: salva automaticamente il testo modificato
            saveModifiedText(modifiedText);
        }
    }

    private void saveModifiedText(String text) {
        // Implementa il salvataggio permanente se necessario
    }
    private void processSelectedImage(Uri imageUri) {
        new Thread(() -> {
            try {
                Bitmap bitmap = uriToBitmap(imageUri);
                runOnUiThread(() -> {
                    imagePreview.setImageURI(imageUri);
                    extractTextFromBitmap(bitmap);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    Toast.makeText(this, "Errore nel caricamento", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private Bitmap uriToBitmap(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
            } else {
                return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Errore nella conversione dell'immagine", e);
            return null;
        }
    }

    private void extractTextFromBitmap(Bitmap bitmap) {
        showProgress(true); // Mostra la barra

        new Thread(() -> {
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> runOnUiThread(() -> {
                        textOutput.setText(visionText.getText());
                        showProgress(false);
                    }))
                    .addOnFailureListener(e -> runOnUiThread(() -> {
                        showProgress(false);
                        Toast.makeText(this, "Estrazione fallita", Toast.LENGTH_SHORT).show();
                    }));
        }).start();
    }

    private void setButtonsEnabled(boolean enabled) {
        findViewById(R.id.btnExtractText).setEnabled(enabled);
        findViewById(R.id.btnPickImage).setEnabled(enabled);
        findViewById(R.id.btnCapture).setEnabled(enabled);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
    }
}