package com.example.childapp;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.example.childapp.model.AppHelper;
import com.example.childapp.model.VolleyMultipartRequest;
import com.example.childapp.model.VolleySingleton;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity{
    public static final int CAM_REQUEST_CODE = 102;
    private static final int CAM_PERMISSION_CODE = 101;
    private static final int GALLERY_REQUEST_CODE = 103;
    private static final int WRITE_PERMISSION_CODE = 104;
    private ImageView temp;
    private Uri image_uri;
    private final String TAG = "111111111111111";

    ImageView personImg;
    Button submit;
    ProgressDialog progressDialog;
    FloatingActionButton fab;
    BottomNavigationView bottomNavigationView;
    String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        prg = findViewById(R.id.progressbar);
        personImg = findViewById(R.id.personImg);
        submit = findViewById(R.id.submit_btn);
        submit.setOnClickListener(this::postData);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        fab = findViewById(R.id.fab_btn);

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction().replace(R.id.frame_layout, new MainFragment()).commit();
        }

        replaceFragment(new MainFragment());

        bottomNavigationView.setBackground(null);
        bottomNavigationView.setOnItemSelectedListener(item -> {

            switch (item.getItemId()) {
                case R.id.nav_main:
                    replaceFragment(new MainFragment());
                    break;
                case R.id.nav_results:

                    break;
            }

            return true;
        });

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showBottomDialog();
            }
        });
    }

    private  void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.frame_layout, fragment);
        fragmentTransaction.commit();
    }

    private void showBottomDialog() {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.bottomsheetlayout);

        LinearLayout galleryLayout = dialog.findViewById(R.id.layoutGallery);
        LinearLayout cameraLayout = dialog.findViewById(R.id.layoutCamera);

        galleryLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this,"Upload a Image is clicked",Toast.LENGTH_SHORT).show();
                Intent gallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(gallery, GALLERY_REQUEST_CODE);

            }
        });

        cameraLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                Toast.makeText(MainActivity.this,"Camera is clicked",Toast.LENGTH_SHORT).show();
                askCameraPermission();
            }
        });

        dialog.show();
        dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().setGravity(Gravity.BOTTOM);
    }


    public void postData(View v){
        if (personImg.getDrawable() != null){
            String url = "http://10.0.2.2:8000/api/submit";
            VolleyMultipartRequest multipartRequest= new VolleyMultipartRequest(Request.Method.POST, url,
                    new Response.Listener<NetworkResponse>() {
                        @Override
                        public void onResponse(NetworkResponse response) {
                            if (progressDialog != null) {
                                progressDialog.dismiss();
                            }
                            String urls = new String(response.data);
                            Log.d("@@@", urls);
                            Intent intent = new Intent(MainActivity.this, ResultActivity.class);
                            intent.putExtra("urls", urls);
                            startActivity(intent);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Log.d("@@@", new String(error.getMessage()));
                        }
                    }){
                @Override
                protected Map<String, String> getParams(){
                    Map<String,String> params = new HashMap<>();
                    params.put("tittle", "upload Image");
                    return params;
                }
                @Override
                protected Map<String, DataPart> getByteData() {
                    Map<String, DataPart> params = new HashMap<>();
                    params.put("image", new DataPart("personImg.jpg",
                                        AppHelper.getFileDataFromDrawable(getBaseContext(),
                                        personImg.getDrawable()),
                                "image/jpeg"));
                    return params;
                }
            };
            multipartRequest.setRetryPolicy(new DefaultRetryPolicy(
                    15000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            progressDialog = new ProgressDialog(this);
            progressDialog.setTitle("Processing");
            progressDialog.show();

            VolleySingleton.getInstance(getBaseContext()).addToRequestQueue(multipartRequest);
        }
    }

    public void askCameraPermission() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, permissions, CAM_PERMISSION_CODE);
        } else {
            dispatchTakePictureIntent();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAM_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAM_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                File f = new File(currentPhotoPath);

                Bitmap bitmap = BitmapFactory.decodeFile(f.getAbsolutePath());
                Matrix matrix = new Matrix();
                matrix.postRotate(-90); // Xoay 90 độ
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                // Record rotated images to a file
                try (FileOutputStream out = new FileOutputStream(f)) {
                    rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                personImg.setImageURI(Uri.fromFile(f));
                Log.d(TAG, "Absolute Url of Img is:" + Uri.fromFile(f));

                //Save to gallery
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaScanIntent.setData(Uri.fromFile(f));
                sendBroadcast(mediaScanIntent);
            }
        }

        if(requestCode == GALLERY_REQUEST_CODE){
            if(resultCode == Activity.RESULT_OK){
                Uri contentUri = data.getData();
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
                String imageFileName = "JPEG_" + timeStamp +"."+getFileExt(contentUri);
                Log.d(TAG, "onActivityResult: Gallery Image Uri:  " +  imageFileName);
                personImg.setImageURI(contentUri);
            }
        }
    }

    private String getFileExt(Uri contentUri) {
        ContentResolver c = getContentResolver();
        MimeTypeMap mime = MimeTypeMap.getSingleton();
        return mime.getExtensionFromMimeType(c.getType(contentUri));
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException e) {
            Log.e("CheckingError2", "Error message: " + e.getMessage());
        }

        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.example.android.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            startActivityForResult(takePictureIntent, CAM_REQUEST_CODE);
        }
    }
}
