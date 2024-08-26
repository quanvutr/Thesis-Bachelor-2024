package com.example.childapp;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.GridView;

import com.example.childapp.model.GridViewAdapter;

import java.util.ArrayList;

public class ResultActivity extends AppCompatActivity {

    GridView gridView;
    GridViewAdapter adapter;
    ArrayList<String> urls;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);
        gridView = findViewById(R.id.gridview);
        loadUrls();
        initGridView();
    }

    private void initGridView() {
        adapter = new GridViewAdapter(this, R.layout.grid_item, urls);
        gridView.setAdapter(adapter);
    }

    private void loadUrls() {
        String strData = getIntent().getStringExtra("urls");
        urls = new ArrayList<>();
        for(String str: strData.split(";")) {
            urls.add("http://10.0.2.2:8000" + str);
        }
    }
}