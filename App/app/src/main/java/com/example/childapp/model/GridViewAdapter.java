package com.example.childapp.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.childapp.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class GridViewAdapter extends ArrayAdapter<String> {
    Context context;
    int layoutId;
    ArrayList<String> urls;


    public GridViewAdapter(@NonNull Context context, int resource, @NonNull ArrayList<String> urls) {
        super(context, resource, urls);
        this.context = context;
        this.layoutId = resource;
        this.urls = urls;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(layoutId, parent, false);
        }

        ImageView imageView = convertView.findViewById(R.id.grid_image);
        TextView textView = convertView.findViewById(R.id.grid_img_name);
        Picasso.with(context).load(urls.get(position))
                .fit().centerCrop().placeholder(R.mipmap.ic_launcher)
                .error(R.mipmap.ic_launcher)
                .into(imageView);

        String url = urls.get(position);
        // Tách chuỗi dựa trên dấu "/"
        String[] parts = url.split("/");
        // Lấy phần tử cuối cùng (chứa tên hình ảnh)
        String imageName = parts[parts.length - 1].replace(".jpg", "");
        textView.setText(imageName);

        return convertView;
    }

    @Override
    public int getCount() {
        return urls.size();
    }
}
