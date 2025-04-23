package com.phototext.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.util.Objects;

public class OCRManager {
    private final TextRecognizer recognizer;
    private final OCRCallback callback;

    public interface OCRCallback {
        void onOCRComplete(String recognizedText);
        void onOCRError(Exception e);
    }

    public OCRManager(Context context, OCRCallback callback) {
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.callback = callback;
    }

    public void extractText(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            Exception e = new IllegalArgumentException("Bitmap nullo o non valido");
            Log.e("OCR_ERROR", "Bitmap nullo o non valido.", e);
            callback.onOCRError(e);
            return;
        }

        try {
            // Miglioriamo il contrasto per il riconoscimento del testo
            Bitmap processedBitmap = enhanceImage(bitmap);
            InputImage image = InputImage.fromBitmap(processedBitmap, 0);

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String rawText = result.getText();
                        String formattedText = formatText(rawText);
                        Log.d("OCR_SUCCESS", "Testo riconosciuto: " + formattedText);
                        callback.onOCRComplete(formattedText);
                    })
                    .addOnFailureListener(e -> {
                        Log.e("OCR_ERROR", "Errore nel riconoscimento del testo", e);
                        callback.onOCRError(e);
                    });
        } catch (Exception e) {
            Log.e("OCR_ERROR", "Errore durante l'elaborazione dell'immagine", e);
            callback.onOCRError(e);
        }
    }

    /**
     * Migliora il contrasto dell'immagine per un migliore riconoscimento OCR.
     */
    private Bitmap enhanceImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap enhancedBitmap = Bitmap.createBitmap(width, height, Objects.requireNonNull(bitmap.getConfig()));

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixel = bitmap.getPixel(x, y);
                int red = Color.red(pixel);
                int green = Color.green(pixel);
                int blue = Color.blue(pixel);

                // Aumentiamo il contrasto del testo rispetto allo sfondo
                int avg = (red + green + blue) / 3;
                int newPixel = avg > 128 ? Color.WHITE : Color.BLACK;

                enhancedBitmap.setPixel(x, y, newPixel);
            }
        }
        return enhancedBitmap;
    }

    /**
     * Rimuove ritorni a capo e spazi multipli per ottenere un testo più leggibile.
     */
    private String formatText(String text) {
        return text.replace("\n", " ")
                .replaceAll("\\s+", " ") // Rimuove spazi multipli
                .trim();
    }
}