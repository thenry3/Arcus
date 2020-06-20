package com.example.arcus;

import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.content.CursorLoader;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
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

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.entity.mime.HttpMultipart;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import com.loopj.android.http.*;

import cz.msebera.android.httpclient.Header;


public class Review extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = BuildConfig.VISION_KEY;
    private String toLang;
    private String fromLang;
    private String toLangCode;
    private String fromLangCode;
    private Uri imgUri;
    Bitmap imgBitmap;

    List<String> fromTranslations = null;
    List<String> toTranslations = null;

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

                fromTranslations = translations.first;
                toTranslations = translations.second;

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
        if (toTranslations == null || fromTranslations == null) {
            Toast.makeText(this, "Wait for image processing to be finished", Toast.LENGTH_SHORT).show();
        }

        String url = "http://192.168.1.17:8000/api/posts/";

        File fileToUse = new File(getPath(this, imgUri));

        //god help me
        RequestParams params = new RequestParams();
        try {
            params.put("picture", fileToUse);
        } catch(FileNotFoundException e) {}

        AsyncHttpClient client = new AsyncHttpClient();
        client.post(url, params, new JsonHttpResponseHandler(){
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    int postID = response.getInt("id");
                    Log.e("asdf", Integer.toString(postID));

                    String postTransURL = "http://192.168.1.17:8000/api/translations/";

                    HttpPost httppost = new HttpPost(postTransURL);
                    httppost.addHeader("Content-Type", "application/json");

                    for (int i = 0; i < toTranslations.size(); i++) {
                        DefaultHttpClient httpclient = new DefaultHttpClient();
                        JSONObject json = new JSONObject();
                        json.put("from_lang", fromLang);
                        json.put("other_lang", toLang);
                        json.put("from_word", fromTranslations.get(i));
                        json.put("other_word", toTranslations.get(i));
                        json.put("post", postID);

                        StringEntity strEntity = new StringEntity(json.toString());
                        httppost.setEntity(strEntity);
                        httpclient.execute(httppost);
                    }
                } catch (Exception e) {
                    Log.e("broken", "welp");
                }
            }
        });

        startActivity(new Intent(this, feed.class));

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

    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
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
