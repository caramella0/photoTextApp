package com.phototext.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CameraManager {
    private final AppCompatActivity activity;
    private final ImageView imagePreview;
    private Bitmap capturedImage;
    private String currentPhotoPath; // Percorso assoluto del file
    private final ActivityResultLauncher<Intent> cameraLauncher;
    private final ActivityResultLauncher<String> requestPermissionLauncher;

    public CameraManager(AppCompatActivity activity, ImageView imagePreview) {
        this.activity = activity;
        this.imagePreview = imagePreview;

        // Registrazione del launcher per la fotocamera
        cameraLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Log.d("CameraManager", "Foto acquisita. Percorso: " + currentPhotoPath);
                        loadCapturedImage();
                    } else {
                        Log.e("CameraManager", "Errore: La foto non è stata acquisita.");
                    }
                }
        );

        // Gestione richiesta permessi
        requestPermissionLauncher = activity.registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        openCamera();
                    } else {
                        Toast.makeText(activity, "Permesso fotocamera negato!", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void openCamera() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        } else {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
                try {
                    File photoFile = createImageFile();
                    Uri photoUri = FileProvider.getUriForFile(activity,
                            activity.getApplicationContext().getPackageName() + ".provider", photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                    cameraLauncher.launch(takePictureIntent);
                } catch (IOException e) {
                    Toast.makeText(activity, "Errore nella creazione del file", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(activity, "Nessuna app fotocamera disponibile", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("IMG_" + timeStamp, ".jpg", storageDir);

        // Salviamo il percorso per caricare l'immagine dopo
        currentPhotoPath = image.getAbsolutePath();
        Log.d("CameraManager", "File immagine creato: " + currentPhotoPath);

        return image;
    }

    private void loadCapturedImage() {
        File imgFile = new File(currentPhotoPath);
        if (imgFile.exists()) {
            capturedImage = BitmapFactory.decodeFile(currentPhotoPath);
            imagePreview.setImageBitmap(capturedImage);
            Log.d("CameraManager", "Immagine caricata con successo.");
        } else {
            Log.e("CameraManager", "Errore: Il file immagine non esiste. Attendere qualche secondo e riprovare.");
            Toast.makeText(activity, "Caricamento immagine in corso, riprova tra poco.", Toast.LENGTH_SHORT).show();
        }
    }

    public Bitmap getCapturedImage() {
        return capturedImage;
    }
}
