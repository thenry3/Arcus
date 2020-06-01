package com.example.arcus;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.HttpURLConnection;

public class feed extends AppCompatActivity {
    private SwipeRefreshLayout mainFeed;
    private RecyclerView recyclerView;

    private HttpURLConnection httpConn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        recyclerView = findViewById(R.id.recycler);
        mainFeed = findViewById(R.id.main_feed);
        mainFeed.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //refreshData(); // your code
                mainFeed.setRefreshing(true);
                mainFeed.setRefreshing(false);
            }
        });
    }

    void refreshData() {
        String url = "";
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(url);
        httppost.addHeader("Accept", "application/json");
        httppost.addHeader("Content-type", "multipart/form-data");

    }

    public void onClick(View v) {
        Intent intent = new Intent(this, ChoosePhoto.class);
        startActivity(intent);
    }
}
