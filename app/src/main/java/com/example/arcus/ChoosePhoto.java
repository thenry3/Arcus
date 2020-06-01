package com.example.arcus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

public class ChoosePhoto extends AppCompatActivity {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int GALLERY_REQUEST_CODE = 2;
    private String toLang = "Afrikaans";
    private String fromLang = "Afrikaans";
    private Uri imgUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_photo);

        Spinner fSpinner = findViewById(R.id.from_lang_spinner);
        Spinner tSpinner = findViewById(R.id.to_lang_spinner);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> Adapter = ArrayAdapter.createFromResource(this, R.array.lang_array, R.layout.spinner_text);
        // Specify the layout to use when the list of choices appears
        Adapter.setDropDownViewResource(R.layout.spinner_dropdown);
        // Apply the adapter to the spinner
        fSpinner.setAdapter(Adapter);
        tSpinner.setAdapter(Adapter);

        AdapterView.OnItemSelectedListener langListener = new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String language = (String) parent.getItemAtPosition(position);
                if (id == R.id.from_lang_spinner)
                    fromLang = language;
                else if (id == R.id.to_lang_spinner)
                    toLang = language;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        };

        fSpinner.setOnItemSelectedListener(langListener);
        tSpinner.setOnItemSelectedListener(langListener);
    }

    public void onCamClick(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.TITLE, "New Picture");
            values.put(MediaStore.Images.Media.DESCRIPTION, "From your Camera");
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            imgUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int imgMode = 1;
        if (resultCode != RESULT_OK)
            return;
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK) {
            imgUri = data.getData();
            imgMode = 0;
        }

        if (imgUri != null) {

            Intent intent = new Intent(this, Review.class);
            intent.putExtra("toLang", toLang);
            intent.putExtra("fromLang", fromLang);
            intent.putExtra("imageMode", imgMode);
            intent.putExtra("image", imgUri);

            startActivity(intent);
        }
    }

    public void onGalleryClick(View v) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1052);
        } else {
            Intent pickPictureIntent = new Intent(Intent.ACTION_PICK);
            pickPictureIntent.setType("image/*");
            String[] mimeTypes = {"image/jpeg", "image/png"};
            pickPictureIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
            // Launching the Intent
            startActivityForResult(pickPictureIntent, GALLERY_REQUEST_CODE);
        }
    }
}
