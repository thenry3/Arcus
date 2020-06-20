package com.example.arcus;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.RequestFuture;
import com.bumptech.glide.Glide;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.client.HttpClient;

public class feed extends AppCompatActivity {
    private SwipeRefreshLayout mainFeed;
    private LinearLayout linearView;

    androidx.constraintlayout.widget.ConstraintLayout postLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);
        refreshData();

        linearView = findViewById(R.id.linear);
        mainFeed = findViewById(R.id.main_feed);
        mainFeed.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //refreshData(); // your code
                mainFeed.setRefreshing(true);
                linearView.removeAllViews();
                refreshData();
            }
        });
    }

    void refreshData() {
        String url = "http://192.168.1.17:8000/api/posts/";
        AsyncHttpClient client = new AsyncHttpClient();
        client.get(url, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                try {
                    for (int i = timeline.length() - 1; i >= 0; i--) {
                        JSONObject post = timeline.getJSONObject(i);
                        int postID = post.getInt("id");
                        String picture = post.getString("picture");
                        String postTransURL = "http://192.168.1.17:8000/api/translations/?post=".concat(Integer.toString(postID));

                        DefaultHttpClient httpclient = new DefaultHttpClient();
                        HttpGet httpGet = new HttpGet(postTransURL);
                        HttpResponse httpresponse = httpclient.execute(httpGet);

                        String jsonString = EntityUtils.toString(httpresponse.getEntity());
                        JSONArray transJSON = new JSONArray(jsonString);

                        ArrayList<String> fromWord = new ArrayList<>();
                        ArrayList<String> toWord = new ArrayList<>();
                        String fromLang = "";
                        String toLang = "";

                        if (transJSON.length() == 0)
                            continue;
                        for (int j = 0; j < transJSON.length(); j++) {
                            JSONObject transObj = transJSON.getJSONObject(j);
                            fromWord.add(transObj.getString("from_word"));
                            toWord.add(transObj.getString("other_word"));
                            fromLang = transObj.getString("from_lang");
                            toLang = transObj.getString("other_lang");
                        }

                        LinearLayout temp = createPost(i * 3, picture, fromLang, toLang, fromWord, toWord);
                        if (temp == null)
                            continue;
                        Log.e("df", postTransURL);
                        linearView.addView(temp);
                    }
                } catch (Exception e) {
                    Log.e("Fuck", e.toString());
                    mainFeed.setRefreshing(false);
                }
                mainFeed.setRefreshing(false);
            }
        });
    }

    public LinearLayout createPost(int id, String picture, String fromLang, String toLang, ArrayList<String> fromWords, ArrayList<String> toWords) {
        String picUrl = "http://192.168.1.17:8000/media/".concat(picture);
        Bitmap image;
        LinearLayout linearLayout = new LinearLayout(this);

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        try {
            URL url = new URL(picUrl);
            image = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        } catch(Exception e) {
            Log.e("asdf", e.toString());
            Log.e("Asdf", picUrl);
            return null;
        }

        ImageView imgView = new ImageView(this);
//        imgView.setImageBitmap(image);
        imgView.setMaxHeight(400);
        Glide.with(this).load(image).into(imgView);
        linearLayout.addView(imgView);

        FrameLayout.LayoutParams textLayout = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        textLayout.setMargins(0, 3, 0, 0);
        TextView titleView = new TextView(this);
        titleView.setText(fromLang.concat(" -----> ").concat(toLang));
        titleView.setLayoutParams(textLayout);
        titleView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        titleView.setTextColor(Color.parseColor("#37BFA9"));
        titleView.setTextSize(15);
        linearLayout.addView(titleView);
        Log.e("title", "hi");

        ConstraintLayout constraintView = new ConstraintLayout(this);
        ConstraintLayout.LayoutParams constraintLayout = new ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT);
        constraintView.setLayoutParams(constraintLayout);
        constraintView.setId(id);

        TextView listFrom = new TextView(this);
        listFrom.setId(id + 1);
        TextView listTo = new TextView(this);
        listTo.setId(id + 1);

        StringBuilder Fmessage = new StringBuilder("");
        StringBuilder Tmessage = new StringBuilder("");

        for (int i = 0; i < fromWords.size(); i++) {
            Fmessage.append(fromWords.get(i));
            Tmessage.append(toWords.get(i));
            Fmessage.append("\n\n");
            Tmessage.append("\n\n");
        }

        listFrom.setText(Fmessage);
        listTo.setText(Tmessage);

        FrameLayout.LayoutParams fromParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        FrameLayout.LayoutParams toParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        toParams.setMargins(0, 0, 20, 0);
        listFrom.setLayoutParams(fromParams);
        listTo.setLayoutParams(toParams);

        listTo.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_END);

        ConstraintSet constraintSetFrom = new ConstraintSet();
        ConstraintSet constraintSetTo = new ConstraintSet();
        constraintSetFrom.clone(constraintView);
        constraintSetTo.clone(constraintView);

        constraintSetFrom.connect(listFrom.getId(), ConstraintSet.TOP, constraintView.getId(), ConstraintSet.TOP);
        constraintSetFrom.connect(listFrom.getId(), ConstraintSet.START, constraintView.getId(), ConstraintSet.START);
        constraintSetTo.connect(listTo.getId(), ConstraintSet.TOP, constraintView.getId(), ConstraintSet.TOP);
        constraintSetTo.connect(listTo.getId(), ConstraintSet.END, constraintView.getId(), ConstraintSet.END);
        constraintSetFrom.applyTo(constraintView);
        constraintSetTo.applyTo(constraintView);

        constraintView.addView(listFrom);
        constraintView.addView(listTo);

        linearLayout.addView(constraintView);

        return linearLayout;
    }

    public void onClick(View v) {
        Intent intent = new Intent(this, ChoosePhoto.class);
        startActivity(intent);
    }
}
