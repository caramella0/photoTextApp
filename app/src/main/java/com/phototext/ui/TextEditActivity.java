package com.phototext.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.phototext.R;

public class TextEditActivity extends AppCompatActivity {

    public static final String EXTRA_TEXT = "original_text";
    public static final String RESULT_TEXT = "modified_text";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text_edit);

        EditText editText = findViewById(R.id.editText);
        String originalText = getIntent().getStringExtra(EXTRA_TEXT);
        editText.setText(originalText);

        findViewById(R.id.btnSave).setOnClickListener(v -> {
            String modifiedText = editText.getText().toString();
            Intent resultIntent = new Intent();
            resultIntent.putExtra(RESULT_TEXT, modifiedText);
            setResult(RESULT_OK, resultIntent);
            finish();
        });
    }
}