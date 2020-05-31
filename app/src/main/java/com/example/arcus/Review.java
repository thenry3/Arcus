package com.example.arcus;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.AnnotateImageResponse;
import com.google.cloud.vision.v1.BatchAnnotateImagesRequest;
import com.google.cloud.vision.v1.BatchAnnotateImagesResponse;
import com.google.cloud.vision.v1.EntityAnnotation;
import com.google.cloud.vision.v1.Feature;
import com.google.cloud.vision.v1.Feature.Type;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.protobuf.ByteString;

public class Review extends AppCompatActivity {
    private String toLang;
    private String fromLang;
    private int imgMode;
    private String toLangCode;
    private String fromLangCode;
    Bitmap imgBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            toLang = (String) extras.get("toLang");
            fromLang = (String) extras.get("fromLang");
            imgMode = (int) extras.get("imageMode");
            if (imgMode == 1)
                imgBitmap = (Bitmap) extras.get("image");
            else {
                try {
                    imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), (Uri) extras.get("image"));
                } catch (Exception e) {

                }
            }
        }

        toLangCode = getLangCode(toLang);
        fromLangCode = getLangCode(fromLang);
        ImageView imgView = findViewById(R.id.chosenImg);
        imgView.setImageBitmap(imgBitmap);
        callCloudVision(imgBitmap);
    }

    @SuppressLint("StaticFieldLeak")
    public void callCloudVision(final Bitmap bitmap){

        new AsyncTask<Object, Void, BatchAnnotateImagesResponse>(){
            @Override
            protected BatchAnnotateImagesResponse doInBackground(Object... params){
                List<AnnotateImageRequest> requests = new ArrayList<>();
                List<Feature> featureList = new ArrayList<>();

                Image img = getBase64EncodedJpeg(bitmap);

                Feature label = Feature.newBuilder().setType(Type.LABEL_DETECTION).build();
                Feature obj = Feature.newBuilder().setType(Type.OBJECT_LOCALIZATION).build();
                Feature text = Feature.newBuilder().setType(Type.TEXT_DETECTION).build();
                featureList.add(label);
                featureList.add(obj);
                featureList.add(text);

                AnnotateImageRequest request =
                        AnnotateImageRequest.newBuilder().addAllFeatures(featureList).setImage(img).build();
                requests.add(request);

                try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
                    BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
                    return response;
                } catch (Exception e) {
                    return null;
                }
            }
            protected void onPostExecute(BatchAnnotateImagesResponse response) {
//                labelResults.setText(getDetectedLabels(response));
            }
        }.execute();
    }

    public Image getBase64EncodedJpeg(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        ByteString byteString = ByteString.copyFrom(imageBytes);
        return Image.newBuilder().setContent(byteString).build();
    }

    private String getLangCode(String language) {
        String ans = "af";
        switch(language) {
            case "Afrikaans":
                ans = "af";
                break;
            case "Albanian":
                ans = "sq";
                break;
            case "Aranic":
                ans = "ar";
                break;
            case "Azerbaijani":
                ans = "az";
                break;
            case "Basque":
                ans = "eu";
                break;
            case "Bengali":
                ans = "bn";
                break;
            case "Belarusian":
                ans = "be";
                break;
            case "Bulgarian":
                ans = "bg";
                break;
            case "Catalan":
                ans = "ca";
                break;
            case "Chinese Simplified":
                ans = "zh-CN";
                break;
            case "Chinese Traditional":
                ans = "zh-TW";
                break;
            case "Croatian":
                ans = "hr";
                break;
            case "Czech":
                ans = "cs";
                break;
            case "Danish":
                ans = "da";
                break;
            case "Dutch":
                ans = "nl";
                break;
            case "English":
                ans = "en";
                break;
            case "Esperanto":
                ans = "eo";
                break;
            case "Estonian":
                ans = "et";
                break;
            case "Filipino":
                ans = "tl";
                break;
            case "Finnish":
                ans = "fi";
                break;
            case "French":
                ans = "fr";
                break;
            case "Galician":
                ans = "gl";
                break;
            case "Georgian":
                ans = "ka";
                break;
            case "German":
                ans = "de";
                break;
            case "Greek":
                ans = "el";
                break;
            case "Gujarati":
                ans = "gu";
                break;
            case "Haitian Creole":
                ans = "ht";
                break;
            case "Hebrew":
                ans = "iw";
                break;
            case "Hindi":
                ans = "hi";
                break;
            case "Hungarian":
                ans = "hu";
                break;
            case "Icelandic":
                ans = "is";
                break;
            case "Indonesian":
                ans = "id";
                break;
            case "Irish":
                ans = "ga";
                break;
            case "Italian":
                ans = "it";
                break;
            case "Japanese":
                ans = "ja";
                break;
            case "Kannada":
                ans = "kn";
                break;
            case "Korean":
                ans = "ko";
                break;
            case "Latin":
                ans = "la";
                break;
            case "Latvian":
                ans = "lv";
                break;
            case "Lithuanian":
                ans = "lt";
                break;
            case "Macedonian":
                ans = "mk";
                break;
            case "Malay":
                ans = "ms";
                break;
            case "Maltese":
                ans = "mt";
                break;
            case "Norwegian":
                ans = "no";
                break;
            case "Persian":
                ans = "fa";
                break;
            case "Polish":
                ans = "pl";
                break;
            case "Portuguese":
                ans = "pt";
                break;
            case "Romanian":
                ans = "ro";
                break;
            case "Russian":
                ans = "ru";
                break;
            case "Serbian":
                ans = "sr";
                break;
            case "Slovak":
                ans = "sk";
                break;
            case "Slovenian":
                ans = "sl";
                break;
            case "Spanish":
                ans = "es";
                break;
            case "Swahili":
                ans = "sw";
                break;
            case "Swedish":
                ans = "sv";
                break;
            case "Tamil":
                ans = "ta";
                break;
            case "Telugu":
                ans = "te";
                break;
            case "Thai":
                ans = "th";
                break;
            case "Turkish":
                ans = "tr";
                break;
            case "Ukrainian":
                ans = "uk";
                break;
            case "Urdu":
                ans = "ur";
                break;
            case "Vietnamese":
                ans = "vi";
                break;
            case "Welsh":
                ans = "cy";
                break;
            case "Yiddish":
                ans = "yi";
                break;
        }
        return ans;
    }
}
