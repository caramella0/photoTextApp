package com.phototext.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.widget.TextView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class OCRManager {
    private final TextRecognizer recognizer;
    private final TextView textView;

    public OCRManager(Context context, TextView textView) {
        this.recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
        this.textView = textView;
    }

    public void extractText(Bitmap bitmap) {
        if (bitmap == null || bitmap.getWidth() == 0 || bitmap.getHeight() == 0) {
            textView.setText("Errore: Nessuna immagine valida.");
            Log.e("OCR_ERROR", "Bitmap nullo o non valido.");
            return;
        }

        // Miglioriamo il contrasto per il riconoscimento del testo
        Bitmap processedBitmap = enhanceImage(bitmap);

        InputImage image = InputImage.fromBitmap(processedBitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(result -> {
                    String rawText = result.getText();
                    String formattedText = formatText(rawText);
                    textView.setText(formattedText);
                    Log.d("OCR_SUCCESS", "Testo riconosciuto: " + formattedText);
                })
                .addOnFailureListener(e -> {
                    Log.e("OCR_ERROR", "Errore nel riconoscimento del testo", e);
                    textView.setText("Errore nell'estrazione del testo.");
                });
    }

    /**
     * Migliora il contrasto dell'immagine per un migliore riconoscimento OCR.
     */
    private Bitmap enhanceImage(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap enhancedBitmap = Bitmap.createBitmap(width, height, bitmap.getConfig());

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
