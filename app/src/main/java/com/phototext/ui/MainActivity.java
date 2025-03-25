package com.phototext.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
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
    private TextView textOutput;
    private ImageView imagePreview;
    private CameraManager cameraManager;
    private OCRManager ocrManager;
    private TextToSpeechManager ttsManager;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean isDarkTheme = preferences.getBoolean("isDarkTheme", false);
        AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        setContentView(R.layout.activity_main);

        initViews();
        initManagers();
        setupListeners();
        requestStoragePermission();
    }

    /** Inizializza gli elementi UI */
    private void initViews() {
        imagePreview = findViewById(R.id.imagePreview);
        textOutput = findViewById(R.id.textOutput);
    }

    /** Inizializza i gestori delle funzionalità */
    private void initManagers() {
        cameraManager = new CameraManager(this, imagePreview);
        ocrManager = new OCRManager(this, textOutput);
        ttsManager = new TextToSpeechManager(this);
    }

    /** Imposta i listener per i pulsanti */
    private void setupListeners() {
        findViewById(R.id.btnCapture).setOnClickListener(v -> cameraManager.openCamera());
        findViewById(R.id.btnExtractText).setOnClickListener(v -> ocrManager.extractText(cameraManager.getCapturedImage()));
        findViewById(R.id.btnPlay).setOnClickListener(v -> ttsManager.speak(textOutput.getText().toString()));
        findViewById(R.id.btnPause).setOnClickListener(v -> ttsManager.pause());
        findViewById(R.id.btnStop).setOnClickListener(v -> ttsManager.stop());
        findViewById(R.id.btnPickImage).setOnClickListener(v -> pickImageFromGallery());

        findViewById(R.id.btnSettings).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        });
    }

    /** Metodo per aprire la galleria */
    private void pickImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    /** Activity Result API per la selezione dell'immagine */
    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        imagePreview.setImageURI(selectedImageUri);
                        processSelectedImage(selectedImageUri);
                    }
                }
            }
    );

    /** Converte un Uri in Bitmap e avvia l'estrazione del testo */
    private void processSelectedImage(Uri imageUri) {
        Bitmap bitmap = uriToBitmap(imageUri);
        if (bitmap != null) {
            extractTextFromBitmap(bitmap);
        } else {
            Toast.makeText(this, "Errore nel caricamento dell'immagine", Toast.LENGTH_SHORT).show();
        }
    }

    /** Converte un Uri in Bitmap */
    private Bitmap uriToBitmap(Uri uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // Android 9 (Pie) e successivi
                return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
            } else { // Versioni precedenti
                return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
            }
        } catch (IOException e) {
            Log.e("MainActivity", "Errore nella conversione dell'immagine", e);
            return null;
        }
    }


    /** Estrae il testo da un Bitmap */
    private void extractTextFromBitmap(Bitmap bitmap) {
        ProgressBar progressBar = findViewById(R.id.progressBar);
        runOnUiThread(() -> progressBar.setVisibility(View.VISIBLE)); // Mostra la ProgressBar

        new Thread(() -> {
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> runOnUiThread(() -> {
                        textOutput.setText(visionText.getText());
                        progressBar.setVisibility(View.GONE); // Nasconde la ProgressBar dopo il completamento
                    }))
                    .addOnFailureListener(e -> runOnUiThread(() -> {
                        Log.e("OCR", "Errore nel riconoscimento del testo", e);
                        Toast.makeText(MainActivity.this, "Errore nell'estrazione del testo", Toast.LENGTH_SHORT).show();
                        progressBar.setVisibility(View.GONE); // Nasconde la ProgressBar anche in caso di errore
                    }));
        }).start();
    }


    /** Gestione dei permessi per l'accesso alla galleria */
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (!isGranted) {
                    Toast.makeText(this, "Permesso necessario per selezionare immagini", Toast.LENGTH_SHORT).show();
                }
            });

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {  // Android 13+
            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ttsManager.shutdown();
    }
}
