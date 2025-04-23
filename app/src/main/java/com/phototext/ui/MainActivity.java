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
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
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
import java.io.InputStream;

public class MainActivity extends AppCompatActivity implements OCRManager.OCRCallback {
    private static final int REQUEST_EDIT_TEXT = 1;
    private static final String TAG = "MainActivity";

    // Views
    private ImageView imagePreview;
    private TextView textOutput;
    private ProgressBar progressBar;

    // Managers
    private CameraManager cameraManager;
    private OCRManager ocrManager;
    private TextToSpeechManager ttsManager;
    private static final int REQUEST_READ_STORAGE = 101;

    // Activity launchers
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> pickImageLauncher;

    // Shared preferences
    private SharedPreferences sharedPreferences;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSharedPreferences();
        initViews();
        initManagers();
        initActivityLaunchers();
        setupListeners();
        checkPermissions();
    }

    private void initSharedPreferences() {
        sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE);
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

    private void initActivityLaunchers() {
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> showMessage(isGranted ? "Permesso concesso" : "Permesso negato"));

        pickImageLauncher = registerForActivityResult(
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
    }

    private void setupListeners() {
        findViewById(R.id.btnCapture).setOnClickListener(v -> cameraManager.openCamera());

        findViewById(R.id.btnPickImage).setOnClickListener(v -> pickImageFromGallery());

        findViewById(R.id.btnExtractText).setOnClickListener(v -> {
            Bitmap image = cameraManager.getCapturedImage();
            if (image != null) {
                recognizeTextFromImage(image);
            } else {
                showMessage("Nessuna immagine da elaborare");
            }
        });

        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            String text = textOutput.getText().toString();
            if (!text.isEmpty()) {
                ttsManager.speak(text);
            } else {
                showMessage("Nessun testo da riprodurre");
            }
        });

        findViewById(R.id.btnPause).setOnClickListener(v -> ttsManager.pause());
        findViewById(R.id.btnStop).setOnClickListener(v -> ttsManager.stop());

        findViewById(R.id.btnModText).setOnClickListener(v -> {
            String currentText = textOutput.getText().toString();
            if (!currentText.isEmpty()) {
                openTextEditor(currentText);
            } else {
                showMessage("Nessun testo da modificare");
            }
        });

        findViewById(R.id.btnDownloadAudio).setOnClickListener(v -> {
            String text = textOutput.getText().toString();
            if (!text.isEmpty()) {
                saveAudioFile(text);
            } else {
                showMessage("Nessun testo da convertire");
            }
        });

        findViewById(R.id.btnAudioLibrary).setOnClickListener(v ->
                startActivity(new Intent(this, AudioLibraryActivity.class)));

        findViewById(R.id.btnSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }

    // Implementazione OCRCallback
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

        // Pulisci eventuali risorse non utilizzate
        System.gc();

        try {
            // Crea una nuova istanza del recognizer per ogni elaborazione
            TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        String resultText = visionText.getText();
                        textOutput.setText(resultText);
                        showProgress(false);
                        recognizer.close(); // Chiudi esplicitamente il recognizer
                    })
                    .addOnFailureListener(e -> {
                        showProgress(false);
                        showMessage("Errore riconoscimento testo");
                        Log.e(TAG, "OCR Error", e);
                        recognizer.close(); // Chiudi esplicitamente il recognizer
                    });
        } catch (Exception e) {
            showProgress(false);
            showMessage("Errore nell'elaborazione");
            Log.e(TAG, "Image processing error", e);
        }
    }

    private void processSelectedImage(Uri imageUri) {
        showProgress(true);
        new Thread(() -> {
            try {
                // Aggiungi questo controllo iniziale
                if (imageUri == null) {
                    throw new IOException("URI immagine nullo");
                }

                // Usa ContentResolver per ottenere un input stream stabile
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    throw new IOException("Impossibile aprire lo stream dell'immagine");
                }

                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();

                if (bitmap == null) {
                    throw new IOException("Bitmap non decodificato correttamente");
                }

                // Forza un GC prima dell'elaborazione
                System.gc();

                runOnUiThread(() -> {
                    imagePreview.setImageURI(imageUri);
                    recognizeTextFromImage(bitmap);
                });

            } catch (Exception e) {
                Log.e(TAG, "Errore elaborazione immagine", e);
                runOnUiThread(() -> {
                    showProgress(false);
                    showMessage("Errore: " + e.getMessage());
                });
            }
        }).start();
    }

    private Bitmap uriToBitmap(Uri uri) throws IOException {
        if (uri == null) return null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), uri));
        } else {
            return BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
        }
    }

    private void pickImageFromGallery() {
        if (ContextCompat.checkSelfPermission(this,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                        Manifest.permission.READ_MEDIA_IMAGES :
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                                    Manifest.permission.READ_MEDIA_IMAGES :
                                    Manifest.permission.READ_EXTERNAL_STORAGE
                    },
                    REQUEST_READ_STORAGE);
            return;
        }
        launchImagePicker();
    }

    private void launchImagePicker() {
        showProgress(true);
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        pickImageLauncher.launch(intent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_READ_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchImagePicker();
            } else {
                showMessage("Permesso necessario per accedere alle immagini");
            }
        }
    }

    private void openTextEditor(String text) {
        Intent intent = new Intent(this, TextEditActivity.class);
        intent.putExtra(TextEditActivity.EXTRA_TEXT, text);
        startActivityForResult(intent, REQUEST_EDIT_TEXT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == REQUEST_EDIT_TEXT && data != null) {
            String modifiedText = data.getStringExtra(TextEditActivity.RESULT_TEXT);
            if (modifiedText != null) {
                textOutput.setText(modifiedText);
                saveModifiedText(modifiedText);
            }
        }
    }

    private void saveModifiedText(String text) {
        // Implementa il salvataggio permanente se necessario
    }

    private void saveAudioFile(String text) {
        String filename = "audio_" + System.currentTimeMillis() + ".wav";
        ttsManager.saveAudioToFile(text, filename);
        showMessage("Audio salvato come " + filename);
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

        String storagePermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ?
                Manifest.permission.READ_MEDIA_IMAGES :
                Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, storagePermission)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(storagePermission);
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