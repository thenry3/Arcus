package com.example.arcus;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.bumptech.glide.Glide;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.translate.Translate;
import com.google.api.services.translate.model.TranslationsListResponse;
import com.google.api.services.vision.v1.model.*;
import com.google.api.services.vision.v1.*;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;


public class Review extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = BuildConfig.VISION_KEY;
    private String toLang;
    private String fromLang;
    private String toLangCode;
    private String fromLangCode;
    private Uri imgUri;
    Bitmap imgBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            toLang = (String) extras.get("toLang");
            fromLang = (String) extras.get("fromLang");
            imgUri = (Uri) extras.get("image");
            try {
                imgBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), (Uri) extras.get("image"));
            } catch (Exception e) {

            }

        }

        toLangCode = getLangCode(toLang);
        fromLangCode = getLangCode(fromLang);
        ImageView imgView = findViewById(R.id.chosenImg);
        Glide.with(this).load((Uri) extras.get("image")).into(imgView);
        callCloudVision(imgBitmap, toLangCode, fromLangCode);
    }

    @SuppressLint("StaticFieldLeak")
    public void callCloudVision(final Bitmap bitmap, final String toLangCode, final String fromLangCode){

        new AsyncTask<Object, Void, BatchAnnotateImagesResponse>(){
            @Override
            protected BatchAnnotateImagesResponse doInBackground(Object... params){
                try{
                    HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = GsonFactory.getDefaultInstance();


                    VisionRequestInitializer requestInitializer =
                            new VisionRequestInitializer(CLOUD_VISION_API_KEY);


                    Vision.Builder builder = new Vision.Builder
                            (httpTransport, jsonFactory, null);
                    builder.setVisionRequestInitializer(requestInitializer);
                    Vision vision = builder.build();

                    List<Feature> featureList = new ArrayList<>();
                    Feature labelDetection = new Feature();
                    labelDetection.setType("LABEL_DETECTION");
                    labelDetection.setMaxResults(10);
                    featureList.add(labelDetection);

                    Feature objectLocalization = new Feature();
                    objectLocalization.setType("OBJECT_LOCALIZATION");
                    objectLocalization.setMaxResults(10);
                    featureList.add(objectLocalization);

                    Feature textDetection = new Feature();
                    textDetection.setType("TEXT_DETECTION");
                    textDetection.setMaxResults(10);
                    featureList.add(textDetection);

                    List<AnnotateImageRequest> imageList = new ArrayList<>();
                    AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();
                    Image base64EncodedImage = getBase64EncodedJpeg(bitmap);
                    annotateImageRequest.setImage(base64EncodedImage);
                    annotateImageRequest.setFeatures(featureList);
                    imageList.add(annotateImageRequest);

                    BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                            new BatchAnnotateImagesRequest();
                    batchAnnotateImagesRequest.setRequests(imageList);

                    Vision.Images.Annotate annotateRequest =
                            vision.images().annotate(batchAnnotateImagesRequest);
                    annotateRequest.setDisableGZipContent(true);
                    Log.d("oop", "Sending request to Google Cloud");

                    BatchAnnotateImagesResponse response = annotateRequest.execute();
                    return response;
                } catch (GoogleJsonResponseException e) {
                    Log.e("oop", "Request error: " + e.getContent());
                } catch (IOException e) {
                    Log.d("oop", "Request error: " + e.getMessage());
                }
                return null;
            }
            protected void onPostExecute(BatchAnnotateImagesResponse response) {
                Pair<List<String>, List<String>> translations = getLabels(response, fromLangCode, toLangCode);
                TextView titleView = findViewById(R.id.titleView);
                titleView.setText(fromLang + " -----> " + toLang);

                List<String> fromTranslations = translations.first;
                List<String> toTranslations = translations.second;

                StringBuilder Fmessage = new StringBuilder("");
                StringBuilder Tmessage = new StringBuilder("");

                for (int i = 0; i < fromTranslations.size(); i++) {
                    Fmessage.append(fromTranslations.get(i));
                    Tmessage.append(toTranslations.get(i));
                    Fmessage.append("\n\n");
                    Tmessage.append("\n\n");
                }

                ((TextView) findViewById(R.id.listFrom)).setText(Fmessage);
                ((TextView) findViewById(R.id.listTo)).setText(Tmessage);
            }
        }.execute();
    }

    private Pair<List<String>, List<String>> getLabels(BatchAnnotateImagesResponse response, String fromLangCode, String toLangCode) {
        if (response == null){
            return null;
        }
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        ArrayList<String> words = new ArrayList<>();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                words.add(label.getDescription());
            }
        } else {
            return null;
        }
        Translator t = new Translator();
        try{
            ArrayList<String> fTranslations = fromLangCode != "en" ? t.translate(words, fromLangCode): words;
            ArrayList<String> tTranslations = toLangCode != "en" ? t.translate(words, toLangCode) : words;
            if (labels != null) {
                return new Pair<List<String>, List<String>>(fTranslations, tTranslations);
            } else {
                return null;
            }
        } catch (IOException e){
            Log.e("asdf", e.toString());
        }
        return null;
    }

    public void onPostClick(View v) {
        String url = "";
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        httppost.addHeader("Accept", "application/json");
        httppost.addHeader("Content-type", "multipart/form-data");

        File fileToUse = new File(imgUri.getPath());
    }

    private class Translator {

        private ArrayList<String> translate(ArrayList<String> arr, String tar) throws IOException {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            String key = BuildConfig.TRANS_KEY;
            // Set up the HTTP transport and JSON factory
            HttpTransport httpTransport = new NetHttpTransport();
            JsonFactory jsonFactory = AndroidJsonFactory.getDefaultInstance();
            Translate.Builder translateBuilder = new Translate.Builder(httpTransport, jsonFactory, null);
            translateBuilder.setApplicationName(getString(R.string.app_name));
            Translate translate = translateBuilder.build();
            ArrayList<String> q = arr;
            ArrayList<String> responseList = new ArrayList<>();
            for(int i = 0; i < q.size(); i++)
            {
                Translate.Translations.List list = translate.translations().list(q, tar);
                list.setKey(key);
                list.setSource("en");
                TranslationsListResponse translateResponse = list.execute();
                String response = translateResponse.getTranslations().get(i).getTranslatedText();
                responseList.add(response);
            }
            for (int i = 0; i < responseList.size(); i++)
            {
                Log.d("oop", responseList.get(i));
            }
            return responseList;
        }
    }

    public Image getBase64EncodedJpeg(Bitmap bitmap) {
        Image image = new Image();
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();
        image.encodeContent(imageBytes);
        return image;
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
